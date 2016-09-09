package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import com.ojcoleman.europa.algos.neat.NEATSpecies;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.DefaultEvolver.SpeciesData;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.util.Stringer;

/**
 * A default Evolver that selects {@link DefaultEvolver#parentsProportion} of the highest ranked {@link Individual}s
 * from the Population, or each Species if applicable, and produces new members via recombination and/or cloning and
 * mutation to replace the current members. If elitism is used, some number of members may be maintained to the next
 * generation unchanged.
 */
public class DefaultEvolver<G extends Genotype<?>> extends Evolver<G> {
	@Parameter(description = "The proportion of parents to select from the population or each species, used to generate new genotypes.", defaultValue = "0.2", minimumValue = "0", maximumValue = "1")
	protected double parentsProportion;

	@Parameter(description = "The proportion of elites to select from the population or each species, elites continue to next generation unchanged.", defaultValue = "0.05", minimumValue = "0", maximumValue = "1")
	protected double elitismProportion;

	@Parameter(description = "The maximum number of elites to select from the population or each species, elites continue to next generation unchanged. -1 indicates no limit.", defaultValue = "-1", minimumValue = "-1")
	protected int elitismMax;

	@Parameter(description = "If speciation is used, whether to use fitness sharing for a species when determining the relative number of children to produce from a species. If disabled then the number of children is proportional to a species current size.", defaultValue = "true")
	protected boolean speciesFitnessSharing;

	@Parameter(description = "If speciation is used, how many iterations a species may persist without the (overall) fitness of its fittest individual improving. Set to 0 to disable.", defaultValue = "0")
	protected int speciesMaxStagnantIterations;
	
	private final Map<Species<G>, SpeciesData<G>> speciesData = Collections.synchronizedMap(new HashMap<Species<G>, SpeciesData<G>>());

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public DefaultEvolver(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void evolve(final Population<G, ?> population) {
		List<Species<G>> parentSpecies = new ArrayList<>(population.getSpecies());
		// Remove empty species.
		Iterator<Species<G>> speciesIter = parentSpecies.iterator();
		while (speciesIter.hasNext()) {
			if (speciesIter.next().isEmpty()) {
				speciesIter.remove();
			}
		}

		if (parentSpecies.isEmpty()) {
			throw new IllegalStateException("No species from which to produce offspring.");
		}
		
		// Calculate stats and rank members for each species.
		this.getParentComponent(Run.class).parallel.foreach(parentSpecies, new Parallel.Operation<Species<G>>() {
			public void perform(Species<G> species) {
				SpeciesData<G> sd = speciesData.get(species);
				if (sd == null) {
					sd = new SpeciesData<G>();
					speciesData.put(species, sd);
				}
				sd.setMembers(species.getMembers());
			}
		});
		
		if (speciesFitnessSharing) {
			// Number of offspring to produce from a species is proportional to its average fitness (fitness sharing).
			double totalAvgFitness = 0;
			int includeCount = 0;
			int totalIncludePop = 0;
			for (Species<G> species : parentSpecies) {
				SpeciesData<G> sd = speciesData.get(species);
				
				if (speciesMaxStagnantIterations <= 0 || sd.stagnantIterationCount < speciesMaxStagnantIterations) {
					totalAvgFitness += sd.averageFitness;
					includeCount++;
					totalIncludePop += sd.rankedMembers.size();
				}
			}
			
			
			// Normalise proportional new sizes, and factor in species previous.
			for (Species<G> species : parentSpecies) {
				SpeciesData<G> sd = speciesData.get(species);
				
				// If there's only one species, or this species hasn't been stagnant too long.
				if (parentSpecies.size() == 1 || speciesMaxStagnantIterations <= 0 || sd.stagnantIterationCount < speciesMaxStagnantIterations) {
					// If every single species has zero fitness value, assign equal sizes.
					if (totalAvgFitness == 0) {
						sd.newSizeProportion = 1.0 / includeCount;
					}
					else {
						double newSizeFitnessSharing = speciesData.get(species).averageFitness / totalAvgFitness;
						// Factor in previous size. This helps stop a species' size from oscillating.
						double previousSize = (double) species.size() / totalIncludePop;
						
						sd.newSizeProportion = 0.7 * newSizeFitnessSharing + 0.3 * previousSize;
						//sd.newSizeProportion = newSizeFitnessSharing;
					}
				}
				else {
					// Species has been stagnant too long, don't reproduce from it.
					sd.newSizeProportion = 0;
				}
			}

		} else {
			// Number of offspring to produce from a species is proportional to the species size (determined by the speciator).
			for (Species<G> species : parentSpecies) {
				SpeciesData<G> sd = speciesData.get(species);
				
				// If there's only one species, or this species hasn't been stagnant too long.
				if (parentSpecies.size() == 1 || speciesMaxStagnantIterations <= 0 || sd.stagnantIterationCount < speciesMaxStagnantIterations) {
					sd.newSizeProportion = (double) species.size() / population.size();
				}
				else {
					// Species has been stagnant too long, don't reproduce from it.
					sd.newSizeProportion = 0;
				}
			}
		}
		
		final List<G> newOffspring = Collections.synchronizedList(new ArrayList<G>(population.getDesiredSize()));
		final List<Individual<G, ?>> toRemove = Collections.synchronizedList(new ArrayList<Individual<G, ?>>());
		final DefaultEvolver<G> evolver = this;
		final Random random = this.getParentComponent(Run.class).random;
		
		// Reproduce from each species relative to its percentage of total fitness.
		this.getParentComponent(Run.class).parallel.foreach(parentSpecies, new Parallel.Operation<Species<G>>() {
			public void perform(Species<G> species) {
				SpeciesData<G> sd = speciesData.get(species);
				
				int newSpeciesSize = (int) Math.round(sd.newSizeProportion * population.getDesiredSize());
				
				int eliteCount = Math.min(sd.rankedMembers.size(), (int) Math.round(elitismProportion * newSpeciesSize));
				if (elitismMax != -1 && eliteCount > elitismMax) {
					eliteCount = elitismMax;
				}
				
				if (sd != null) {
					Individual<G, ?> fittest = sd.rankedMembers.get(0);
					//System.out.println(species.id + " : " + newSpeciesSize + " : " + eliteCount + " | " + fittest.id + " : " + fittest.evaluationData.getFitnessResults().values().iterator().next());
				}
				
				if (newSpeciesSize > 0) {
					int numSpeciesOffspring = newSpeciesSize - eliteCount;
					
					if (numSpeciesOffspring > 0) {
						// Get parents.
						int parentCount = Math.max(2, (int) Math.round(species.size() * parentsProportion));
						if (parentCount > sd.rankedMembers.size()) {
							parentCount = sd.rankedMembers.size();
						}
						List<Individual<G, ?>> parents = new ArrayList<>(sd.rankedMembers.subList(0, parentCount));
						
						for (int offspringIdx = 0; offspringIdx < numSpeciesOffspring; offspringIdx++) {
							G newGenotype = null;
							
							// Select a recombiner (or cloning) at random, with probability proportional to
							// Evolver#actualRecombinerProportions
							Recombiner<G> recombiner = selectRandomRecombiner();
							
							// If we should use a recombiner (not cloning).
							if (recombiner != null && parents.size() >= 2) {
								// Determine number of parents.
								int maxParents = recombiner.parentCountMaximum();
								if (maxParents < 2) {
									throw new IllegalStateException("The maximum number of parents for a Recombiner must be >= 2, " + recombiner.getClass().getName() + " gave " + maxParents);
								}
								int offspringParentCount = random.nextInt(maxParents - 1) + 2;
								
								// Collect genotypes from randomly selected parents.
								Collections.shuffle(parents, random);
								List<Individual<G, ?>> offspringParents = parents.subList(0, offspringParentCount);
								List<G> offspringParentGenotypes = new ArrayList<>(offspringParentCount);
								for (Individual<G, ?> p : offspringParents) {
									offspringParentGenotypes.add(p.genotype);
								}
								
								// Create a new genotype by recombining parent genotypes.
								newGenotype = recombiner.recombine(offspringParentGenotypes);
							} else {
								// Create a clone of one of the parents.
								Individual<G, ?> parent = parents.get(random.nextInt(parents.size()));
								
								newGenotype = parent.genotype.newInstance();
							}
							
							// Mutate the new genotype as necessary.
							mutateGenotype(newGenotype, false);
							
							// Add to list to add to population.
							newOffspring.add(newGenotype);
						}
					}
					
					// Record non-elites of this species to be removed from population.
					toRemove.addAll(sd.rankedMembers.subList(eliteCount, sd.rankedMembers.size()));
				}
				else {
					// Species is either not fit enough to reproduce or has been stagnant for too long.
					toRemove.addAll(species.members);
					species.setDefunct();
				}
			}
		});
		
		// Remove non-elites from population (this also removes the individuals from the species).
		for (Individual<G, ?> ind : toRemove) {
			population.removeIndividual(ind);
		}

		// The number of offspring should be the desired size minus the number of remaining elites.
		int targetNewOffspringCount = population.getDesiredSize() - population.size();

		// Remove random offspring if we have too many.
		while (newOffspring.size() > targetNewOffspringCount) {
			newOffspring.remove(random.nextInt(newOffspring.size()));
		}

		// Add mutated clones of random offspring if we don't have enough.
		while (newOffspring.size() > 0 && newOffspring.size() < targetNewOffspringCount) {
			G newGenotype = newOffspring.get(random.nextInt(newOffspring.size())).newInstance();
			mutateGenotype(newGenotype, false);
			newOffspring.add(newGenotype);
		}
		
		// Add new genotypes to population.
		for (G newGenotype : newOffspring) {
			population.addGenotype(newGenotype);
		}
		
		// Remove data for missing/defunct species.
		Iterator<Entry<Species<G>, SpeciesData<G>>> itr = speciesData.entrySet().iterator();
		while (itr.hasNext()) {
			if (!parentSpecies.contains(itr.next().getKey())) {
				itr.remove();
			}
		};
	}
	
	
	static class SpeciesData<G2 extends Genotype<?>> {
		public double newSizeProportion;
		public double newSize;
		public double averageFitness;
		public double currentBestFitness;
		public double bestEverFitness = Double.NaN;
		public int stagnantIterationCount = 0;
		public List<Individual<G2, ?>> rankedMembers = new ArrayList<>();
		
		public void setMembers(Set<Individual<G2, ?>> members) {
			rankedMembers.clear();
			rankedMembers.addAll(members);
			Collections.sort(rankedMembers);
			Collections.reverse(rankedMembers);
			
			averageFitness = 0;
			currentBestFitness = Double.NaN;
			
			for (Individual<G2, ?> ind : members) {
				Map<EvaluationDescription, Double> results = ind.evaluationData.getFitnessResults();
				double f = 0;
				for (Entry<EvaluationDescription, Double> evData : results.entrySet()) {
					f += evData.getKey().range.translateToUnit(evData.getValue());
				}
				f /= results.size();
				averageFitness += f;
				if (Double.isNaN(currentBestFitness) || f > currentBestFitness) {
					currentBestFitness = f;
				}
			}
			
			averageFitness /= members.size();
			
			//String s = bestEverFitness + " : " + currentBestFitness;
			
			if (Double.isNaN(bestEverFitness) || currentBestFitness > bestEverFitness) {
				bestEverFitness = currentBestFitness;
				stagnantIterationCount = 0;
			}
			else {
				stagnantIterationCount++;
				//s += " : +1 = ";
			}
			
			//System.out.println(s + stagnantIterationCount);
		}
	}
}

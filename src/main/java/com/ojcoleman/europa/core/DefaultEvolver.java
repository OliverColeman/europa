package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.util.Stringer;

/**
 * A default Evolver that selects {@link DefaultEvolver#parentsProportion} of the highest ranked {@link Individual}s from the Population, or
 * each Species if applicable, and produces new members via recombination and/or cloning and mutation to replace the
 * current members. If elitism is used, some number of members may be maintained to the next generation unchanged.
 */
public class DefaultEvolver<G extends Genotype<?>> extends Evolver<G> {
	@Parameter(description = "The proportion of parents to select from the population or each species, used to generate new genotypes.", defaultValue = "0.2", minimumValue = "0", maximumValue = "1")
	double parentsProportion;

	@Parameter(description = "The proportion of elites to select from the population or each species, elites continue to next generation unchanged.", defaultValue = "0.05", minimumValue = "0", maximumValue = "1")
	double elitismProportion;
	
	@Parameter(description = "If speciation is used, whether to use fitness sharing for a species when determining the relative number of children to produce from a species. If disabled then the number of children is proportional to a species current size.", defaultValue = "true")
	boolean speciesFitnessSharing;
	

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public DefaultEvolver(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	
	@Override
	public void evolve(final Population<G, ?> population) {
		List<Species<G>> parentSpecies = population.getSpecies();
		
		if (parentSpecies.isEmpty()) {
			throw new IllegalStateException("No species from which to produce offspring.");
		}
		
		
		final Map<Species<G>, Double> speciesNewSizeProportional = new HashMap<>();
		if (speciesFitnessSharing) {
			// Number of offspring to produce from a species is proportional to its average fitness (fitness sharing).
			double totalAvgRank = 0;
			for (Species<G> species : parentSpecies) {
				double avgRank = species.getAverageRank();
				speciesNewSizeProportional.put(species, avgRank);
				totalAvgRank += avgRank;
			}

			// Normalise proportional new sizes.
			for (Species<G> species : parentSpecies) {
				speciesNewSizeProportional.put(species, speciesNewSizeProportional.get(species) / totalAvgRank);
			}
			
		}
		else {
			// Number of offspring to produce from a species is proportional to the species size.
			for (Species<G> species : parentSpecies) {
				speciesNewSizeProportional.put(species, (double) species.size() / population.size());
			}
		}
		
		final List<G> newOffspring = Collections.synchronizedList(new ArrayList<G>(population.getDesiredSize()));
		final DefaultEvolver<G> evolver = this;
		final Random random = this.getParentComponent(Run.class).random;
		
		// Reproduce from each species relative to its percentage of total fitness.
		this.getParentComponent(Run.class).parallel.foreach(parentSpecies, new Parallel.Operation<Species<G>>() {
			public void perform(Species<G> species) {
				if (!species.isEmpty()) {
					// Get all members of species, highest ranked first.
					List<Individual<G, ?>> rankedMembers = new ArrayList<>(species.getMembers());
					Collections.sort(rankedMembers);
					Collections.reverse(rankedMembers);
					
					int eliteCount = (int) Math.round(elitismProportion * species.size());
					int numSpeciesOffspring =  (int) Math.round(speciesNewSizeProportional.get(species) * population.getDesiredSize()) - eliteCount;
					
					if (numSpeciesOffspring > 0) {
						// Get parents.
						int parentCount = Math.max(2, (int) Math.round(species.size() * parentsProportion));
						if (parentCount > rankedMembers.size()) {
							parentCount = rankedMembers.size();
						}
						List<Individual<G, ?>> parents = rankedMembers.subList(0, parentCount);
						
						for (int offspringIdx = 0; offspringIdx < numSpeciesOffspring; offspringIdx++) {
							G newGenotype = null;
							
							// Select a recombiner (or cloning) at random, with probability proportional to Evolver#actualRecombinerProportions
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
							}
							else {
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
					
					// Remove non-elites from population (this also removes the individuals from the species).
					List<Individual<G, ?>> toRemove = rankedMembers.subList(eliteCount, rankedMembers.size());
					for (Individual<G, ?> ind : toRemove) {
						population.removeIndividual(ind);
					}
				}
			}
		});
		
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
	}
}

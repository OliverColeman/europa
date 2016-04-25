package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Parallel;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Run;

/**
 * @author O. J. Coleman
 *
 */
public class NEATSpeciatorKMeans extends NEATSpeciator<NEATGenotype, NEATSpecies> {
	@Parameter(description = "The number of species (clusters). If set to 0 defaults to [population size] ^ 0.6 (a bit more than square root of population size).", defaultValue = "0")
	protected int speciesCount;

	@Parameter(description = "The maximum number of iterations of the k-means algorithm.", minimumValue = "1", defaultValue = "5")
	protected int maxIterations;

	private final Run run;
	// Reference to the component used to perform operations in parallel.
	private final Parallel parallel;

	public NEATSpeciatorKMeans(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		run = this.getParentComponent(Run.class);
		parallel = this.getParentComponent(Run.class).getParallel();

		if (speciesCount <= 0) {
			speciesCount = (int) Math.round(Math.pow(this.getParentComponent(Population.class).getDesiredSize(), 0.6));
		}
	}

	@Override
	public void speciate(Population<NEATGenotype, ?> population, final List<NEATSpecies> speciesList) {
		List<Individual<NEATGenotype, ?>> individualList = new ArrayList<Individual<NEATGenotype, ?>>(population.getMembers());

		// If this is the first speciation (or somehow the desired number of species has changed).
		if (speciesList.size() != speciesCount) {
			// Reset all species assignments.
			for (NEATSpecies species : speciesList) {
				species.clear();
			}
			// Create new set of species with randomly selected individuals as initial representatives.
			speciesList.clear();
			for (int i = 0; i < speciesCount; i++) {
				int indIndexRandom = run.random.nextInt(individualList.size());
				// new NEATSpecies(speciesPrototype, individualList.get(indIndexRandom).genotype); //newInstance
				// prototype constructor parameter check.
				NEATSpecies species = speciesPrototype.newInstance(individualList.get(indIndexRandom).genotype);
				speciesList.add(species);
			}
		}

		// Update the centroid of each species. If we're adding offspring this means that old individuals
		// have been removed from the population and therefore the centroids are out-of-date.
		calculateSpecieCentroids(speciesList);

		// Allocate each individual to the species it is closest to.
		parallel.foreach(individualList, new Parallel.Operation<Individual<NEATGenotype, ?>>() {
			@Override
			public void perform(Individual<NEATGenotype, ?> individual) {
				NEATSpecies closestSpecies = findClosestSpecies(individual, speciesList);
				closestSpecies.addMemberRemoveFromCurrent(individual);
			}
		});

		assert testSpeciationIntegrity(individualList, speciesList);

		// Recalculate each species centroid now that they contain additional individuals.
		calculateSpecieCentroids(speciesList);

		// double initialDistance = calculateAverageDistance(individualList, specParms);

		// Perform the main k-means loop until convergence.
		speciateUntilConvergence(individualList, speciesList);

		// System.err.println(initialDistance + " -> " + calculateAverageDistance(individualList, specParms));
	}

	// Perform the main k-means loop until no individual reallocations occur or some maximum number of loops
	// has been performed. Theoretically a small number of reallocations may occur for a great many loops
	// therefore we require the additional max loops threshold exit strategy - the clusters should be pretty
	// stable and well defined after a few loops even if the the algorithm hasn't converged completely.
	private synchronized void speciateUntilConvergence(final List<Individual<NEATGenotype, ?>> individualList, final List<NEATSpecies> speciesList) {
		List<NEATSpecies> emptySpeciesList = Collections.synchronizedList(new ArrayList<NEATSpecies>());
		for (NEATSpecies species : speciesList) {
			if (species.isEmpty()) {
				emptySpeciesList.add(species);
			}
		}

		assert testSpeciationIntegrity(individualList, speciesList);

		// List of modified species (had individuals allocated to and/or from it).
		final Set<NEATSpecies> speciesMod = Collections.synchronizedSet(new HashSet<NEATSpecies>());

		// Main k-means loop.
		boolean reallocationsOccurred = false;
		for (int loop = 0; loop < maxIterations; loop++) {
			// System.err.println("current average distance: " + calculateAverageDistance(individualList,
			// speciationParms));

			// Track number of reallocations made on each loop.
			reallocationsOccurred = false;

			assert testSpeciationIntegrity(individualList, speciesList);

			// Loop over individuals. For each one find the species it is closest to; if it is not the species
			// it is currently in then reallocate it.
			parallel.foreach(individualList, new Parallel.Operation<Individual<NEATGenotype, ?>>() {
				@Override
				public void perform(Individual<NEATGenotype, ?> individual) {
					NEATSpecies closestSpecies = findClosestSpecies(individual, speciesList);
					if (!individual.getSpecies().equals(closestSpecies)) {
						// Track which species have been modified.
						speciesMod.add((NEATSpecies) individual.getSpecies());
						speciesMod.add(closestSpecies);

						closestSpecies.addMemberRemoveFromCurrent(individual); // This removes the individual from its
																				// previous species.

						// assert testSpeciationIntegrity(individualList, speciesList);
					}
				}
			});

			assert testSpeciationIntegrity(individualList, speciesList);

			// Track empty species. We will allocate individuals to them after this loop.
			// This is necessary as some distance metrics can result in empty species occurring.
			for (NEATSpecies species : speciesMod) {
				if (species.isEmpty() && !emptySpeciesList.contains(species)) {
					emptySpeciesList.add(species);
				}
			}

			// Recalculate centroid for all affected species.
			calculateSpecieCentroids(speciesMod);

			assert testSpeciationIntegrity(individualList, speciesList);

			reallocationsOccurred |= !speciesMod.isEmpty();
			speciesMod.clear();

			// Check for empty species. We need to reallocate some individuals into the empty specieList to maintain the
			// required number of species.
			if (!emptySpeciesList.isEmpty()) {
				// We find the individuals in the population as a whole that are farthest from their containing species
				// centroid - we call these outlier individuals. We then move these individuals into the empty species
				// to
				// act as the sole member and centroid of those species; These act as species seeds for the next k-means
				// loop.
				List<Individual<NEATGenotype, ?>> individualsByDistance = getChromosomesByDistanceFromSpecies(individualList, speciesList);

				assert testSpeciationIntegrity(individualList, speciesList);

				// Reallocate each of the outlier individuals from their current species to an empty species.
				int emptySpeciesCount = emptySpeciesList.size();
				int outlierIdx = 0;
				for (int i = 0; i < emptySpeciesCount && outlierIdx < individualsByDistance.size(); i++) {
					// Find the next outlier individual that can be re-allocated. Skip individuals that are the
					// only member of a species - that would just create another empty species.
					Individual<NEATGenotype, ?> individual;
					NEATSpecies sourceSpecies;
					do {
						individual = individualsByDistance.get(outlierIdx++);
						sourceSpecies = (NEATSpecies) individual.getSpecies();
					} while (sourceSpecies.size() == 1 && outlierIdx < individualsByDistance.size());

					// If the provided population is very small then it won't always be possible to fill all the empty
					// species.
					if (outlierIdx < individualsByDistance.size()) {
						// Get ref to the empty species and register both source and target species with specieModArr.
						NEATSpecies emptySpecies = emptySpeciesList.get(i);
						speciesMod.add(emptySpecies);
						speciesMod.add(sourceSpecies);

						emptySpecies.addMemberRemoveFromCurrent(individual);

						assert testSpeciationIntegrity(individualList, speciesList);
					}
				}

				// Recalculate centroid for all affected species.
				calculateSpecieCentroids(speciesMod);

				assert testSpeciationIntegrity(individualList, speciesList);

				reallocationsOccurred |= !speciesMod.isEmpty();
				speciesMod.clear();

				// Clear emptySpecieList after using it. Otherwise we are holding old references and thus creating
				// work for the garbage collector.
				emptySpeciesList.clear();
			}

			// for (NEATSpecies species : speciesList) {
			// System.err.print(species.size() + ", ");
			// }
			// System.err.println();

			// Exit the loop if no individual reallocations have occurred. The species are stable, speciation is
			// completed.
			if (!reallocationsOccurred) {
				// System.err.println("stable");
				break;
			}
		}
	}

	private void calculateSpecieCentroids(Collection<NEATSpecies> speciesList) {
		parallel.foreach(speciesList, new Parallel.Operation<NEATSpecies>() {
			@Override
			public void perform(NEATSpecies species) {
				if (!species.isEmpty()) {
					species.setRepresentative(calculateSpecieCentroid(species));
				}
			}
		});
	}

	// Recalculate the species centroid based on the individuals currently in the species.
	private NEATGenotype calculateSpecieCentroid(NEATSpecies species) {
		assert !species.isEmpty() : "NEATSpecies can't be empty when calculating its centroid.";

		// Special case - 1 individual in species (its position *is* the species centroid).
		if (species.size() == 1) {
			return species.getMembers().iterator().next().genotype;
		}

		// Create a temp list containing all of the individual positions.
		Set<Individual<NEATGenotype, ?>> individualList = species.getMembers();
		List<NEATGenotype> coordList = new ArrayList<NEATGenotype>(individualList.size());
		for (Individual<NEATGenotype, ?> ind : individualList) {
			coordList.add(ind.genotype);
		}

		return calculateCentroid(coordList);
	}

	// Calculates the centroid for the given set of points.
	// A centroid is used in k-means clustering to define the centre of a cluster.
	// The centroid is a central position within a set of points that minimises the sum of the squared distance
	// between each of those points and the centroid. As such it can also be thought of as being an exemplar
	// for a set of points.
	private NEATGenotype calculateCentroid(List<NEATGenotype> genotypes) {
		assert !genotypes.isEmpty() : "coordList can't be empty when calculating its centroid.";

		// Special case - one item in list, it *is* the centroid.
		if (1 == genotypes.size()) {
			return genotypes.get(0);
		}

		// Calculate the average value over each parameter in each Allele over all Genotypes in this Species/cluster.
		// First we collect the parameter vectors for each allele for each gene.
		TreeMap<Gene, List<Vector>> allelesForGene = new TreeMap<>();
		for (NEATGenotype coord : genotypes) {
			for (NEATAllele<?> coordElem : coord.getAlleles()) {
				List<Vector> vectors = allelesForGene.get(coordElem.gene);
				if (vectors == null) {
					vectors = new ArrayList<>(genotypes.size());
				}
				vectors.add(coordElem.vector);
			}
		}
		// Now calculate average allele for each set of alleles for each gene.
		ArrayList<NEATAllele<?>> avgAlleles = new ArrayList<>(allelesForGene.keySet().size());
		for (Gene gene : allelesForGene.keySet()) {
			Vector avg = Vector.calculateAverage(allelesForGene.get(gene));
		}
		// Create the average Genotype.
		// new NEATGenotype(genotypes.get(0), avgAlleles, new ArrayList<Genotype<?>>()); // Prototype constructor check.
		return genotypes.get(0).newInstance(avgAlleles, new ArrayList<Genotype<?>>());
	}

	/**
	 * Gets an array of all individuals ordered by their distance from their current species, largest distance first.
	 */
	private List<Individual<NEATGenotype, ?>> getChromosomesByDistanceFromSpecies(List<Individual<NEATGenotype, ?>> individualList, List<NEATSpecies> speciesList) {
		// Build a distance-sorted map of all individuals and their distance from their centroid.
		// Use a multimap because two individual could be the same distance from their species centroid.
		TreeMultimap<Double, Individual<NEATGenotype, ?>> distanceMap = TreeMultimap.create();

		for (Individual<NEATGenotype, ?> individual : individualList) {
			double distance = getDistance(individual.genotype, ((NEATSpecies) individual.getSpecies()).getRepresentative());
			distanceMap.put(distance, individual);
		}

		// Put the sorted individuals in a list. Note we get a reverse-sorted view
		// (descendingSet) of the distanceMap because we want the largest distance first.
		ArrayList<Individual<NEATGenotype, ?>> sortedIndividuals = new ArrayList<>(individualList.size());
		for (Double d : distanceMap.keySet().descendingSet()) {
			sortedIndividuals.addAll(distanceMap.values());
		}

		return sortedIndividuals;
	}

	/**
	 * Find the species that an individual is closest to.
	 */
	private NEATSpecies findClosestSpecies(Individual<NEATGenotype, ?> individual, List<NEATSpecies> speciesList) {
		NEATSpecies closestSpecies = null;
		double closestDistance = Double.MAX_VALUE;

		// Find closest species (we don't parallelise here because this method is called from inside parallelised code
		// sections).
		for (NEATSpecies species : speciesList) {
			double distance = getDistance(individual.genotype, species.getRepresentative());
			// All else being equal keep individual in same species.
			if (distance < closestDistance || (distance == closestDistance && individual.getSpecies() != null && individual.getSpecies() == species)) {
				closestDistance = distance;
				closestSpecies = species;
			}
		}
		return closestSpecies;
	}

	private double calculateAverageDistance(List<Individual<NEATGenotype, ?>> individualList) {
		double totalDistance = 0;
		for (int i = 0; i < individualList.size(); i++) {
			Individual<NEATGenotype, ?> individual = individualList.get(i);
			double distance = getDistance(individual.genotype, ((NEATSpecies) individual.getSpecies()).getRepresentative());
			totalDistance += distance;
		}
		return totalDistance / individualList.size();
	}

	private boolean testSpeciationIntegrity(List<Individual<NEATGenotype, ?>> individualList, List<NEATSpecies> speciesList) {
		Collections.sort(individualList);

		Individual<NEATGenotype, ?> previous = null;
		for (Individual<NEATGenotype, ?> c : individualList) {
			if (previous != null && previous.equals(c)) {
				System.err.println("Individual<NEATGenotype, ?> appears multiple times in population.");
				return false;
			}

			if (c.getSpecies() != null && !c.getSpecies().getMembers().contains(c)) {
				System.err.println("Chromosomes recorded species does not include the chromosome.");
				return false;
			}

			int individualInAnySpeciesCount = 0;
			for (NEATSpecies s : speciesList) {
				int individualInThisSpeciesCount = 0;
				for (Individual<NEATGenotype, ?> sc : s.getMembers()) {
					if (c.equals(sc)) {
						individualInThisSpeciesCount++;
						individualInAnySpeciesCount++;
					}
				}
				if (individualInThisSpeciesCount > 1) {
					System.err.println("Individual<NEATGenotype, ?> appears in same species " + individualInThisSpeciesCount + " times");
				}
			}
			if (individualInAnySpeciesCount != 1) {
				System.err.println("Individual<NEATGenotype, ?> appears in any species " + individualInAnySpeciesCount + " times");
				return false;
			}
		}
		return true;
	}
}

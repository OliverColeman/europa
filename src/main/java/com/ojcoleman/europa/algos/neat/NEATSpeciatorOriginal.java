package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Parallel;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.core.Speciator;
import com.ojcoleman.europa.core.Species;
import com.ojcoleman.europa.util.Stringer;

/**
 * Implements speciation according to the original NEAT speciation method with a few difference:
 * <ul>
 * <li>A target number of species may be specified with {@link #speciationTarget}, in which case the speciation threshold
 * will be adjusted automatically.</li>
 * <li>Individuals are added to the closest matching species, not the first matching species found.</li>
 * <li>It can handle alleles with multiple parameters (not just weight or bias).</li>
 * <li>Parameter values may be normalised (see {@link NEATSpeciator#normaliseParameterValues}) when calculating the distance between
 * two genomes.</li>
 * </ul>
 * 
 * @author O. J. Coleman
 */
public class NEATSpeciatorOriginal extends NEATSpeciator<NEATGenotype, NEATSpecies> {
	private final Logger logger = LoggerFactory.getLogger(NEATSpeciatorOriginal.class);
	
	@Parameter(description = "(Initial) speciation threshold.", defaultValue = "3", minimumValue = "0")
	protected double speciationThreshold;

	@Parameter(description = "Target number of species. If set to a value > -1 then speciationThreshold will be adjusted to maintain this target. If set to 0 defaults to [population size] ^ 0.6 (a bit more than square root of population size).", defaultValue = "0")
	protected int speciationTarget;

	private final Run run;
	private double currentSpeciationThreshold;
	private int lastGenChangedSpeciesCompatThreshold;
	private int countChangedSpeciesCompatThresholdSoonAsAllowed;
	private double previousSpeciationThreshold;

	// Statistics for getStateStatistics()
	private int newSpeciesCount, speciesRemovedCount;

	public NEATSpeciatorOriginal(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		run = this.getParentComponent(Run.class);

		if (speciationTarget == 0) {
			speciationTarget = (int) Math.round(Math.pow(this.getParentComponent(Population.class).getDesiredSize(), 0.6));
			logger.info("Target species count: " + speciationTarget + " (acceptable range: [" + ((int) Math.round(speciationTarget * 0.85)) + ", " + ((int) Math.round(speciationTarget * 1.15)) + "]).");
		}

		currentSpeciationThreshold = speciationThreshold;
	}

	@Override
	public void speciate(Population<NEATGenotype, ?> population, final List<NEATSpecies> speciesList) {
		newSpeciesCount = 0;
		speciesRemovedCount = 0;
		
		// Reference to the component used to perform operations in parallel.
		Parallel parallel = this.getParentComponent(Run.class).getParallel();
		
		// Sort so highest ranked are first as it's probably best to use the highest
		// ranked as the representative of a species when creating new species.
		List<Individual<NEATGenotype, ?>> individuals = new ArrayList<Individual<NEATGenotype, ?>>(population.getMembers());
		Collections.sort(individuals);
		Collections.reverse(individuals);
		
		// If we've just adjusted the threshold, and have done so as soon 
		// as allowed 3 times in a row, re-assign all individuals.
		if (previousSpeciationThreshold != currentSpeciationThreshold && countChangedSpeciesCompatThresholdSoonAsAllowed == 3) {
			// Check that each individual still belongs in it's current species.
			parallel.foreach(individuals, new Parallel.Operation<Individual<NEATGenotype, ?>>() {
				public void perform(Individual<NEATGenotype, ?> ind) {
					if (ind.hasSpecies()) {
						ind.getSpecies().removeMember(ind);
					}
				}
			});
			
			countChangedSpeciesCompatThresholdSoonAsAllowed = 0;
		}
		previousSpeciationThreshold = currentSpeciationThreshold;
		
		final boolean dbg = false; //currentSpeciationThreshold > 100;
		
		if (dbg) System.out.println("currentSpeciationThreshold: " + currentSpeciationThreshold);
		
		// Determine species for each individual that isn't already in a species.
		// We can't parallelise over the list of individuals because we need to process them in rank order, so that
		// as new species are added, subsequent lower-ranked individuals can be matched against the new species.
		final NEATSpeciatorOriginal speciator = this;
		for (Individual<NEATGenotype, ?> ind : individuals) {
			if (!ind.hasSpecies()) {
				// Determine best matching species.
				NEATSpecies match = null;
				for (NEATSpecies species : speciesList) {
					double distance = getDistance(species.representative, ind.genotype);
					
					if (dbg) System.out.println("distance: " + distance); 
					
					// If the individual matches this species.
					if (distance < currentSpeciationThreshold) {
						match = species;
					}
				}
				
				// If we found one or more matching species.
				if (match != null) {
					// Add to closest species.
					match.addMember(ind);
				} else {
					// Otherwise create a new species with this individual's genotype as representative.
					// new NEATSpecies(speciesPrototype, ind.genotype); //newInstance prototype constructor parameter check.
					NEATGenotype genotypeCopy = ind.genotype.newInstance();
					NEATSpecies species = speciesPrototype.newInstance(genotypeCopy);
					species.addMember(ind);
					speciesList.add(species);
					newSpeciesCount++;
				}
			}
		}

		// Remove empty species.
		Iterator<NEATSpecies> speciesIter = speciesList.iterator();
		while (speciesIter.hasNext()) {
			NEATSpecies s = speciesIter.next();
			if (s.isEmpty()) {
				speciesIter.remove();
				speciesRemovedCount++;
			}
			else {
				List<Individual<NEATGenotype, ?>> rankedMembers = new ArrayList<>(s.getMembers());
				Collections.sort(rankedMembers);
				Collections.reverse(rankedMembers);
				Individual<NEATGenotype, ?> fittest = rankedMembers.get(0);
				//System.out.println(s.id + " : " + s.size() + " | " + fittest.id + " : " + fittest.evaluationData.getFitnessResults().values().iterator().next());
			}
		}

		// Attempt to maintain species count target if specified.
		// Don't change threshold too frequently.
		int maxAdjustFreq = 1; //(int) Math.round(Math.pow(population.size(), 0.333));
		if (speciationTarget > 0 && (run.getCurrentIteration() - lastGenChangedSpeciesCompatThreshold >= maxAdjustFreq) && 
				(speciesList.size() > Math.round(speciationTarget * 1.15) || speciesList.size() < Math.round(speciationTarget * 0.85))) {
			// Change the threshold an amount proportional to the discrepancy between target and current species counts.
			double ratio = (double) speciesList.size() / speciationTarget;
			double factor = (ratio - 1) * 0.1 + 1;
			currentSpeciationThreshold = currentSpeciationThreshold * factor;
			
			if (currentSpeciationThreshold < 1) currentSpeciationThreshold = 1;
			
			// Record the number of times in a row the threshold has been changed as soon as allowed. 
			if (run.getCurrentIteration() - lastGenChangedSpeciesCompatThreshold == maxAdjustFreq) {
				countChangedSpeciesCompatThresholdSoonAsAllowed++;
			}
			else {
				countChangedSpeciesCompatThresholdSoonAsAllowed = 0;
			}
			
			lastGenChangedSpeciesCompatThreshold = run.getCurrentIteration();
		}
	}

	/**
	 * Determine if the given Individual matches the given Species, according to whether the distance between the
	 * Individual's genome and the Species representative genome is less than the speciation threshold
	 */
	private boolean match(NEATSpecies species, Individual<NEATGenotype, ?> individual) {
		return getDistance(species.getRepresentative(), individual.genotype) < speciationThreshold;
	}

	@Override
	public List<ComponentStateLog> getState() {
		List<ComponentStateLog> stats = new ArrayList<>();

		stats.add(new ComponentStateLog("Species", "Compatability threshold", currentSpeciationThreshold));
		stats.add(new ComponentStateLog("Species", "Added count", newSpeciesCount));
		stats.add(new ComponentStateLog("Species", "Removed count", speciesRemovedCount));

		return stats;
	}
}

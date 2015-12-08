package com.ojcoleman.europa.evolvers;

import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Evolver;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Run;

/**
 * A default Evolver that selects some proportion of the fittest members ({@link Individual}s) from the population, or
 * each species if applicable, and produces new members via recombination and/or cloning and mutation to replace the
 * current members. If elitism is used, some number of members may be maintained to the next generation unchanged.
 */
public class DefaultEvolver extends Evolver {
	@Parameter (description="The proportion of parents to select from the population or each species, used to generate new genotypes.", defaultValue="0.2", minimumValue="0", maximumValue="1")
	double parentsProportion;
	
	@Parameter (description="The proportion of elites to select from the population or each species, elites coninue to next generation unchanged.", defaultValue="0.05", minimumValue="0", maximumValue="1")
	double elitismProportion;
	
	@Parameter (description="If speciation is used, whether to use fitness sharing for a species when determining the relative number of children to produce from a species.", defaultValue="true")
	boolean speciesFitnessSharing;
	
	
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public DefaultEvolver(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	@Override
	public void evolvePopulation(Population pop, List<List<Individual>> species) {
	}
}

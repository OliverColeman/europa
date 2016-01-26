package com.ojcoleman.europa.core;

import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;

/**
 * A default Evolver that selects some proportion of the fittest members ({@link Individual}s) from the population, or
 * each species if applicable, and produces new members via recombination and/or cloning and mutation to replace the
 * current members. If elitism is used, some number of members may be maintained to the next generation unchanged.
 */
public class DefaultEvolver<G extends Genotype<?>> extends Evolver<G> {
	@IsParameter(description = "The proportion of parents to select from the population or each species, used to generate new genotypes.", defaultValue = "0.2", minimumValue = "0", maximumValue = "1")
	double parentsProportion;

	@IsParameter(description = "The proportion of elites to select from the population or each species, elites coninue to next generation unchanged.", defaultValue = "0.05", minimumValue = "0", maximumValue = "1")
	double elitismProportion;

	@IsParameter(description = "If speciation is used, whether to use fitness sharing for a species when determining the relative number of children to produce from a species.", defaultValue = "true")
	boolean speciesFitnessSharing;

	/**
	 * Constructor for {@link Component}.
	 */
	public DefaultEvolver(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}


	@Override
	public void evolve(Population<G, ?> population) {
		// TODO Auto-generated method stub
		
	}
}
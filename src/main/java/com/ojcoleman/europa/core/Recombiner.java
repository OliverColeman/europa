package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;

/**
 * Base class for all classes that mutate a {@link Genotype}.
 */
public abstract class Recombiner extends ConfigurableComponent {
	@Parameter (description="If set to true then the children produced by this recombiner will not be mutated.", defaultValue="false")
	protected boolean vetoMutation;
	
	@Parameter (description="The relative proportion of children to produce with this recombiner.", defaultValue="1")
	protected double relativeProportion;
	
	
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public Recombiner(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Produce a new genotype from the given genotypes.
	 * 
	 * @param parents The parent genotypes. Two parents will be provided unless a custom {@link Evolver} is used which
	 *            provides more.
	 */
	public abstract Genotype<?> recombine(Genotype<?>... parents);
	
	/**
	 * Returns true iff the children produced by this recombiner should not be mutated.
	 */
	public boolean isMutationVetoed() {
		return vetoMutation;
	}
	
	/**
	 * Get rhe relative proportion of children to produce with this recombiner.
	 */
	public double getRelativeProportion() {
		return relativeProportion;
	}
}

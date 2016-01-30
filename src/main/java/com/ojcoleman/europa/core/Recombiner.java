package com.ojcoleman.europa.core;

import java.util.List;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;

/**
 * Base class for all classes that mutate a {@link Genotype}.
 */
public abstract class Recombiner<G extends Genotype<?>> extends ComponentBase {
	@Parameter(description = "If set to true then the children produced by this recombiner will not be mutated.", defaultValue = "false")
	protected boolean vetoMutation;

	@Parameter(description = "The relative proportion of children to produce with this recombiner.", defaultValue = "1")
	protected double relativeProportion;
	
	
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Recombiner(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Produce a new genotype from the given genotypes. This method must be able to accept at least two parents (and up to {@link #parentCountMaximum()).
	 * 
	 * @param parents The parent genotypes.
	 */
	public abstract G recombine(List<G> parents);

	/**
	 * Return the maximum number of parents that {@link #recombine(Genotype...)} accepts. Must be >= 2.
	 */
	public abstract int parentCountMaximum();
	

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

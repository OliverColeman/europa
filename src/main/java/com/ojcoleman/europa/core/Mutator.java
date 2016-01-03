package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;

/**
 * Base class for all classes that mutate a {@link Genotype}.
 */
public abstract class Mutator<G extends Genotype<?>> extends ConfigurableComponent {
	@Parameter (description="The percentage of clones to apply this mutator to.", defaultValue="1", minimumValue="0", maximumValue="1")
	protected double applyPercentageClones;
	
	@Parameter (description="The percentage of children (produced by a recombiner) to apply this mutator to.", defaultValue="0.25", minimumValue="0", maximumValue="1")
	protected double applyPercentageChildren;
	
	
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public Mutator(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	/**
	 * Mutate the given genotype.
	 */
	public abstract void mutate(G genotype);

	/**
	 * Determine if this mutator should be applied to the given genotype. This may be influenced by randomised processes so can not be relied upon to return the same value for the same genotype.
	 * This default implementation checks if the genotype is a clone of a single parent or a child  of multiple parents and then compares either {@link #applyPercentageClones}
	 * or {@link #applyPercentageChildren} accordingly to a randomly generated number to determine if this mutator should be applied.
	 */
	public boolean shouldMutate(G genotype) {
		if (genotype.parents.size() == 1) {
			return getParentComponent(Run.class).random.nextDouble() < applyPercentageClones;
		}
		else {
			return getParentComponent(Run.class).random.nextDouble() < applyPercentageChildren;
		}
	}
	
	/**
	 * Get the percentage of clones to apply this mutator to, [0, 1].
	 */
	public double getApplyPercentageClones() {
		return applyPercentageClones;
	}

	/**
	 * Get the percentage of children (produced by a {@link Recombiner}) to apply this mutator to, [0, 1].
	 */
	public double getApplyPercentageChildren() {
		return applyPercentageChildren;
	}
}

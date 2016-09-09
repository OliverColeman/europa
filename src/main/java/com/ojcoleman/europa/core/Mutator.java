package com.ojcoleman.europa.core;

import java.util.Random;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;

/**
 * Base class for all classes that mutate a {@link Genotype}.
 */
public abstract class Mutator<G extends Genotype<?>> extends ComponentBase {
	@Parameter(description = "The percentage of individuals to apply this mutator to.", defaultValue = "1", minimumValue = "0", maximumValue = "1")
	protected double overallApplyRate;
	
	@Parameter(description = "The percentage of clones to apply this mutator to (after considering overallApplyRate).", defaultValue = "1", minimumValue = "0", maximumValue = "1")
	protected double applyPercentageClones;

	@Parameter(description = "The percentage of children (produced by a recombiner) to apply this mutator to (after considering overallApplyRate).", defaultValue = "1", minimumValue = "0", maximumValue = "1")
	protected double applyPercentageRecombined;

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Mutator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Mutate the given genotype.
	 */
	public abstract void mutate(G genotype);

	/**
	 * Determine if this mutator should be applied to the given genotype. This may be influenced by randomised processes
	 * so can not be relied upon to return the same value for the same genotype. This default implementation first 
	 * considers {@link #overallApplyRate}, if this test passes then it  checks if
	 * the genotype is a clone of a single parent or a child of multiple parents and then compares either
	 * {@link #applyPercentageClones} or {@link #applyPercentageRecombined} accordingly to a randomly generated number
	 * to determine if this mutator should be applied.
	 */
	public boolean shouldMutate(G genotype) {
		Random random = getParentComponent(Run.class).random;
		
		if (random.nextDouble() >= overallApplyRate){
			return false;
		}
		
		if (genotype.parents.size() == 1) {
			return random.nextDouble() < applyPercentageClones;
		} else {
			return random.nextDouble() < applyPercentageRecombined;
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
		return applyPercentageRecombined;
	}
}

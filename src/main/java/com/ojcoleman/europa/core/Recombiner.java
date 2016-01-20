package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;

/**
 * Base class for all classes that mutate a {@link Genotype}.
 */
public abstract class Recombiner<G extends Genotype> extends Component {
	@IsParameter(description = "If set to true then the children produced by this recombiner will not be mutated.", defaultValue = "false")
	protected boolean vetoMutation;

	@IsParameter(description = "The relative proportion of children to produce with this recombiner.", defaultValue = "1")
	protected double relativeProportion;

	/**
	 * Constructor for {@link Component}.
	 */
	public Recombiner(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Produce a new genotype from the given genotypes.
	 * 
	 * @param parents The parent genotypes. Two parents will be provided unless a custom {@link Evolver} is used which
	 *            provides more.
	 */
	public abstract G recombine(G... parents);

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

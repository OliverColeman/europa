package com.ojcoleman.europa.algos.neat;

import java.util.Iterator;

import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Species;

/**
 * @author O. J. Coleman
 *
 */
public class NEATSpecies extends Species<NEATGenotype> {
	/**
	 * The representative for this species.
	 */
	protected NEATGenotype representative;
	
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public NEATSpecies(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param representative The representative for the new species.
	 */
	public NEATSpecies(NEATSpecies prototype, NEATGenotype representative) {
		super(prototype);
		this.representative = representative;
	}
	
	/**
	 * Get the representative for this species.
	 */
	public NEATGenotype getRepresentative() {
		return representative;
	}

	/**
	 * Set the representative for this species.
	 */
	public void setRepresentative(NEATGenotype representative) {
		this.representative = representative;
	}
}

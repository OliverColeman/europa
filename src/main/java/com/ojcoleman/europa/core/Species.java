package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Prototype;

/**
 * Represents a species in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public class Species<G extends Genotype<?>, F extends Function<?, ?>> extends Prototype {
	/**
	 * Prototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public Species(JsonObject config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 */
	public Species(Species<G, F> prototype) {
		super(prototype);
	}
}

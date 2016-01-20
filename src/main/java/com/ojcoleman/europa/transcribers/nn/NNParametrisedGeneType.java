package com.ojcoleman.europa.transcribers.nn;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;

/**
 * Encapsulates the configuration for a type of {@link Gene} and/or {Allele} that defines a set of evolvable parameters
 * for a component type in a neural network, such as the neurons or synapses.
 * 
 * @author O. J. Coleman
 */
public class NNParametrisedGeneType extends ParametrisedGeneType {
	public NNParametrisedGeneType(JsonObject config) throws Exception {
		super(config);
	}

	@IsParameter(description = "The class of the model to use for the neural network component.")
	protected Class<?> modelClass;
}

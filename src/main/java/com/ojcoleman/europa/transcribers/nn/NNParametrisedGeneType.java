package com.ojcoleman.europa.transcribers.nn;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.genotypes.ParametrisedGeneType;

/**
 * Encapsulates the configuration for a type of {@link Gene} and/or {Allele} that defines a set of 
 * evolvable parameters for a component type in a neural network, such as the neurons or synapses.
 * 
 * @author O. J. Coleman
 */
public class NNParametrisedGeneType extends ParametrisedGeneType {
	public NNParametrisedGeneType(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Parameter(description="The class of the model to use for the neural network component.")
	protected Class<?> modelClass;	
}

package com.ojcoleman.europa.transcribers.nn.integration;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuronGeneType;

/**
 * Encapsulates the configuration for a neuron {@link com.ojcoleman.europa.core.Gene} and/or {@link com.ojcoleman.europa.core.Allele} that defines a set of evolvable parameters
 * for a neuron component in a Bain neural network.
 * 
 * @author O. J. Coleman
 */
public class BainParametrisedNeuronGeneType extends ParametrisedNeuronGeneType {
	public BainParametrisedNeuronGeneType(Configuration config) throws Exception {
		super(config);
	}

	@Parameter(description = "The class of the model to use for the neural network component, if applicable.")
	protected Class<? extends ComponentCollection> modelClass;

	/**
	 * Returns the class to use for the neural network component, if applicable/specified.
	 */
	public Class<? extends ComponentCollection> getModelClass() {
		return modelClass;
	}
}

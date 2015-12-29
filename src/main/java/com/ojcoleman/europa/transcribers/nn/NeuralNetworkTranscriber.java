package com.ojcoleman.europa.transcribers.nn;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.SubComponent;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.transcribers.VectorFunctionTranscriber;

/**
 * Base class for transcribers that generate neural networks.
 * 
 * @author O. J. Coleman
 */
public abstract class NeuralNetworkTranscriber<G extends Genotype<?>> extends VectorFunctionTranscriber<G> {
	@SubComponent (description="Configuration for the neurons.", defaultImplementation=NNParametrisedGeneType.class)
	protected NNConfig config;
	
	@Parameter (description="The class that will construct a neural network. Must extend com.ojcoleman.europa.transcribers.nn.", defaultValue="com.ojcoleman.europa.integration.BainNN")
	protected NNWrapper nnWrapper;

	public NeuralNetworkTranscriber(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	
}

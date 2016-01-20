package com.ojcoleman.europa.transcribers.nn;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.VectorFunctionTranscriber;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.IsComponent;
import com.ojcoleman.europa.core.Genotype;

/**
 * Base class for transcribers that generate neural networks.
 * 
 * @author O. J. Coleman
 */
public abstract class NeuralNetworkTranscriber<G extends Genotype<?>> extends VectorFunctionTranscriber<G> {
	public NeuralNetworkTranscriber(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Returns the neural network prototype instance for networks produced by this transcriber.
	 */
	public abstract NeuralNetwork getNeuralNetworkPrototype();
}

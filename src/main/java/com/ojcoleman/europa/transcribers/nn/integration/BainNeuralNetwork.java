package com.ojcoleman.europa.transcribers.nn.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.IsConfigurable;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.NeuralNetwork;

/**
 * A wrapper for Bain neural networks.
 */
public class BainNeuralNetwork extends NeuralNetwork {
	@IsConfigurable (description="The general or base configuration for the neural networks.")
	protected BainNNConfig config;
	
	
	/**
	 * Prototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public BainNeuralNetwork(JsonObject config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}.
	 * 
	 * Create a Bain neural network with the given base configuration and instance configuration.
	 * @param prototype The prototype neural network instance to copy.
	 * @param instConfig The instance configuration for the neural network, for example number of inputs and outputs.
	 */
	public BainNeuralNetwork(BainNeuralNetwork prototype, NNInstanceConfig instConfig) {
		super(prototype, instConfig);
	}
	
	
	@Override
	public BainNNConfig getConfig() {
		return config;
	}
	
	
	/**
	 * Add a neuron to this neural network.
	 * 
	 * @param config Neuron model-specific configuration parameters, if applicable. This will typically contain a "bias"
	 *            value. If types are used then the config must contain a value for "typeReference" that will refer to
	 *            the value returned by {@link #addNeuronType(Map)}.
	 * @return The index of the new neuron in this neural network. This is used to reference a neuron, for example in
	 *         {@link #addSynapse(Map, int, int)}.
	 */
	@Override
	public int addNeuron(Map<String, Double> config) {
		return 0;
	}

	/**
	 * Add a synapse to this neural network.
	 * 
	 * @param config Synapse model-specific configuration parameters, if applicable. This will typically contain at
	 *            least a "weight" value. If types are used then the config must contain a value for "typeReference"
	 *            that will refer to the value returned by {@link #addNeuronType(Map)}.
	 * @param source The index of the source neuron for the synapse, as returned by {@link #addNeuron(Map)}.
	 * @param dest The index of the destination neuron for the synapse, as returned by {@link #addNeuron(Map)}.
	 * @return The index of the new synapse in this neural network.
	 */
	@Override
	public void addSynapse(Map<String, Double> config, int source, int dest) {
		
	}

	@Override
	public double[] apply(double[] input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] apply(double[][] input) {
		// TODO Auto-generated method stub
		return null;
	}
}

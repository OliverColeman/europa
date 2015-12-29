package com.ojcoleman.europa.transcribers.nn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ojcoleman.europa.functiontypes.VectorFunction;

/**
 * A wrapper or decorator for all neural network implementations.
 */
public abstract class NNWrapper implements VectorFunction {
	protected final List<Map<String, Double>> neuronTypes;
	protected final List<Map<String, Double>> synapseTypes;

	/**
	 * Create a neural network with the given configuration.
	 */
	public NNWrapper(NNConfig config, NNInstanceConfig instConfig) {
		neuronTypes = new ArrayList<Map<String, Double>>();
		synapseTypes = new ArrayList<Map<String, Double>>();
	}

	/**
	 * Add a neuron type to this neural network.
	 * 
	 * @param config Neuron model-specific configuration parameters, if applicable.
	 * @return The index of the new neuron type in this neural network. This is used to reference a neuron type, for
	 *         example in {@link #addNeuron(Map, int)}.
	 */
	public int addNeuronType(Map<String, Double> config) {
		neuronTypes.add(config);
		return neuronTypes.size() - 1;
	}

	/**
	 * Add a synapse type to this neural network.
	 * 
	 * @param config Synapse model-specific configuration parameters, if applicable.
	 * @return The index of the new synapse type in this neural network. This is used to reference a synapse type, for
	 *         example in {@link #addSynapse(Map, int, int, int)}.
	 */
	public int addSynapseType(Map<String, Double> config) {
		synapseTypes.add(config);
		return synapseTypes.size() - 1;
	}

	/**
	 * Add a neuron to this neural network.
	 * 
	 * @param config Neuron model-specific configuration parameters, if applicable. This will typically contain a "bias"
	 *            value. If types are used then the config will contain a value for "typeReference" that will refer to
	 *            the value returned by {@link #addNeuronType(Map)}.
	 * @return The index of the new neuron in this neural network. This is used to reference a neuron, for example in
	 *         {@link #addSynapse(Map, int, int, int)}.
	 */
	public abstract int addNeuron(Map<String, Double> config);

	/**
	 * Add a synapse to this neural network.
	 * 
	 * @param config Synapse model-specific configuration parameters, if applicable. This will typically contain at
	 *            least a "weight" value. If types are used then the config will contain a value for "typeReference"
	 *            that will refer to the value returned by {@link #addNeuronType(Map)}.
	 * @param source The index of the source neuron for the synapse, as returned by {@link #addNeuron(Map, int)}.
	 * @param dest The index of the destination neuron for the synapse, as returned by {@link #addNeuron(Map, int)}.
	 * @return The index of the new synapse in this neural network.
	 */
	public abstract void addSynapse(Map<String, Double> config, int source, int dest);
}

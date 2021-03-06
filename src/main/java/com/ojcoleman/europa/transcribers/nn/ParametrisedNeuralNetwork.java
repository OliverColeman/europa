package com.ojcoleman.europa.transcribers.nn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Stringable;

/**
 * <p>
 * A wrapper for neural network implementations whose configuration is defined by a set of labelled numeric parameters.
 * The wrapper specifies methods for constructing the network, and implements the {@link VectorFunction} interface to
 * represent applying input to the network and reading the associated output.
 * </p>
 * <p>
 * The wrapper supports the notion of neuron and synapse types. A type is defined by a specific configuration (a set of
 * values for some parameters), and a neuron or synapse that references that type will adopt that configuration. See
 * {@link ParametrisedGeneType#getParamsType()} for more information.
 * </p>
 */
public abstract class ParametrisedNeuralNetwork extends PrototypeBase implements VectorFunction, Stringable {
	/**
	 * The instance configuration for the neural network to be built.
	 */
	public final NNInstanceConfig instanceConfig;

	/**
	 * The neuron type configurations.
	 */
	protected List<Map<String, Double>> neuronTypes;

	/**
	 * The synapse type configurations.
	 */
	protected List<Map<String, Double>> synapseTypes;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(Configuration)}.
	 */
	public ParametrisedNeuralNetwork(Configuration config) {
		super(config);
		instanceConfig = null;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
	 * 
	 * Create a neural network with the given instance configuration.
	 *
	 * @param prototype The prototype neural network instance to copy.
	 * @param instConfig The instance configuration for the new neural network, for example specifying the number of
	 *            inputs and outputs.
	 */
	public ParametrisedNeuralNetwork(ParametrisedNeuralNetwork prototype, NNInstanceConfig instConfig) {
		super(prototype);

		this.instanceConfig = instConfig;

		neuronTypes = new ArrayList<>();
		synapseTypes = new ArrayList<>();
	}

	/**
	 * @return The base or general configuration for the neural network.
	 */
	public abstract NNConfig<?, ?> getConfig();

	/**
	 * Add a neuron type to this neural network.
	 * 
	 * @param nnConfig Neuron model-specific configuration parameters, if applicable.
	 * @return The index of the new neuron type in this neural network. This is used to reference a neuron type, for
	 *         example in {@link #addNeuron(Map)}.
	 */
	public int addNeuronType(Map<String, Double> config) {
		neuronTypes.add(config);
		return neuronTypes.size() - 1;
	}

	/**
	 * Add a synapse type to this neural network.
	 * 
	 * @param nnConfig Synapse model-specific configuration parameters, if applicable.
	 * @return The index of the new synapse type in this neural network. This is used to reference a synapse type, for
	 *         example in {@link #addSynapse(Map, int, int)}.
	 */
	public int addSynapseType(Map<String, Double> config) {
		synapseTypes.add(config);
		return synapseTypes.size() - 1;
	}

	/**
	 * Add a neuron to this neural network. Neurons should be added in order of input, hidden and output.
	 * 
	 * @param nnConfig Neuron model-specific configuration parameters, if applicable. This will typically contain a "bias"
	 *            value. If types are used then the nnConfig should generally contain a reference to a type according to
	 *            the values returned by {@link #addNeuronType(Map)}.
	 * @return The index of the new neuron in this neural network. This is used to reference a neuron, for example in
	 *         {@link #addSynapse(Map, int, int)}.
	 */
	public abstract int addNeuron(Map<String, Double> config);

	/**
	 * Add a synapse to this neural network.
	 * 
	 * @param nnConfig Synapse model-specific configuration parameters, if applicable. This will typically contain at
	 *            least a "weight" value. If types are used then the nnConfig should generally contain a reference to a
	 *            type according to the values returned by {@link #addSynapseType(Map)}.
	 * @param source The index of the source neuron for the synapse, as returned by {@link #addNeuron(Map)}.
	 * @param dest The index of the destination neuron for the synapse, as returned by {@link #addNeuron(Map)}.
	 */
	public abstract void addSynapse(Map<String, Double> config, int source, int dest);

	/**
	 * This method should be called by a {@link com.ojcoleman.europa.core.Transcriber} when the neural network is completed (when there will be no
	 * more calls to {@link #addNeuron} or {@link #addSynapse(Map, int, int)}. Sub-classes may override this method in
	 * order to perform any clean-up or other initialisation tasks.
	 */
	public void finishedBuilding() {
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("instanceConfig", instanceConfig);
		map.put("neuronTypes", neuronTypes);
		map.put("synapseTypes", synapseTypes);
	}
}

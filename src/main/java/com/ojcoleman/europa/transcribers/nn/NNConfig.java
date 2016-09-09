package com.ojcoleman.europa.transcribers.nn;

import com.ojcoleman.europa.configurable.ConfigurableBase;
import com.ojcoleman.europa.configurable.Configuration;

import java.util.Map;

import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.transcribers.nn.integration.BainParametrisedNeuronGeneType;

/**
 * A component describing essential configuration information for a neural network for which the configuration is
 * defined by {@link BainParametrisedNeuronGeneType}s.
 * 
 * @author O. J. Coleman
 */
public class NNConfig<N extends ParametrisedNeuronGeneType, S extends ParametrisedGeneType> extends ConfigurableBase {
	@Configurable(description = "Configuration for the neurons.")
	protected N neuron;

	@Configurable(description = "Configuration for the synapses.")
	protected S synapse;

	@Parameter(description = "The basic allowable topology class of the networks, \"recurrent\" or \"feed_forward\"", defaultValue = "feed_forward")
	protected Topology topology;

	@Parameter(description = "For recurrent neural networks, the number of simulation steps to perform for each application of the input and reading of the output.", defaultValue = "5")
	protected int simulationStepsPerStep;

	public NNConfig(Configuration config) throws Exception {
		super(config);
	}

	/**
	 * @return the configuration for the neurons.
	 */
	public N neuron() {
		return neuron;
	}

	/**
	 * @return the configuration for the synapses.
	 */
	public S synapse() {
		return synapse;
	}

	/**
	 * @return the basic allowable topology type of the networks.
	 */
	public Topology getTopology() {
		return topology;
	}

	/**
	 * @return for recurrent neural networks, the number of simulation steps to perform for each application of the
	 *         input and reading of the output.
	 */
	public int getSimulationStepsPerStep() {
		return simulationStepsPerStep;
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("neuron", neuron);
		map.put("synapse", synapse);
		map.put("topology", topology);
		map.put("simulationStepsPerStep", simulationStepsPerStep);
	}
}

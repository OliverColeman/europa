package com.ojcoleman.europa.transcribers.nn;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.IsConfigurable;
import com.ojcoleman.europa.configurable.IsParameter;

/**
 * A component describing essential configuration information for a neural network for which the configuration is defined by {@link NNParametrisedGeneType}s.
 * 
 * @author O. J. Coleman
 */
public class NNConfig extends Configurable {
	@IsConfigurable(description = "Configuration for the neurons.")
	protected NNParametrisedGeneType neuron;

	@IsConfigurable(description = "Configuration for the synapses.")
	protected NNParametrisedGeneType synapse;

	@IsParameter(description = "The basic allowable topology type of the networks, \"recurrent\" or \"feed_forward\"", defaultValue = "feed_forward")
	protected Topology topology;

	@IsParameter(description = "For recurrent neural networks, the number of simulation steps to perform for each application of the input and reading of the output.", defaultValue = "5")
	protected int simulationStepsPerStep;

	
	public NNConfig(JsonObject config) throws Exception {
		super(config);
	}
	

	/**
	 * @return the configuration for the neurons.
	 */
	public NNParametrisedGeneType neuron() {
		return neuron;
	}

	/**
	 * @return the configuration for the synapses.
	 */
	public NNParametrisedGeneType synapse() {
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
}

package com.ojcoleman.europa.transcribers.nn;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.SubComponent;

/**
 * A component describing essential configuration information for a neural network.
 * 
 * @author O. J. Coleman
 */
public class NNConfig extends ConfigurableComponent {
	@SubComponent (description="Configuration for the neurons.", defaultImplementation=NNParametrisedGeneType.class)
	protected NNParametrisedGeneType neuron;

	@SubComponent (description="Configuration for the synapses.", defaultImplementation=NNParametrisedGeneType.class)
	protected NNParametrisedGeneType synapse;
	
	@Parameter (description="The basic allowable topology type of the networks, \"recurrent\" or \"feed_forward\"", defaultValue="feed_forward")
	protected Topology topology;
	
	@Parameter (description="For recurrent neural networks, the number of simulation steps to perform for each application of the input and reading of the output.", defaultValue="5") 
	protected int simulationStepsPerStep;
	
	
	public NNConfig(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	
	/**
	 * @return the configuration for the neurons.
	 */
	public NNParametrisedGeneType neuronConfig() {
		return neuron;
	}

	/**
	 * @return the configuration for the synapses.
	 */
	public NNParametrisedGeneType synapseConfig() {
		return synapse;
	}

	/**
	 * @return the basic allowable topology type of the networks.
	 */
	public Topology getTopology() {
		return topology;
	}

	/**
	 * @return for recurrent neural networks, the number of simulation steps to perform for each application of the input and reading of the output.
	 */
	public int getSimulationStepsPerStep() {
		return simulationStepsPerStep;
	}
}

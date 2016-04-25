package com.ojcoleman.europa.transcribers.nn;

import java.util.Map;

import com.ojcoleman.europa.core.Stringable;
import com.ojcoleman.europa.functiontypes.VectorFunction;

/**
 * Container for basic configuration metadata for neural networks. Sub-classes may extend this for neural network
 * implementations requiring other configuration metadata.
 * 
 * @see ParametrisedNeuralNetwork
 * 
 * @author O. J. Coleman
 */
public class NNInstanceConfig implements Stringable {
	/**
	 * The total number of neurons in the network.
	 */
	public final int neuronCount;

	/**
	 * The total number of synapses in the network.
	 */
	public final int synapseCount;

	/**
	 * The number of input neurons in the network.
	 */
	public final int inputCount;

	/**
	 * The number of output neurons in the network.
	 */
	public final int outputCount;

	/**
	 * If applicable, the number of simulation steps to perform for each request to apply input to the network and
	 * retrieve output, for example via {@link VectorFunction#apply(double[])}.
	 */
	public final int stepsPerStep;

	/**
	 * A name/identifier for the network.
	 */
	public String name;

	/**
	 * Creates an uninitialised NNInstanceConfig.
	 */
	public NNInstanceConfig() {
		this.neuronCount = 0;
		this.synapseCount = 0;
		this.inputCount = 0;
		this.outputCount = 0;
		this.stepsPerStep = 0;
		this.name = null;
	}

	/**
	 * Creates an NNInstanceConfig initialised with the given metadata, and {@link #stepsPerStep} set to 1.
	 */
	public NNInstanceConfig(int neuronCount, int synapseCount, int inputCount, int outputCount, String name) {
		this.neuronCount = neuronCount;
		this.synapseCount = synapseCount;
		this.inputCount = inputCount;
		this.outputCount = outputCount;
		this.stepsPerStep = 1;
		this.name = name;
	}

	/**
	 * Creates an NNInstanceConfig initialised with the given metadata.
	 */
	public NNInstanceConfig(int neuronCount, int synapseCount, int inputCount, int outputCount, int stepsPerStep, String name) {
		this.neuronCount = neuronCount;
		this.synapseCount = synapseCount;
		this.inputCount = inputCount;
		this.outputCount = outputCount;
		this.stepsPerStep = stepsPerStep;
		this.name = name;
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("neuronCount", neuronCount);
		map.put("synapseCount", synapseCount);
		map.put("inputCount", inputCount);
		map.put("outputCount", outputCount);
		map.put("stepsPerStep", stepsPerStep);
		map.put("name", name);
	}
}

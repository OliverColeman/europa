package com.ojcoleman.europa.transcribers.nn;

/**
 * Container for basic configuration info for neural networks.
 * Sub-classes may extend this for neural network implementations requiring other configuration info.
 * 
 * @see NNWrapper
 * 
 * @author O. J. Coleman
 */
public class NNInstanceConfig {
	/**
	 * The total number of neurons in the network.
	 */
	public int neuronCount;
	
	/**
	 * The total number of synapes in the network.
	 */
	public int synapseCount;
	
	/**
	 * The number of input neurons in the network.
	 */
	public int inputCount;
	
	/**
	 * The number of output neurons in the network.
	 */
	public int outputCount;
	
	/**
	 * A name/identifier for the network.
	 */
	public String name;
	
	/**
	 * Creates an uninitialised NNInstanceConfig.
	 */
	public NNInstanceConfig() {
		
	}
	
	/**
	 * Creates an NNInstanceConfig initialised with the given info.
	 */
	public NNInstanceConfig(int neuronCount, int synapseCount, int inputCount, int outputCount, String name) {
		this.neuronCount = neuronCount;
		this.synapseCount = synapseCount;
		this.inputCount = inputCount;
		this.outputCount = outputCount;
		this.name = name;
	}
	
	
}

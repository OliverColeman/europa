package com.ojcoleman.europa.algos.neat;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.transcribers.nn.NNConfig;

/**
 * Represents the mutable parameters of a neuron in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronAllele extends NEATAllele<NEATNeuronGene> {
	/**
	 * Prototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public NEATNeuronAllele(JsonObject config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * NEATNeuronAllele referencing the same underlying Gene but storing an independent copy of the original parameter Vector.
	 * 
	 * @param prototype The allele to copy.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public NEATNeuronAllele(NEATNeuronAllele prototype) {
		super(prototype);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * NEATNeuronAllele with the specified underlying Gene and storing the specified parameter Vector.
	 * 
	 * @param prototype The allele to copy.
	 * @param gene the underlying gene for the new allele.
	 * @param paramVector The parameter vector for the new allele, copied by reference.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public NEATNeuronAllele(NEATNeuronAllele prototype, NEATNeuronGene gene, Vector paramVector) {
		super(prototype, gene, paramVector);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * NEATNeuronAllele based on a specified neural network configuration and with parameter values initialised to 0.
	 * 
	 * @param gene The gene to underlie the new allele.
	 * @param nnConfig The configuration parameters for the neural network.
	 */
	private NEATNeuronAllele(NEATNeuronAllele prototype, NEATNeuronGene gene, NNConfig nnConfig) {
		super(prototype, gene, nnConfig.neuron().createAlleleVector());
	}
}

package com.ojcoleman.europa.algos.neat;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Represents the mutable parameters of a synapse or connection in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATSynapseAllele extends NEATAllele<NEATSynapseGene> {
	/**
	 * Prototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public NEATSynapseAllele(JsonObject config) {
		super(config);
	}
	
	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * NEATSynapseAllele referencing the same underlying Gene but storing an independent copy of the original parameter Vector.
	 * 
	 * @param prototype The allele to copy.
	 */
	public NEATSynapseAllele(NEATSynapseAllele prototype) {
		super(prototype);
	}


	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * NEATSynapseAllele with the specified underlying Gene and storing the specified parameter Vector.
	 * 
	 * @param prototype The allele to copy.
	 * @param gene the underlying gene for the new allele.
	 * @param paramVector The parameter vector for the new allele, copied by reference.
	 */
	public NEATSynapseAllele(NEATSynapseAllele prototype, NEATSynapseGene gene, Vector paramVector) {
		super(prototype, gene, paramVector);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * NEATSynapseAllele with the specified underlying Gene and with parameters based on a specified neural network
	 * configuration (and initialised to 0).
	 * 
	 * @param gene The gene to underlie the new allele.
	 * @param nnConfig The configuration parameters for the neural network.
	 */
	private NEATSynapseAllele(NEATSynapseAllele prototype, NEATSynapseGene gene, NNConfig nnConfig) {
		super(prototype, gene, nnConfig.neuron().createAlleleVector());
	}
}

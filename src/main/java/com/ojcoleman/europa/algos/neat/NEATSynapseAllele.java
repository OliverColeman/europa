package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
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
	 * @see Allele#Allele(Allele)
	 */
	public NEATSynapseAllele(NEATSynapseAllele allele) {
		super(allele);
	}

	/**
	 * Create a NEATSynapseAllele with the specified parameter values.
	 * 
	 * @param gene The gene this allele is for.
	 * @param paramVector The parameter values for this synapse.
	 */
	public NEATSynapseAllele(NEATSynapseGene gene, Vector paramVector) {
		super(gene, paramVector);
	}

	/**
	 * Create a new NEATSynapseAllele based on a specified neural network configuration and with parameter values
	 * initialised to 0.
	 * 
	 * @param gene The gene to underlie the new allele.
	 * @param nnConfig The configuration parameters for the neural network.
	 */
	private NEATSynapseAllele(NEATSynapseGene gene, NNConfig nnConfig) {
		super(gene, new Vector(nnConfig.synapse().getParamsAllele()));
	}
}

package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Represents the mutable parameters of a neuron in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronAllele extends NEATAllele<NEATNeuronGene> {
	/**
	 * @see Allele#Allele(Allele)
	 */
	public NEATNeuronAllele(NEATNeuronAllele allele) {
		super(allele);
	}

	/**
	 * Create a NEATNeuronAllele with values initialised to 0.
	 * 
	 * @param gene The gene this allele is for.
	 * @param paramVector The parameter values for this synapse.
	 */
	public NEATNeuronAllele(NEATNeuronGene gene, Vector paramVector) {
		super(gene, paramVector);
	}

	/**
	 * Create a new NEATNeuronAllele based on a specified neural network configuration and with parameter values
	 * initialised to 0.
	 * 
	 * @param gene The gene to underlie the new allele.
	 * @param nnConfig The configuration parameters for the neural network.
	 */
	private NEATNeuronAllele(NEATNeuronGene gene, NNConfig nnConfig) {
		super(gene, new Vector(nnConfig.neuron().getParamsAllele()));
	}
}

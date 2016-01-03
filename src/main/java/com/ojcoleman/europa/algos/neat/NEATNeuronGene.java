package com.ojcoleman.europa.algos.neat;

import java.util.Set;

import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Represents a neuron in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronGene extends NEATGene {
	/**
	 * Create a new NEATSynapseGene based on a specified neural network configuration.
	 * 
	 * @param type The type of the neuron. Generally one of {@link NNPart#NEURON_INPUT}, {@link NNPart#NEURON_HIDDEN}, {@link NNPart#NEURON_OUTPUT}.
	 * @param geneID The ID of the gene to be created.
	 * @param paramVector The parameter vector for this neuron.
	 */
	NEATNeuronGene(NNPart type, long geneID, Vector paramVector) {
		super(Gene.typeSet(NNPart.NEURON, type), geneID, paramVector);
	}
}

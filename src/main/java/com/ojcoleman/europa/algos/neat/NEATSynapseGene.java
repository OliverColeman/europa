package com.ojcoleman.europa.algos.neat;

import java.util.Set;

import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Represents a synapse or connection in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATSynapseGene extends NEATGene {
	/**
	 * The NEAT ID of the source neuron gene.
	 */
	public final long sourceID;
	
	/**
	 * The NEAT ID of the destination neuron gene.
	 */
	public final long destinationID;
	

	/**
	 * Create a new NEATSynapseGene based on a specified neural network configuration.
	 * The parameter values, if any, are initialised with random values.
	 * 
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT algorithm.
	 * @param paramVector The parameter vector for this synapse.
	 * @param sourceID The NEAT ID of the source neuron gene.
	 * @param destinationID The NEAT ID of the destination neuron gene.
	 * 
	 */
	NEATSynapseGene( long id, Vector paramVector, long sourceID, long destinationID) {
		super(Gene.typeSet(NNPart.SYNAPSE), id, paramVector);
		this.sourceID = sourceID;
		this.destinationID = destinationID;
	}
}

package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.util.VectorInfo;

/**
 * Represents a synapse or connection in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATConnectionGene extends NEATGene {
	/**
	 * The NEAT ID of the source neuron gene.
	 */
	public final long sourceID;
	
	/**
	 * The NEAT ID of the destination neuron gene.
	 */
	public final long destinationID;
	
	/**
	 * Create a NEATGene with values initialised to 0.
	 * 
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT algorithm.
	 * @param info The information about each element of the vector representing this gene.
	 * @param sourceID The NEAT ID of the source neuron gene.
	 * @param destinationID The NEAT ID of the destination neuron gene.
	 */
	public NEATConnectionGene(Object type, long id, VectorInfo info, long sourceID, long destinationID) {
		super(type, id, info);
		this.sourceID = sourceID;
		this.destinationID = destinationID;
	}
	
	/**
	 * Create a NEATGene with the specified values.
	 * 
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT algorithm.
	 * @param info The information about each element of the vector representing this gene.
	 * @param values The values of the gene.
	 * @param sourceID The NEAT ID of the source neuron gene.
	 * @param destinationID The NEAT ID of the destination neuron gene.
	 */
	public NEATConnectionGene(Object type, long id, VectorInfo info, double[] values, long sourceID, long destinationID) {
		super(type, id, info, values);
		this.sourceID = sourceID;
		this.destinationID = destinationID;
	}
}

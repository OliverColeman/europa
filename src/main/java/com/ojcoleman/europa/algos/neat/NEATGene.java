package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.genotypes.VectorGene;
import com.ojcoleman.europa.util.VectorInfo;

/**
 * Base class for representations of NEAT genes.
 * 
 * @author O. J. Coleman
 */
public class NEATGene extends VectorGene implements Comparable<NEATGene> {
	/**
	 * The NEAT innovation ID.
	 */
	public final long id;
	
	/**
	 * Create a NEATGene with values initialised to 0.
	 * 
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT algorithm.
	 * @param info The information about each element of the vector representing this gene.
	 */
	public NEATGene(Object type, long id, VectorInfo info) {
		super(type, info);
		this.id = id;
	}
	
	/**
	 * Create a NEATGene with the specified values.
	 * 
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT algorithm.
	 * @param info The information about each element of the vector representing this gene.
	 * @param values The values of the gene.
	 */
	public NEATGene(Object type, long id, VectorInfo info, double[] values) {
		super(type, info, values);
		this.id = id;
	}
	
	@Override
	public int compareTo(NEATGene other) {
		if (id < other.id) return -1;
		if (id > other.id) return 1;
		return 0;
	}
}

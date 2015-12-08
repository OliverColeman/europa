package com.ojcoleman.europa.genotypes;


/**
 * Base class for representations of NEAT genes.
 * 
 * @author O. J. Coleman
 */
public class NEATGene extends VectorGene implements Comparable<NEATGene> {
	final long id;
	
	public NEATGene(long id, VectorInfo info, double[] values) {
		super(info, values);
		this.id = id;
	}

	@Override
	public int compareTo(NEATGene other) {
		if (id < other.id) return -1;
		if (id > other.id) return 1;
		return 0;
	}
}

package com.ojcoleman.europa.genotypes;

/**
 * Base class for representations of NEAT alleles.
 *  
 * @author O. J. Coleman
 */
public class NEATAllele extends VectorAllele<NEATGene> implements Comparable<NEATAllele> {
	public NEATAllele(NEATGene gene, VectorInfo info, double[] values) {
		super(gene, info, values);
	}

	@Override
	public int compareTo(NEATAllele other) {
		return gene.compareTo(other.gene);
	}

}

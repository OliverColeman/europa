package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.genotypes.VectorAllele;
import com.ojcoleman.europa.util.VectorInfo;

/**
 * Base class for representations of NEAT alleles.
 *  
 * @author O. J. Coleman
 */
public class NEATAllele<T extends NEATGene> extends VectorAllele<T> implements Comparable<NEATAllele<T>> {
	/**
	 * @see Allele#Allele(Allele)
	 */
	public NEATAllele(NEATAllele<T> allele) {
		super(allele);
	}
	
	/**
	 * Create a NEATAllele with values initialised to 0.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 */
	public NEATAllele(T gene, VectorInfo info) {
		super(gene, info);
	}

	/**
	 * Create a NEATAllele with values initialised to those specified.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 * @param values The initial values of the allele.
	 */
	public NEATAllele(T gene, VectorInfo info, double[] values) {
		super(gene, info, values);
	}
	
	@Override
	public int compareTo(NEATAllele<T> other) {
		return gene.compareTo(other.gene);
	}
}

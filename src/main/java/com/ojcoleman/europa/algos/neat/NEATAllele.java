package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorAllele;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNPart;

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
	 * @param vector The parameter vector for this allele.
	 */
	public NEATAllele(T gene, Vector vector) {
		super(gene, vector);
	}
	
	@Override
	public int compareTo(NEATAllele<T> other) {
		return gene.compareTo(other.gene);
	}
}

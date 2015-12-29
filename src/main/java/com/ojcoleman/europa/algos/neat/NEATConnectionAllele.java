package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.util.VectorInfo;

/**
 * Represents a synapse or connection in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATConnectionAllele extends NEATAllele<NEATConnectionGene> {
	/**
	 * @see Allele#Allele(Allele)
	 */
	public NEATConnectionAllele(NEATConnectionAllele allele) {
		super(allele);
	}
		
	/**
	 * Create a NEATConnectionAllele with values initialised to 0.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 */
	public NEATConnectionAllele(NEATConnectionGene gene, VectorInfo info) {
		super(gene, info);
	}

	/**
	 * Create a NEATConnectionAllele with values initialised to those specified.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 * @param values The initial values of the allele.
	 */
	public NEATConnectionAllele(NEATConnectionGene gene, VectorInfo info, double[] values) {
		super(gene, info, values);
	}
}

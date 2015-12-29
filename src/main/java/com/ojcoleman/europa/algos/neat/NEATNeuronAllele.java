package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.util.VectorInfo;

/**
 * Represents a neuron in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronAllele extends NEATAllele<NEATGene> {
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
	 * @param info The information about each element of the vector representing this allele.
	 */
	public NEATNeuronAllele(NEATGene gene, VectorInfo info) {
		super(gene, info);
	}

	/**
	 * Create a NEATNeuronAllele with values initialised to those specified.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 * @param values The initial values of the allele.
	 */
	public NEATNeuronAllele(NEATGene gene, VectorInfo info, double[] values) {
		super(gene, info, values);
	}
}

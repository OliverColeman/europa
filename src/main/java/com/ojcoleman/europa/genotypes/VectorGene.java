package com.ojcoleman.europa.genotypes;

import java.util.Collections;
import java.util.List;

import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.util.VectorInfo;

/**
 * Interface for Genes represented by a vector of (double precision) floating-point values.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGene extends Gene {
	/**
	 * Information about each element of the vector representing this Gene.
	 */
	public final VectorInfo info;
	
	private final double[] values;
	
	/**
	 * Create a VectorGene with values initialised to 0.
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 * @param info The information about each element of the vector representing this gene.
	 */
	public VectorGene(Object type, VectorInfo info) {
		super(type);
		this.info = info;
		this.values = new double[info.size()];
	}
	
	/**
	 * Create a VectorGene with values initialised to those specified.
	 * 
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 * @param info The information about each element of the vector representing this gene.
	 * @param values The values of the gene.
	 */
	public VectorGene(Object type, VectorInfo info, double[] values) {
		super(type);
		if (info.size() != values.length) {
			throw new IllegalArgumentException("The size of the VectorInfo and the provided values must match.");
		}
		
		this.info = info;
		this.values = new double[values.length];
		System.arraycopy(values, 0, this.values, 0, values.length);
	}
	
	/**
	 * Get the value at the specified index.
	 */
	public double getValue(int index) {
		return values[index];
	}
	
	/**
	 * Get a List view of the values (the values as an unmodifiable list backed by the underlying primitive array).
	 */
	public List<Double> getValues() {
		return Collections.unmodifiableList(Doubles.asList(values));
	}
}

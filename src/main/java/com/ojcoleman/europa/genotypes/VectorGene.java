package com.ojcoleman.europa.genotypes;

import java.util.Collections;
import java.util.List;

import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Gene;

/**
 * Interface for Genes represented by a vector of (double precision) floating-point values.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGene extends Gene {
	private final VectorInfo info;
	private final double[] values;
	
	public VectorGene(VectorInfo info, double[] values) {
		this.info = info;
		this.values = values;
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

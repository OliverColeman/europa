package com.ojcoleman.europa.genotypes;

import java.util.Collections;
import java.util.List;

import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Allele;

/**
 * Interface for Alleles represented by a vector of (double precision) floating-point values.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorAllele<G extends VectorGene> extends Allele<G> {
	private final VectorInfo info;
	private final double[] values;
	
	public VectorAllele(G gene, VectorInfo info, double[] values) {
		super(gene);
		this.values = values;
		this.info = info;
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

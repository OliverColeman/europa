package com.ojcoleman.europa.genotypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.util.VectorInfo;

/**
 * Interface for Alleles represented by a vector of (double precision) floating-point values.
 * 
 * @author O. J. Coleman
 */
public class VectorAllele<G extends VectorGene> extends Allele<G> {
	/**
	 * Information about each element of the vector representing this Allele.
	 */
	public final VectorInfo info;
	
	private final double[] values;
	
	/**
	 * @see Allele#Allele(Allele)
	 */
	public VectorAllele(VectorAllele<G> allele) {
		super(allele.gene);
		info = allele.info;
		values = new double[allele.values.length];
		System.arraycopy(allele.values, 0, this.values, 0, values.length);
	}
	
	/**
	 * Create a VectorAllele with values initialised to 0.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 */
	public VectorAllele(G gene, VectorInfo info) {
		super(gene);
		this.info = info;
		this.values = new double[info.size()];
	}
	
	/**
	 * Create a VectorAllele with values initialised to those specified.
	 * 
	 * @param gene The gene this allele is for.
	 * @param info The information about each element of the vector representing this allele.
	 * @param values The initial values of the allele.
	 */
	public VectorAllele(G gene, VectorInfo info, double[] values) {
		super(gene);
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
	
	/**
	 * Get the values from this allele and its underlying gene as a map from the vector element labels to their values.
	 */
	public Map<String, Double> getAllValuesAsMap() {
		return getAllValuesAsMap(new HashMap<String, Double>());
	}
	
	/**
	 * Get the values from this allele and its underlying gene as a map from the vector element labels to their values.
	 */
	public Map<String, Double> getAllValuesAsMap(Map<String, Double> map) {
		for (int i = 0; i < gene.info.size(); i++) {
			map.put(info.label(i), gene.getValue(i));
		}
		for (int i = 0; i < info.size(); i++) {
			map.put(info.label(i), values[i]);
		}
		return map;
	}
}

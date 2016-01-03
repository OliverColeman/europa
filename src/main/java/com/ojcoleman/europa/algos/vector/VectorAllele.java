package com.ojcoleman.europa.algos.vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Allele;

/**
 * Interface for Alleles represented by a vector of (double precision) floating-point values.
 * 
 * @author O. J. Coleman
 */
public class VectorAllele<G extends VectorGene> extends Allele<G> {
	/**
	 * The vector stored by this allele. The values are {@link Vector#mutable}.
	 */
	public final Vector vector;
	
	
	/**
	 * @see Allele#Allele(Allele)
	 */
	public VectorAllele(VectorAllele<G> allele) {
		super(allele.gene);
		vector = new Vector(allele.vector);
	}
	
	/**
	 * Create a VectorAllele with values initialised to 0.
	 * 
	 * @param gene The gene this allele is for.
	 * @param paramVector The vector for this allele.
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public VectorAllele(G gene, Vector paramVector) {
		super(gene);
		if (!paramVector.mutable) {
			throw new IllegalArgumentException("The Vector for a VectorAllele must be set as mutable.");
		}
		this.vector = paramVector;
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
		for (int i = 0; i < gene.vector.metadata.size(); i++) {
			map.put(vector.metadata.label(i), gene.vector.get(i));
		}
		for (int i = 0; i < vector.metadata.size(); i++) {
			map.put(vector.metadata.label(i), vector.get(i));
		}
		return map;
	}
}

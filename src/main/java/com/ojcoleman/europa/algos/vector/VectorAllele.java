package com.ojcoleman.europa.algos.vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.Gene;

/**
 * <p>
 * Class for Alleles represented by a vector of (double precision) floating-point values.
 * </p>
 * <p>
 * <strong>Sub-classes must implement a copy-constructor that accepts a single parameter which is the allele to copy,
 * and which should generally just call <em>super()</em> with the allele to copy.</strong> See
 * {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
 * <p>
 * 
 * @see Vector
 * 
 * @author O. J. Coleman
 */
public class VectorAllele<G extends VectorGene> extends Allele<G> {
	/**
	 * The vector stored by this allele. The values are {@link Vector#mutable}.
	 */
	public final Vector vector;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public VectorAllele(Configuration config) {
		super(config);
		vector = Vector.EMPTY;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a new
	 * VectorAllele referencing the same underlying Gene but storing an independent copy of the Vector in the original allele.
	 * 
	 * @param paramVector The vector for the new allele, copied by reference.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public VectorAllele(VectorAllele<G> prototype) {
		super(prototype);
		this.vector = prototype.vector.copy();
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a new
	 * VectorAllele with the specified underlying Gene and storing the specified Vector.
	 * 
	 * @param allele The allele to copy.
	 * @param gene the underlying gene for the new allele.
	 * @param paramVector The vector for the new allele, copied by reference.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public VectorAllele(VectorAllele<G> prototype, G gene, Vector paramVector) {
		super(prototype, gene);
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

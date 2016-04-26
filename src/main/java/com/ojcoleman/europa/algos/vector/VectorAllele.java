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
import com.ojcoleman.europa.util.Interval;
import com.ojcoleman.europa.util.IntervalDouble;

/**
 * <p>
 * Class for Alleles represented by a vector of (double precision) floating-point values.
 * </p>
 * <p>
 * <strong>Sub-classes must implement a copy-constructor that accepts a single parameter which is the allele to copy,
 * and which should generally just call <em>super()</em> with the allele to copy.</strong> See
 * {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
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
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(Configuration)}.
	 */
	public VectorAllele(Configuration config) {
		super(config);
		vector = Vector.EMPTY;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}. Create a
	 * new VectorAllele referencing the same underlying Gene but storing an independent copy of the Vector in the
	 * original allele.
	 * 
	 * @param prototype The prototype VectorAllele to copy.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public VectorAllele(VectorAllele<G> prototype) {
		super(prototype);
		this.vector = prototype.vector.copy();
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}. Create a
	 * new VectorAllele with the specified underlying Gene and storing the specified Vector.
	 * 
	 * @param prototype The allele to copy.
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
	 * 
	 * @param map The map to put the parameters in. This is emptied before putting the parameters in.
	 */
	public Map<String, Double> getAllValuesAsMap(Map<String, Double> map) {
		map.clear();
		for (int i = 0; i < gene.vector.metadata.size(); i++) {
			map.put(gene.vector.metadata.label(i), gene.vector.get(i));
		}
		for (int i = 0; i < vector.metadata.size(); i++) {
			map.put(vector.metadata.label(i), vector.get(i));
		}
		return map;
	}

	/**
	 * Returns the sum of the absolute differences between corresponding values in this and the given VectorAllele,
	 * ignoring the "typeReference" parameter if present.
	 * 
	 * @param other The VectorAllele to get the difference from.
	 * @param normalise Whether the difference for each element should be normalised to a unit range, and the sum of the
	 *            differences should be normalised to a unit range.
	 * 
	 * @throws IllegalArgumentException If this and the given VectorAllele have different Vector metadata.
	 */
	public double difference(VectorAllele<?> other, boolean normalise) {
		if (!vector.metadata.equals(other.vector.metadata)) {
			throw new IllegalArgumentException("Can't compute difference of Vectors with different metadata.");
		}

		double diff = 0;
		if (!normalise) {
			for (int i = 0; i < vector.metadata.size(); i++) {
				if (!vector.metadata.label(i).equals("typeReference")) {
					diff += Math.abs(vector.get(i) - other.vector.get(i));
				}
			}
		} else {
			for (int i = 0; i < vector.metadata.size(); i++) {
				if (!vector.metadata.label(i).equals("typeReference")) {
					Interval<?> bounds = (IntervalDouble) vector.metadata.bound(i);
					diff += Math.abs(bounds.translateToUnit(vector.get(i)) - bounds.translateToUnit(other.vector.get(i)));
				}
			}
			diff /= vector.size();
		}
		return diff;
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		super.getStringableMap(map);
		map.put("vector", vector);
	}
}

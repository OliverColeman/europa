package com.ojcoleman.europa.algos.vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Gene;

/**
 * Interface for Genes represented by a vector of (double precision) floating-point values.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGene extends Gene {
	/**
	 * The vector stored by this gene. The values are not {@link Vector#mutable}.
	 */
	public final Vector vector;
	
	/**
	 * Create a VectorGene with values initialised to those specified.
	 * 
	 * @param type The type(s) of the gene, if applicable to the evolutionary algorithm in use. Usually these are enum constants. May be empty.
	 * @param vector The vector for this gene. Copied by reference.
	 * @throws IllegalArgumentException if the Vector is set as mutable.
	 */
	public VectorGene(Set<Object> type, Vector vector) {
		super(type);

		if (!vector.mutable) {
			throw new IllegalArgumentException("The Vector for a VectorGene must not be set as mutable.");
		}
		this.vector = vector;
	}
	
	/**
	 * Two VectorGene objects are considered equal if they have the same VectorMetadata specifications and the same values.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof VectorGene) {
			VectorGene otherVG = (VectorGene) other;
			return vector.equals(otherVG.vector);
		}
		return false;
	}
	
	/**
	 * Hash code based on the values of the vector values stored by this gene.
	 */
	@Override
	public int hashCode() {
		return vector.hashCode();
	}
}

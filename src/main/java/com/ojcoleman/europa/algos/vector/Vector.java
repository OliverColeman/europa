package com.ojcoleman.europa.algos.vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.eclipsesource.json.JsonObject;
import com.google.common.primitives.Doubles;

/**
 * A Vector stores
 * 
 * @author O. J. Coleman
 */
public class Vector {
	/**
	 * Metadata for an empty (zero length/size) vector.
	 */ 
	public final static Vector EMPTY = new Vector(VectorMetadata.EMPTY);
	
	
	/**
	 * Information about each element of the vector.
	 */
	public final VectorMetadata metadata;

	/**
	 * Whether the values in this Vector may be modified (via {@link #set(int, double)}.
	 */
	public final boolean mutable;

	private final double[] values;

	/**
	 * Copy constructor. The values of the new Vector are independent from the given vector but the metadata is copied
	 * by reference.
	 */
	public Vector(Vector vector) {
		this.metadata = vector.metadata;
		this.values = Arrays.copyOf(vector.values, vector.values.length);
		this.mutable = vector.mutable;
	}

	/**
	 * Create a Vector with the specified meta data and values.
	 * 
	 * @param metadata The information about each element of the vector.
	 * @param values The values of the vector. These will be copied into a new array.
	 * @param mutable Whether the values may be modified.
	 * 
	 * @throws IllegalArgumentException If the meta data does not contain the same number of elements as that for the
	 *             values.
	 */
	public Vector(VectorMetadata info, double[] values, boolean mutable) {
		if (info.size() != values.length) {
			throw new IllegalArgumentException("The meta data for a Vector must contain the same number of elements as that for the values given.");
		}
		this.metadata = info;
		this.values = Arrays.copyOf(values, values.length);
		this.mutable = mutable;
	}

	/**
	 * Create a Vector with the specified meta data and values initialised to zero. The Vector will be {@link #mutable}.
	 * 
	 * @param metadata The information about each element of the vector.
	 */
	public Vector(VectorMetadata info) {
		this.metadata = info;
		this.values = new double[info.size()];
		this.mutable = true;
	}

	/**
	 * Create a Vector with the specified meta data and with random values distributed uniformly between their minimum
	 * and maximum values (inclusive).
	 * 
	 * @param metadata The information about each element of the vector.
	 * @param mutable Whether the values may be modified.
	 * @param random The RNG to use to generate the values.
	 */
	public Vector(VectorMetadata info, boolean mutable, Random random) {
		this.metadata = info;
		this.mutable = mutable;
		this.values = new double[info.size()];
		for (int i = 0; i < size(); i++) {
			values[i] = metadata.bound(i).random(random).doubleValue();
		}
	}

	/**
	 * Get the length of this vector.
	 */
	public int size() {
		return metadata.size();
	}

	/**
	 * Get the value at the specified index.
	 */
	public double get(int index) {
		return values[index];
	}

	/**
	 * Set the value at the specified index. If the value at index should be an integer then the given value will be
	 * rounded.
	 * 
	 * @throws UnsupportedOperationException if the values are not {@link #mutable}.
	 */
	public void set(int index, double value) {
		if (!mutable) {
			throw new UnsupportedOperationException("The values of this Vector may not be modified.");
		}
		values[index] = metadata.isInteger(index) ? Math.round(value) : value;
	}

	/**
	 * Get a List view of the values (the values as an unmodifiable list backed by the underlying primitive array).
	 */
	public List<Double> getValues() {
		return Collections.unmodifiableList(Doubles.asList(values));
	}

	/**
	 * Two Vectors are considered equal if they have the same VectorMetadata specifications and the same values.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Vector) {
			Vector otherV = (Vector) other;
			return metadata.equals(otherV.metadata) && Arrays.equals(values, otherV.values);
		}
		return false;
	}

	/**
	 * Hash code based on the values.
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
}

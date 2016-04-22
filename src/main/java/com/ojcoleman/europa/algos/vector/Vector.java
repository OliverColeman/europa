package com.ojcoleman.europa.algos.vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.eclipsesource.json.JsonObject;
import com.google.common.primitives.Doubles;
import com.ojcoleman.europa.core.Stringable;
import com.ojcoleman.europa.util.Interval;
import com.ojcoleman.europa.util.IntervalDouble;
import com.sun.swing.internal.plaf.metal.resources.metal;

/**
 * A Vector stores an array of double-precision floating point values. The meta-data for each element are defined by a
 * {@link VectorMetadata} (which may be shared among many Vectors). Although the values are stored as doubles, the
 * meta-data specifies whether an element is intended to represent a floating-point or integer value. The magnitude of
 * the maximum value that may be stored is 2^53, see {@link #maximumIntegerValue}. Attempts to store values larger than
 * this in elements intended to store integer values results in an IllegalArgumentException.
 * 
 * @author O. J. Coleman
 */
public class Vector implements Stringable {
	/**
	 * The largest magnitude (positive or negative) integer that may be stored in a Vector. This is determined by the
	 * largest integer value for which it and all smaller integer values can be accurately represented by a (IEEE 754)
	 * double. The value is 2^53 (between an int (2^31) and long (2^63)).
	 */
	public final double maximumIntegerValue = 1L << 53;

	/**
	 * An empty (zero length/size) vector.
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
	 *             values or if a value in the given array for an element intended to store an integer has magnitude
	 *             greater than {@link #maximumIntegerValue}.
	 * @throws UnsupportedOperationException if the values in this vector are not {@link #mutable}.
	 * 
	 */
	public Vector(VectorMetadata info, double[] values, boolean mutable) {
		if (info.size() != values.length) {
			throw new IllegalArgumentException("The meta data for a Vector must contain the same number of elements as that for the values given.");
		}
		this.metadata = info;
		this.mutable = mutable;
		this.values = new double[values.length];
		setValuesIgnoreMutable(values);
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
	 * Get the value at the specified index. If the element at the specified index is intended to hold an integer value
	 * then it may be safely cast to a long without losing precision.
	 */
	public double get(int index) {
		return values[index];
	}

	/**
	 * Get the value at the specified index cast as an int.
	 */
	public int getInt(int index) {
		return (int) values[index];
	}

	/**
	 * Get the value at the specified index cast as a long.
	 */
	public long getLong(int index) {
		return (long) values[index];
	}

	/**
	 * Set the value at the specified index. If the element at the given index is intended to store an integer then the
	 * given value will be rounded. If the value is outside of the bounds specified by the {@link #metadata} then it 
	 * will be clamped to the lower or upper bound as necessary.
	 * 
	 * @throws UnsupportedOperationException if the values are not {@link #mutable} or if the value at the specified
	 *             index is intended to store an integer and a value with magnitude greater than
	 *             {@link #maximumIntegerValue} was given.
	 */
	public void set(int index, double value) {
		if (!mutable) {
			throw new UnsupportedOperationException("The values of this Vector may not be modified.");
		}
		value = metadata.bound(index).clamp(value);
		setIgnoreMutable(index, value);
	}
	
	private void setIgnoreMutable(int index, double value) {
		if (metadata.isInteger(index)) {
			if (value > maximumIntegerValue || value < -maximumIntegerValue) {
				throw new UnsupportedOperationException("The value at the specified index, " + index + ", is intended to hold an integer value, the largest integer that may be stored is (-)2^53 but a value larger than this was given.");
			}
			values[index] = Math.round(value);
		} else {
			values[index] = value;
		}
	}

	/**
	 * Copy the values from the given vector to this vector. For any elements intended to store integers, the
	 * corresponding values from the given vector will be rounded if necessary.
	 * 
	 * @param vector the vector whose values should be copied.
	 * 
	 * @throws UnsupportedOperationException if the values in this vector are not {@link #mutable}.
	 * @throws IllegalArgumentException If the given vector does not contain the same number of elements as this vector
	 *             or if a value in the given vector for an element intended to store an integer has magnitude greater
	 *             than {@link #maximumIntegerValue}.
	 */
	public void setValues(Vector vector) {
		// If this and the given vector share the same metadata object then we don't need to perform any checks.
		if (metadata == vector.metadata) {
			System.arraycopy(vector.values, 0, values, 0, values.length);
			return;
		}

		if (vector.values.length != values.length) {
			throw new IllegalArgumentException("The source and destination Vectors must be of equal length.");
		}

		for (int i = 0; i < values.length; i++) {
			set(i, vector.values[i]);
		}
	}

	/**
	 * Copy the values from the given array to this vector. For any elements intended to store integers, the
	 * corresponding values from the given array will be rounded if necessary.
	 * 
	 * @param newValues the array whose values should be copied.
	 * 
	 * @throws UnsupportedOperationException if the values in this vector are not {@link #mutable}.
	 * @throws IllegalArgumentException If the given array does not contain the same number of elements as this vector
	 *             or if a value in the given array for an element intended to store an integer has magnitude greater
	 *             than {@link #maximumIntegerValue}.
	 */
	public void setValues(double[] newValues) {
		if (newValues.length != values.length) {
			throw new IllegalArgumentException("The source values array and the Vector must be of equal length.");
		}

		for (int i = 0; i < values.length; i++) {
			set(i, newValues[i]);
		}
	}
	
	private void setValuesIgnoreMutable(double[] newValues) {
		if (newValues.length != values.length) {
			throw new IllegalArgumentException("The source values array and the Vector must be of equal length.");
		}

		for (int i = 0; i < values.length; i++) {
			setIgnoreMutable(i, newValues[i]);
		}
	}

	/**
	 * Get a List view of the values (the values as an unmodifiable list backed by the underlying primitive array).
	 */
	public List<Double> getValues() {
		return Collections.unmodifiableList(Doubles.asList(values));
	}


	/**
	 * Get a reference to the underlying values array. Be careful. Do not modify the values if {@link #mutable} is not set.
	 */
	public double[] getValuesReference() {
		return values;
	}

	/**
	 * Creates and returns a copy of this Vector, the values in the new Vector are independent of the values in this
	 * vector but the metadata (which is immutable) is copied by reference.
	 */
	public Vector copy() {
		return new Vector(this);
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

	/**
	 * Returns a raw sum over all the values in this vector.
	 */
	public double sum() {
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		return sum;
	}

	/**
	 * Returns a new Vector whose values are the averages of the corresponding values over the given Vectors.
	 * 
	 * @param vectors the Vectors to average over.
	 * 
	 * @throws IllegalArgumentException If the list of vectors is empty.
	 * @throws IllegalArgumentException If any of the given Vectors have different metadata.
	 */
	public static Vector calculateAverage(Collection<Vector> vectors) {
		if (vectors.isEmpty()) {
			throw new IllegalArgumentException("Can not average over empty list of Vectors.");
		}
		Vector avg = null;
		for (Vector v : vectors) {
			if (avg == null) {
				avg = new Vector(v.metadata);
			}
			else if (!avg.metadata.equals(v.metadata)) {
				throw new IllegalArgumentException("Can not average over Vectors with different metadata.");
			}
			
			for (int i = 0; i < avg.size(); i++) {
				avg.values[i] += v.values[i];
			}
		}
		for (int i = 0; i < avg.size(); i++) {
			avg.values[i] /= vectors.size();
		}
		return avg;
	}
	

	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("metadata", metadata);
		map.put("mutable", mutable);
		for (int i = 0; i < values.length; i++) {
			map.put(metadata.label(i), "(" + (metadata.isInteger(i) ? "int" : "float") + " " + metadata.bound(i) + ")  " + values[i]);
		}
	}
}

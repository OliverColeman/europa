package com.ojcoleman.europa.util;

import java.util.Random;

import com.ojcoleman.europa.algos.vector.VectorMetadata.Element;

/**
 * Represents a numeric interval from a minimum to maximum value, [start, end].
 * 
 * @author O. J. Coleman
 */
public abstract class Interval<T extends Number & Comparable<T>> {
	/**
	 * The start of the interval, inclusive.
	 */
	public final T start;

	/**
	 * The end of the interval, inclusive.
	 */
	public final T end;

	public Interval(T s, T e) {
		if (e.compareTo(s) < 0) {
			throw new IllegalArgumentException("The start of an Interval must be less than or equal to the end of the Interval, values given were: [" + s + ", " + e + "].");
		}
		start = s;
		end = e;
	}

	/**
	 * Returns the range of this Interval: <em>end - start</em>.
	 */
	public abstract T range();

	/**
	 * Determine if the given value lies within this interval.
	 * 
	 * @param value The value to check.
	 * @return true iff the given value is in this interval.
	 */
	public abstract boolean isIn(T value);

	/**
	 * Returns a string representation of this Interval: "[start, end]".
	 */
	public String toString() {
		return "[" + start + ", " + end + "]";
	}

	/**
	 * If the given value is less than the start of this interval then returns the start value. If the given value is
	 * greater than the end of this interval then returns the end value. Otherwise returns the given value.
	 */
	public abstract T clamp(T value);

	/**
	 * Translate a value from the unit interval [0, 1] to the corresponding value from this interval.
	 */
	public abstract double translateFromUnit(double p);

	/**
	 * Translate a value from this interval to the corresponding value from the unit interval [0, 1].
	 */
	public abstract double translateToUnit(double p);

	/**
	 * Get a random uniformly distributed value in the range [start, end] (inclusive).
	 */
	public abstract T random(Random random);

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof Interval) {
			Interval<?> other = (Interval<?>) o;
			// Although Number itself does not implement equals, all concrete sub-classes do.
			return start.equals(other.start) && end.equals(other.end);
		}
		return false;
	}
}

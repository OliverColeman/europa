package com.ojcoleman.europa.util;

import java.util.Random;

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
		if (e.compareTo(s) <= 0) {
			throw new IllegalArgumentException("The start of an Interval must be less than the end of the Interval, values given were: [" + s + ", " + e + "].");
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
	 * @param v The value to check.
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
	 * If the given value is less than the start of this interval then returns the start value.
	 * If the given value is greater than the end of this interval then returns the end value.
	 * Otherwise returns the given value.
	 */
	public abstract T clamp(T value);
	
	/**
	 * Get a random uniformly distributed value in the range [start, end] (inclusive).
	 */
	public abstract T random(Random random);
}
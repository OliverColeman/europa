package com.ojcoleman.europa.util;

/**
 * Represents a number range from a minimum to maximum value, [start, end].
 * 
 * @author O. J. Coleman
 */
public abstract class Range<T extends Number & Comparable<T>> {
	/**
	 * The start of the range, inclusive.
	 */
	public final T start;
	
	/**
	 * The end of the range, inclusive.
	 */
	public final T end;
	
	public Range(T s, T e) {
		if (e.compareTo(s) <= 0) {
			throw new IllegalArgumentException("The start of a RangeT must be less than the end of the RangeT, values given were: [" + s + ", " + e + "].");
		}
		start = s;
		end = e;
	}
	
	/**
	 * Returns the extend of this Range: <em>end - start</em>.
	 */
	public abstract T extent();
	
	/**
	 * Ensure the given value is in the specified range.
	 * 
	 * @param v The value to check.
	 * @param min The start of the range. 
	 * @param max The end of the range.
	 * @return true iff the given value is in the range [min, max].
	 */
	public abstract boolean checkRange(T value);
	
	/**
	 * Returns a string representation of this Range: "[start, end]".
	 */
	public String toString() {
		return "[" + start + ", " + end + "]";
	}

	/**
	 * Returns the given value clamped within the interval defined by this RangeT.
	 */
	public abstract T clamp(T value);
}

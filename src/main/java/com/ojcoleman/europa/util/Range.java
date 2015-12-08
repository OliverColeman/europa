package com.ojcoleman.europa.util;

/**
 * Represents a number range from a minimum to maximum value, [min, max].
 * 
 * @author O. J. Coleman
 */
public class Range {
	/**
	 * The unit range [0, 1].
	 */
	public final static Range UNIT = new Range(0, 1);
	
	/**
	 * The start of the range, inclusive.
	 */
	public final double start;
	
	/**
	 * The end of the range, inclusive.
	 */
	public final double end;
	
	/**
	 * The extent of the range (end - start).
	 */
	public final double range;

	public Range(double s, double e) {
		if (e <= s)
			throw new IllegalArgumentException("The start of a Range must be less than the end of the Range, values given were: [" + s + ", " + e + "].");
		start = s;
		end = e;
		range = e - s;
	}

	/**
	 * Translate a value from the unit range [0, 1] to the corresponding value from this range.
	 */
	public double translateFromUnit(double p) {
		return start + p * range;
	}

	/**
	 * Translate a value from this range to the corresponding value from the unit range [0, 1].
	 */
	public double translateToUnit(double p) {
		return (p - start) / range;
	}
	
	/**
	 * Ensure the given value is in the specified range.
	 * 
	 * @param v The value to check.
	 * @param label If not null and the value given is not within the range [min, max] then an exception thrown using the
	 *            given label as identifier.
	 * @param min The start of the range. 
	 * @param max The end of the range.
	 * @return true iff the given value is in the range [min, max], otherwise false if label == null, otherwise an
	 *         IllegalArgumentException is thrown indicating that the value identified by label is not in the correct
	 *         range.
	 */
	public static boolean checkRange(double v, String label, double min, double max) {
		return checkRange(v, label, min, max, true, true);
	}
	
	/**
	 * Ensure the given value is in the specified range, with either the minimum or maximum value being exclusive.
	 * 
	 * @param v The value to check.
	 * @param label If not null and the value given is not within the range then an exception thrown using the
	 *            given label as identifier.
	 * @param min The start of the range. 
	 * @param max The end of the range.
	 * @param minInclusive Whether the start of the range (min) should be inclusive. 
	 * @param maxInclusive Whether the end of the range (max) should be inclusive.
	 * @return true iff the given value is in the range [0, 1], otherwise false if label == null, otherwise an
	 *         IllegalArgumentException is thrown indicating that the value identified by label is not in the correct
	 *         range.
	 */
	public static boolean checkRange(double v, String label, double min, double max, boolean minInclusive, boolean maxInclusive) {
		if ((v > min || minInclusive && v == min) && (v < max || maxInclusive && v == max))
			return true;
		if (label == null)
			return false;
		throw new IllegalArgumentException(label + " must be in the range " + (minInclusive ? "[" : "(") + min + ", " + max + (maxInclusive ? "]" : ")") + " but " + v + " was given.");
	}
	
	public String toString() {
		return "[" + start + ", " + end + "]";
	}

	/**
	 * Returns the given value clamped within the interval defined by this Range.
	 */
	public double clamp(double v) {
		if (v < start) return start;
		if (v > end) return end;
		return v;
	}
}

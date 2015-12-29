package com.ojcoleman.europa.util;

/**
 * Represents a floating-point number range from a minimum to maximum value, [start, end].
 * 
 * @author O. J. Coleman
 */
public class RangeDouble extends Range<Double> {
	private final double extent;
	
	/**
	 * The unit range [0, 1].
	 */
	public final static RangeDouble UNIT = new RangeDouble(0, 1);

	public RangeDouble(double start, double end) {
		super(start, end);
		extent = end-start;
	}

	/**
	 * Translate a value from the unit range [0, 1] to the corresponding value from this range.
	 */
	public double translateFromUnit(double p) {
		return start + p * extent();
	}

	/**
	 * Translate a value from this range to the corresponding value from the unit range [0, 1].
	 */
	public double translateToUnit(double p) {
		return (p - start) / extent();
	}
	/**
	 * Returns the given value clamped within the interval defined by this RangeDouble.
	 */
	@Override
	public Double clamp(Double v) {
		if (v < start) return start;
		if (v > end) return end;
		return v;
	}

	@Override
	public boolean checkRange(Double value) {
		return value >= start && value <= end;
	}


	@Override
	public Double extent() {
		return extent;
	}
}

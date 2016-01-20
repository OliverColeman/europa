package com.ojcoleman.europa.util;

import java.util.Random;

/**
 * Represents a floating-point numeric interval from a minimum to maximum value, [start, end].
 * 
 * @author O. J. Coleman
 */
public class IntervalDouble extends Interval<Double> {
	private final double range;

	/**
	 * The unit interval [0, 1].
	 */
	public final static IntervalDouble UNIT = new IntervalDouble(0, 1);

	public IntervalDouble(double start, double end) {
		super(start, end);
		range = end - start;
	}

	/**
	 * Translate a value from the unit interval [0, 1] to the corresponding value from this interval.
	 */
	public double translateFromUnit(double p) {
		return start + p * range();
	}

	/**
	 * Translate a value from this interval to the corresponding value from the unit interval [0, 1].
	 */
	public double translateToUnit(double p) {
		return (p - start) / range();
	}

	@Override
	public Double clamp(Double v) {
		if (v < start)
			return start;
		if (v > end)
			return end;
		return v;
	}

	@Override
	public boolean isIn(Double value) {
		return value >= start && value <= end;
	}

	@Override
	public Double range() {
		return range;
	}

	@Override
	public Double random(Random random) {
		return translateFromUnit(random.nextDouble());
	}
}

package com.ojcoleman.europa.util;

import java.util.Random;

/**
 * Represents an integer numeric interval from a minimum to maximum value, [start, end].
 * 
 * @author O. J. Coleman
 */
public class IntervalLong extends Interval<Long> {
	public final long range;

	public IntervalLong(long start, long end) {
		super(start, end);
		range = end - start;
	}

	@Override
	public Long clamp(Long v) {
		if (v < start)
			return start;
		if (v > end)
			return end;
		return v;
	}

	@Override
	public Long range() {
		return range;
	}

	@Override
	public boolean isIn(Long value) {
		return value >= start && value <= end;
	}

	@Override
	public Long random(Random random) {
		// for small n use nextInt and cast
		if ((range + 1) <= Integer.MAX_VALUE) {
			return (long) random.nextInt((int) (range + 1)) + start;
		}

		// for large n use nextInt for both high and low ints
		int highLimit = (int) ((range + 1) >> 32);
		long high = (long) random.nextInt(highLimit) << 32;
		long low = ((long) random.nextInt()) & 0xffffffffL;
		return (high | low) + start;
	}
}

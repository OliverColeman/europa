package com.ojcoleman.europa.util;

/**
 * Represents an integer number range from a minimum to maximum value, [start, end].
 * 
 * @author O. J. Coleman
 */
public class RangeLong extends Range<Long> {
	public final long extent;

	public RangeLong(long start, long end) {
		super(start, end);
		extent = end - start;
	}

	@Override
	public Long clamp(Long v) {
		if (v < start) return start;
		if (v > end) return end;
		return v;
	}

	@Override
	public Long extent() {
		return extent;
	}

	@Override
	public boolean checkRange(Long value) {
		return value >= start && value <= end;
	}
}

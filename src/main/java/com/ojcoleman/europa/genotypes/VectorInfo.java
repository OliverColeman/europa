package com.ojcoleman.europa.genotypes;

import com.ojcoleman.europa.util.Range;

/**
 * Class for containing meta-data about each value in a vector, for example within a {link VectorAllele} or {@link VectorGene}.
 * 
 * @author O. J. Coleman
 */
public class VectorInfo {
	private final Range[] bounds;
	private final boolean[] isInteger;
	
	/**
	 * @param bounds The minimum and maximum (inclusive) values for each value in the vector.
	 * @param isInteger Whether a value in the vector should only take on integer values.
	 * @throws IllegalArgumentException If the lengths of the bounds and isInteger arrays are not equal.
	 */
	public VectorInfo(Range[] bounds, boolean[] isInteger) {
		if (bounds.length != isInteger.length) {
			throw new IllegalArgumentException("The lengths of the bounds and isInteger arrays must be equal.");
		}
		
		this.bounds = bounds;
		this.isInteger = isInteger;
	}
	
	public Range bound(int index) {
		return bounds[index];
	}
	
	public boolean isInteger(int index) {
		return isInteger[index];
	}
}

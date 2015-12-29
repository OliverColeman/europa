package com.ojcoleman.europa.functiontypes;

import com.ojcoleman.europa.core.Function;

/**
 * Interface for functions that take a double vector as input and output a double vector.
 * 
 * @author O. J. Coleman
 */
public interface VectorFunction extends Function<double[], double[]> {
	@Override
	public double[] apply(double[] input);
	
	@Override
	public double[][] apply(double[][] input);
}

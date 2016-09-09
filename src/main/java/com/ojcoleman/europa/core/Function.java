package com.ojcoleman.europa.core;

/**
 * Interface for any object that takes input and produces output. Note that a Function may have internal state that is
 * modified by the process of generating output for given input. The input, I, may be of any type and the output, O, may
 * be of any type.
 * 
 * @author O. J. Coleman
 */
public interface Function<I, O> {
	/**
	 * Apply this function to the given input and return the output.
	 */
	public O apply(I input);

	/**
	 * Apply this function to the given sequence of inputs and return the corresponding outputs.
	 */
	public O[] apply(I[] input);

	/**
	 * Apply this function to the given input and return the output, optionally reusing the supplied output object.
	 */
	public O apply(I input, O output);

	/**
	 * Apply this function to the given sequence of inputs and return the corresponding outputs, optionally reusing the
	 * supplied output objects.
	 */
	public O[] apply(I[] input, O[] output);
	
	/**
	 * Reset internal state to initial values, if applicable.
	 */
	public void reset();
}

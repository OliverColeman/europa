package com.ojcoleman.europa.core;

import com.ojcoleman.europa.util.IntervalDouble;

/**
 * Defines the properties of an evaluation performed by an {@link Evaluator}.
 * 
 * @author O. J. Coleman
 */
public class EvaluationDescription {
	/**
	 * A name for this evaluation.
	 */
	public final String name;
	
	/**
	 * The {@link Evaluator} defining this evaluation.
	 */
	public final Evaluator evaluator;
	
	/**
	 * A IntervalDouble describing the minimum and maximum possible values for this evaluation.
	 */
	public final IntervalDouble range;
	
	/**
	 * The optimal value achievable for this evaluation (will usually be either the minimum or maximum value).
	 */
	public final double optimalValue;
	
	public EvaluationDescription(String name, Evaluator evaluator, IntervalDouble range, double optimalValue) {
		this.name = name;
		this.evaluator = evaluator;
		this.range = range;
		this.optimalValue = optimalValue;
	}
	
	@Override
	public int hashCode() {
		return evaluator.getName().hashCode();
	}
}

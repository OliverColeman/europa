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

	/**
	 * If true indicates that this evaluation is a "human-readable" performance metric and is not used to evaluate an
	 * individuals fitness.
	 */
	public final boolean isPerformanceIndicator;

	public EvaluationDescription(String name, Evaluator evaluator, IntervalDouble range, double optimalValue, boolean isPerformanceIndicator) {
		this.name = name;
		this.evaluator = evaluator;
		this.range = range;
		this.optimalValue = optimalValue;
		this.isPerformanceIndicator = isPerformanceIndicator;
	}

	@Override
	public int hashCode() {
		return evaluator.getName().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof EvaluationDescription) {
			EvaluationDescription ed = (EvaluationDescription) o;
			return this.name.equals(ed.name) && 
					this.evaluator == ed.evaluator && 
					this.range.equals(ed.range) && 
					this.optimalValue == ed.optimalValue && 
					this.isPerformanceIndicator == ed.isPerformanceIndicator;
		}
		return false;
	}
}

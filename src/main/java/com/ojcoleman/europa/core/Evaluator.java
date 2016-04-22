package com.ojcoleman.europa.core;

import java.util.Set;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;

/**
 * Base class for classes used to evaluate the fitness of {@link Individual}s in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public abstract class Evaluator extends ComponentBase {
	@Parameter(description = "A name for the evaluator, used in logging. Default is the evaluators class name.", optional = true)
	private String name;
	
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Evaluator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		if (name == null) {
			name = this.getClass().getSimpleName();
		}
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Get the {@link EvaluationDescription}s that describe the evaluations performed by this evaluator.
	 */
	public abstract Set<EvaluationDescription> getEvaluationDescriptions();

	/**
	 * <p>
	 * Evaluate the given {@link Individual}s. The Evaluator must call {@link #setResult(EvaluationDescription, double)}
	 * on individual.evaluationData for each evaluation type it defines in {@link #getEvaluationDescriptions()}.
	 * </p>
	 * <p>
	 * <strong>It is critical that this method is thread-safe (can handle multiple simultaneous calls).</strong>
	 * </p>
	 * 
	 * @param individual The Individual to evaluate.
	 * @param log Whether, how and what details to log of the evaluation. How the log is interpreted and
	 *            handled is up to the implementing class. If {@link Log#NO_LOG} is given then no logging is required.
	 */
	public abstract void evaluate(Individual individual, Log log);
	
	/**
	 * Allows the Evaluator to indicate if the evolutionary run should terminate, usually because a solution has been found.
	 * This default implementation always returns false.
	 */
	public boolean shouldTerminate() {
		return false;
	}
}

package com.ojcoleman.europa.core;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;

/**
 * Base class for classes used to evaluate the fitness of {@link Individual}s in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public abstract class Evaluator extends Component {
	@IsParameter(description = "A name for the evaluator, used in logging. Default is the evaluators class name.", optional = true)
	private String name;

	/**
	 * Constructor for {@link Component}.
	 */
	public Evaluator(Component parentComponent, JsonObject componentConfig) throws Exception {
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
	 */
	public abstract void evaluate(Individual individual);
}

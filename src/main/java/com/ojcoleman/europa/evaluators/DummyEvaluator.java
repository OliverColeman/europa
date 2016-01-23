package com.ojcoleman.europa.evaluators;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Run;

/**
 * Dummy evaluator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyEvaluator extends Evaluator {
	/**
	 * Constructor for {@link Component}.
	 */
	public DummyEvaluator(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public Set<EvaluationDescription> getEvaluationDescriptions() {
		return null;
	}

	@Override
	public void evaluate(Individual individual) {
	}

}

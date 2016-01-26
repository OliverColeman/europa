package com.ojcoleman.europa.core;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;

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

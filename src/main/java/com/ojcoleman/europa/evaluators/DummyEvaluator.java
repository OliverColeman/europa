package com.ojcoleman.europa.evaluators;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Individual;

/**
 * Dummy evaluator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyEvaluator extends Evaluator {
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public DummyEvaluator(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Set<EvaluationDescription> getEvaluationDescriptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void evaluate(Individual individual) {
		// TODO Auto-generated method stub

	}

}

package com.ojcoleman.europa.core;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;

/**
 * Dummy evaluator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyEvaluator extends Evaluator {
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public DummyEvaluator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public Set<EvaluationDescription> getEvaluationDescriptions() {
		return null;
	}

	@Override
	public void evaluate(Individual individual, Log log) {
	}

}

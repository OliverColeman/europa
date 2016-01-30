package com.ojcoleman.europa.algos.vector;

import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Individual;

/**
 * Base class for evaluators that evaluate {@link VectorFunction}s.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorFunctionEvaluator extends Evaluator {
	public VectorFunctionEvaluator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Primary {@link Evaluator} classes sould override this method to return the expected number of inputs to the
	 * functions to evaluate.
	 */
	public int getVectorFunctionInputSize() {
		return 0;
	}

	/**
	 * Primary {@link Evaluator} classes sould override this method to return the expected number of outputs for the
	 * functions to evaluate.
	 */
	public int getVectorFunctionOutputSize() {
		return 0;
	}
}

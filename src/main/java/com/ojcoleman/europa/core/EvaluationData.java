package com.ojcoleman.europa.core;

import java.util.Map;

import java.util.Collections;
import java.util.HashMap;

/**
 * Container for the result of evaluating an {@link Individual} with an {@link Evaluator}.
 * 
 * @author O. J. Coleman
 */ 
public class EvaluationData {
	private final Map<EvaluationDescription, Double> results;
	
	public EvaluationData() {
		results = new HashMap<EvaluationDescription, Double>();
	}
	
	public void setResult(EvaluationDescription key, double result) {
		if (results.containsKey(key)) {
			throw new IllegalArgumentException("EvaluationData: A result has already been set for " + key.name);
		}
		results.put(key, result);
	}

	/**
	 * Returns the result for the given key.
	 */
	public double getResult(EvaluationDescription key) {
		return results.get(key);
	}
	
	/**
	 * Returns an immutable view of the results.
	 */
	public Map<EvaluationDescription, Double> getResults() {
		return Collections.unmodifiableMap(results);
	}
}

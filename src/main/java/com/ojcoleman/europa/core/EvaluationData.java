package com.ojcoleman.europa.core;

import java.util.Map;
import java.util.Map.Entry;

import com.ojcoleman.europa.util.Stringer;

import java.util.Collections;
import java.util.HashMap;

/**
 * Container for the result of evaluating an {@link Individual} with an {@link Evaluator}.
 * 
 * @author O. J. Coleman
 */
public class EvaluationData {
	private final Map<EvaluationDescription, Double> fitnessResults;
	private final Map<EvaluationDescription, Double> performanceResults;
	private final Map<EvaluationDescription, Double> allResults;

	public EvaluationData() {
		fitnessResults = new HashMap<EvaluationDescription, Double>();
		performanceResults = new HashMap<EvaluationDescription, Double>();
		allResults = new HashMap<EvaluationDescription, Double>();
	}

	public void setResult(EvaluationDescription key, double result) {
		if (allResults.containsKey(key)) {
			throw new IllegalArgumentException("EvaluationData: A result has already been set for " + key.name);
		}
		allResults.put(key, result);
		if (key.isPerformanceIndicator) {
			performanceResults.put(key, result);
		} else {
			fitnessResults.put(key, result);
		}
	}

	/**
	 * Returns the result for the given key.
	 */
	public double getResult(EvaluationDescription key) {
		return allResults.get(key);
	}

	/**
	 * Returns an immutable view of all results.
	 */
	public Map<EvaluationDescription, Double> getResults() {
		return Collections.unmodifiableMap(allResults);
	}

	/**
	 * Returns an immutable view of all fitness results.
	 */
	public Map<EvaluationDescription, Double> getFitnessResults() {
		return Collections.unmodifiableMap(fitnessResults);
	}

	/**
	 * Returns an immutable view of all performance results.
	 */
	public Map<EvaluationDescription, Double> getPerformanceResults() {
		return Collections.unmodifiableMap(performanceResults);
	}

	/**
	 * Returns the number of fitness results recorded.
	 */
	public int getFitnessResultCount() {
		return fitnessResults.size();
	}

	/**
	 * Returns the number of performance results recorded.
	 */
	public int getPerformanceResultCount() {
		return performanceResults.size();
	}
	
	/**
	 * Returns true iff every fitness and performance objective defined in 
	 * both EvaluationData objects is present in the other and has the same value.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof EvaluationData) {
			EvaluationData ed = (EvaluationData) o;
			
			if (this.allResults.size() != ed.allResults.size()) {
				return false;
			}
			
			for (Entry<EvaluationDescription, Double> thisResults : allResults.entrySet()) {
				if (!ed.allResults.containsKey(thisResults.getKey())) {
					return false;
				}
				if (!thisResults.getValue().equals(ed.allResults.get(thisResults.getKey()))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Removes all evaluation data.
	 */
	public void clear() {
		allResults.clear();
		fitnessResults.clear();
		performanceResults.clear();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		boolean first = true;
		for (Map.Entry<EvaluationDescription, Double> res : allResults.entrySet()) {
			if (!first)
				s.append(", ");
			else
				first = false;
			s.append(res.getKey().name + ": " + Run.getDefaultNumberFormatStatic().format(res.getValue()));
		}
		return s.toString();
	}
}

package com.ojcoleman.europa.rankers;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Ranker;

/**
 * This is a simple ranker that only considers the primary fitness objective when ranking individuals.
 * 
 * @author O. J. Coleman
 */
public class DefaultRanker extends Ranker {
	/**
	 * Constructor for {@link Component}.
	 */
	public DefaultRanker(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void rank(Population<?, ?> population) {
		Set<EvaluationDescription> evDescs = population.getMembers().iterator().next().evaluationData.getFitnessResults().keySet();
		if (evDescs.size() != 1) {
			throw new IllegalArgumentException("The DefaultRanker can only be used when a single fitness evaluation is defined.");
		}
		// Get a reference to the one and only EvaluationDescription. 
		EvaluationDescription ed = evDescs.iterator().next();
		
		TreeMap<Double, Individual<?, ?>> fitnessIndividualMap = new TreeMap<>();
		for (Individual<?, ?> ind : population.getMembers()) {
			fitnessIndividualMap.put(ind.evaluationData.getFitnessResults().get(ed), ind);
		}
		int rank = 0;
		for (Individual<?, ?> ind : fitnessIndividualMap.values()) {
			ind.setRank(rank++);
		}
	}
}

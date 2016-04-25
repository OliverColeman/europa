package com.ojcoleman.europa.rankers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Ranker;
import com.ojcoleman.europa.core.Run;

/**
 * This is a simple ranker that only considers the primary fitness objective when ranking individuals.
 * 
 * @author O. J. Coleman
 */
public class SimpleRanker<G extends Genotype<?>, F extends Function<?, ?>> extends Ranker<G, F> {
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public SimpleRanker(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		this.getParentComponent(Run.class).monitor(this);
	}
	
	@Override
	public void rank(Population<G, F> population) {
		Set<EvaluationDescription> evDescs = population.getMembers().iterator().next().evaluationData.getFitnessResults().keySet();
		if (evDescs.size() != 1) {
			throw new IllegalArgumentException("The SimpleRanker can only be used when a single fitness evaluation is defined.");
		}
		// Get a reference to the one and only EvaluationDescription. 
		EvaluationDescription ed = evDescs.iterator().next();
		
		TreeMultimap<Double, Individual<?, ?>> fitnessIndividualMap = TreeMultimap.create();
		for (Individual<?, ?> ind : population.getMembers()) {
			double fitness = ind.evaluationData.getFitnessResults().get(ed);
			fitnessIndividualMap.put(fitness, ind);
		}
		
		int rank = 0;
		Individual<?, ?> fittest = null;
		for (Individual<?, ?> ind : fitnessIndividualMap.values()) {
			ind.setRank(rank++);
			fittest = ind;
		}
		
		this.fireEvent(Event.RankingFinished, fittest);
	}
	
	
	/**
	 * Ranker event types.
	 */
	public static enum Event {
		/**
		 * An event type indicating that ranking of the individuals has finished.
		 */
		RankingFinished,
	}
}

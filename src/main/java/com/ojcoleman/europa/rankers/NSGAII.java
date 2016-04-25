package com.ojcoleman.europa.rankers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Ranker;
import com.ojcoleman.europa.core.Stringable;
import com.ojcoleman.europa.util.Stringer;

/**
 * This class implements the non-dominated sorting selection method (according to rank and then crowding
 * comparison operator) based on the multi-objective genetic algorithm NSGA-II as described in DEB, Kalyanmoy ; PRATAP,
 * Amrit ; AGARWAL, Sameer A. ; MEYARIVAN, T.: "A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II". In: IEEE
 * Transactions on Evolutionary Computation, vol. 6, no. 2, April 2002, pp. 182-197.
 * 
 * This code is based on JNSGA2 by Joachim Melcher, Institut AIFB, Universitaet Karlsruhe (TH), Germany
 * http://sourceforge.net/projects/jnsga2
 */
public class NSGAII<G extends Genotype<?>, F extends Function<?, ?>> extends Ranker<G, F> {
	public NSGAII(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void rank(Population<G, F> population) {
		List<List<Individual<G, F>>> fronts = fastNonDominatedSort(population.getMembers());
		
		// Assign ranks to members from each successive pareto front. The first front is the highest ranked.
		int rank = population.size();
		for (List<Individual<G, F>> front : fronts) {
			// Sort within fronts/ranks by crowding distance, highest distance (best) to lowest.
			sortByCrowdedComparison(front);
			Collections.reverse(front);
			
			for (Individual<G, F> ind : front) {
				ind.setRank(rank);
				rank--;
			}
		}
	}

	/**
	 * Performs a fast non-domination sort of the specified individuals. The method returns the different domination
	 * fronts in ascending order by their rank.
	 * 
	 * @param individuals individuals to sort
	 * @return domination fronts in ascending order by their rank
	 */
	public List<List<Individual<G, F>>> fastNonDominatedSort(Collection<Individual<G, F>> individuals) {
		List<List<Individual<G, F>>> dominationFronts = new ArrayList<List<Individual<G, F>>>();

		HashMap<Individual<G, F>, List<Individual<G, F>>> individual2DominatedIndividuals = new HashMap<Individual<G, F>, List<Individual<G, F>>>();
		HashMap<Individual<G, F>, Integer> individual2NumberOfDominatingIndividuals = new HashMap<Individual<G, F>, Integer>();

		for (Individual<G, F> individualP : individuals) {
			individual2DominatedIndividuals.put(individualP, new ArrayList<Individual<G, F>>());
			individual2NumberOfDominatingIndividuals.put(individualP, 0);

			for (Individual<G, F> individualQ : individuals) {
				if (dominates(individualP, individualQ)) {
					individual2DominatedIndividuals.get(individualP).add(individualQ);
				} else {
					if (dominates(individualQ, individualP)) {
						individual2NumberOfDominatingIndividuals.put(individualP, individual2NumberOfDominatingIndividuals.get(individualP) + 1);
					}
				}
			}

			if (individual2NumberOfDominatingIndividuals.get(individualP) == 0) {
				// p belongs to the first front
				if (dominationFronts.isEmpty()) {
					dominationFronts.add(new ArrayList<Individual<G, F>>());
				}
				dominationFronts.get(0).add(individualP);
			}
		}

		int i = 1;
		while (dominationFronts.size() == i) {
			List<Individual<G, F>> nextDominationFront = new ArrayList<Individual<G, F>>();
			for (Individual<G, F> individualP : dominationFronts.get(i - 1)) {
				for (Individual<G, F> individualQ : individual2DominatedIndividuals.get(individualP)) {
					individual2NumberOfDominatingIndividuals.put(individualQ, individual2NumberOfDominatingIndividuals.get(individualQ) - 1);
					if (individual2NumberOfDominatingIndividuals.get(individualQ) == 0) {
						nextDominationFront.add(individualQ);
					}
				}
			}
			i++;
			if (!nextDominationFront.isEmpty()) {
				dominationFronts.add(nextDominationFront);
			}
		}

		return dominationFronts;
	}

	public List<Individual<G, F>> getTop(List<List<Individual<G, F>>> fronts, int numToSelect) {
		// Add all members from each successive rank until the next rank to add would go over the desired size.
		ArrayList<Individual<G, F>> top = new ArrayList<Individual<G, F>>();
		int i = 0;
		while (i < fronts.size() && top.size() + fronts.get(i).size() <= numToSelect) {
			// crowdingDistanceAssignment(fronts.get(i)); This was used in tournament for selection of parents in JNSGA2
			top.addAll(fronts.get(i));
			i++;
		}

		// If we haven't reached the desired size, add individuals according to crowded comparison operator.
		if (i < fronts.size() && top.size() != numToSelect) {
			List<Individual<G, F>> front = fronts.get(i);
			sortByCrowdedComparison(front);
			int numberOfMissingIndividuals = numToSelect - top.size();
			top.addAll(front.subList(0, numberOfMissingIndividuals));
		}

		return top;
	}

	private void sortByCrowdedComparison(List<Individual<G, F>> individuals) {
		List<IndWrapper> crowdingDistances = new ArrayList<>(individuals.size());
		
		// Set initial crowding distances to 0.
		for (Individual<G, F> ind : individuals) {
			crowdingDistances.add(new IndWrapper(ind));
		}
		
		int last = individuals.size() - 1;
		for (EvaluationDescription evDesc : individuals.get(0).evaluationData.getFitnessResults().keySet()) {	
			// Sort using this objective.
			Collections.sort(individuals, new FitnessValueComparator(evDesc));

			// so that boundary points are always selected
			crowdingDistances.get(0).crowdingDistance = Double.POSITIVE_INFINITY; // Elites always first.
			// Don't replace an infinity value.
			crowdingDistances.get(last).crowdingDistance = Math.max(crowdingDistances.get(last).crowdingDistance, Double.MAX_VALUE);

			// If minimal and maximal fitness value for this objective are equal, do not change crowding distance
			if (individuals.get(0).evaluationData.getResult(evDesc) != individuals.get(last).evaluationData.getResult(evDesc)) {
				double range = individuals.get(last).evaluationData.getResult(evDesc) - individuals.get(0).evaluationData.getResult(evDesc);
				for (int i = 1; i < last; i++) {
					crowdingDistances.get(i).crowdingDistance += (crowdingDistances.get(i + 1).individual.evaluationData.getResult(evDesc) - crowdingDistances.get(i - 1).individual.evaluationData.getResult(evDesc)) / range;
				}
			}
		}
		
		Collections.sort(crowdingDistances);
		
		individuals.clear();
		for (IndWrapper ind : crowdingDistances) {
			individuals.add(ind.individual);
		}
	}
	
	private class IndWrapper implements Comparable<IndWrapper>, Stringable {
		public final Individual<G, F> individual;
		public double crowdingDistance = 0;
		
		public IndWrapper(Individual<G, F> individual) {
			this.individual = individual;
		}
		
		@Override
		public int compareTo(IndWrapper other) {
			if (crowdingDistance < other.crowdingDistance) {
				return -1;
			}
			if (crowdingDistance > other.crowdingDistance) {
				return 1;
			}
			if (individual.id < other.individual.id) {
				return -1;
			}
			if (individual.id > other.individual.id) {
				return 1;
			}
			return 0;
		}
		

		@Override
		public void getStringableMap(Map<String, Object> map) {
			map.put("individual", individual);
			map.put("crowdingDistance", crowdingDistance);
		}
	}
	
	/**
	 * Returns true iff the first individual dominates the second individual, i.e. the first is at least as good as the second
	 * in all fitness objectives and for at least one objective it is better (has higher fitness value).
	 * Special cases: If the first individual has any NaN results then it cannot dominate the second.
     * If the second individual has any NaN results and the first does not then the first dominates the second.
	 */
	public boolean dominates(Individual<G, F> ind1, Individual<G, F> otherIndividual) {
		// Check for NaN results first.
		boolean hasThisNaN = false;
		boolean hasOtherNaN = false;
		for (Entry<EvaluationDescription, Double> result : ind1.evaluationData.getFitnessResults().entrySet()) {
			if (result.getValue().equals(Double.NaN)) {
				hasThisNaN = true;
			}
			if ((new Double(otherIndividual.evaluationData.getResult(result.getKey()))).equals(Double.NaN)) {
				hasOtherNaN = true;
			}
		}

		if (hasThisNaN) {
			return false;
		}
		if (!hasThisNaN && hasOtherNaN) {
			return true;
		}

		// Both individuals have no NaN results.
		boolean atLeastOneObjectiveBetter = false;

		for (Entry<EvaluationDescription, Double> result : ind1.evaluationData.getFitnessResults().entrySet()) {
			if (result.getValue() < otherIndividual.evaluationData.getResult(result.getKey())) {
				return false;
			}
			if (result.getValue() > otherIndividual.evaluationData.getResult(result.getKey())) {
				atLeastOneObjectiveBetter = true;
			}
		}

		return atLeastOneObjectiveBetter;
	}

	/**
	 * This inner class implements a comparator using the specified objective fitness value of two individuals.
	 */
	private class FitnessValueComparator implements Comparator<Individual<G, F>> {
		private EvaluationDescription evDesc;

		/**
		 * @param indexObjective objective/fitness index to sort on.
		 */
		private FitnessValueComparator(EvaluationDescription evDesc) {
			this.evDesc = evDesc;
		}

		public int compare(Individual<G, F> individual1, Individual<G, F> individual2) {
			if (individual1.evaluationData.getResult(evDesc) < individual2.evaluationData.getResult(evDesc)) {
				return -1;
			}
			if (individual1.evaluationData.getResult(evDesc) > individual2.evaluationData.getResult(evDesc)) {
				return 1;
			}
			// compare IDs if fitness is the same to keep the ordering stable.
			return (int) Math.signum(individual1.id - individual2.id);
		}
	}
}
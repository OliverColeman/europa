package com.ojcoleman.europa.core;

/**
 * Represents an individual in the {@link Population} used in the evolutionary algorithm. An individual is a container
 * for a {@link Genotype}, The {@link Function} to be evaluated (transcribed from the Genotype if the Genotype is not
 * also the Function), and the resulting {@link EvaluationData}.
 * 
 * @author O. J. Coleman
 */
public class Individual<G extends Genotype<?>> {
	/**
	 * The genotype represented by this individual.
	 */
	public final G genotype;

	/**
	 * Contains the results of evaluating this individual.
	 */
	public final EvaluationData evaluationData;

	/**
	 * The rank of this individual within the population, according to a {@link Ranker}.
	 */
	protected double rank;

	/**
	 * The {@link Function} to be evaluated (transcribed from the Genotype if the Genotype is not also the Function).
	 */
	protected Function<?, ?> function;

	/**
	 * <strong>NOTE: All Individuals should be created via {@link Population#newIndividual(Object...)}.</strong> Creates
	 * a new Individual with the given genotype.
	 */
	public Individual(G genotype) {
		this.genotype = genotype;
		evaluationData = new EvaluationData();
	}

	/**
	 * Returns true if any evaluation results are present in {@link #evaluationData}.
	 */
	public boolean isEvaluated() {
		return !evaluationData.getResults().isEmpty();
	}

	/**
	 * Returns the function generated (transcribed) from the genotype of this individual.
	 */
	public Function<?, ?> getFunction() {
		return function;
	}

	/**
	 * Sets the function transcribed from the genotype of this individual.
	 */
	public void setFunction(Function<?, ?> function) {
		this.function = function;
	}

	/**
	 * Returns the rank of this individual within the {@link Population}.
	 * 
	 * @see Ranker
	 */
	public double getRank() {
		return rank;
	}

	/**
	 * Sets the rank of this individual within the {@link Population}. Generally speaking only a {@link Ranker} should
	 * call this.
	 */
	public void setRank(double rank) {
		this.rank = rank;
	}
}

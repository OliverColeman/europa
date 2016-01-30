package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collections;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;

/**
 * Represents an individual in the {@link Population} used in the evolutionary algorithm. An individual is a container
 * for a {@link Genotype}, The {@link Function} to be evaluated (transcribed from the Genotype if the Genotype is not
 * also the Function), and the resulting {@link EvaluationData}.
 * 
 * @author O. J. Coleman
 */
public class Individual<G extends Genotype<?>, F extends Function<?, ?>> extends PrototypeBase implements Comparable<Individual<G, F>> {
	/**
	 * The genotype represented by this individual.
	 */
	public final G genotype;
	
	/**
	 * Contains the results of evaluating this individual.
	 */
	public final EvaluationData evaluationData;

	/**
	 * The rank of this individual within the population, according to a {@link Ranker}. A higher value indicates a higher (better) rank.
	 */
	protected double rank;

	/**
	 * The {@link Function} to be evaluated (transcribed from the Genotype if the Genotype is not also the Function).
	 */
	protected F function;

	
	/**
	 * The Species this Individual is currently associated with.
	 */
	Species<G, F> species;
	
	
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public Individual(Configuration config) {
		super(config);
		genotype = null;
		evaluationData = null;
	}


	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param gene the underlying Gene for the new Allele.
	 */
	public Individual(Individual<G, F> prototype, G genotype) {
		super(prototype);
		
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
	public void setFunction(F function) {
		this.function = function;
	}

	/**
	 * Returns the rank of this individual within the {@link Population}. A higher value indicates a higher (better) rank.
	 * 
	 * @see Ranker
	 */
	public double getRank() {
		return rank;
	}

	/**
	 * Sets the rank of this individual within the {@link Population}. Generally only a {@link Ranker} should
	 * call this.
	 */
	public void setRank(double rank) {
		this.rank = rank;
	}
	
	
	/**
	 * Get the Species this Individual is currently in.
	 */
	public Species<G, F> getSpecies() {
		return species;
	}


	@Override
	public int compareTo(Individual<G, F> other) {
		if (rank < other.rank) {
			return -1;
		}
		if (rank > other.rank) {
			return 1;
		}
		return 0;
	}
}

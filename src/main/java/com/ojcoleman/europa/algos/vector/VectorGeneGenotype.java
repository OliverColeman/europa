package com.ojcoleman.europa.algos.vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;

/**
 * Base class for Genotypes that comprise {@link VectorGene} and {@link VectorAllele}s.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGeneGenotype<A extends VectorAllele<?>> extends Genotype<A> {
	/**
	 * IsPrototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public VectorGeneGenotype(JsonObject config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a Genotype
	 * with the given ID (should generally be unique), alleles and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param id The ID for the new Genotype.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public VectorGeneGenotype(VectorGeneGenotype<A> prototype, long id, Genotype<?>... parents) {
		super(prototype, id, parents);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a Genotype
	 * with the given ID (should generally be unique) and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param id The ID for the new Genotype.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. A copy of each allele is made
	 *            and put into a new collection.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public VectorGeneGenotype(VectorGeneGenotype<A> prototype, long id, Collection<A> alleles, Genotype<?>... parents) {
		super(prototype, id, alleles, parents);
	}
}

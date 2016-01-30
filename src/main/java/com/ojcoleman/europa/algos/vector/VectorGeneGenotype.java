package com.ojcoleman.europa.algos.vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;

/**
 * Base class for Genotypes that comprise {@link VectorGene} and {@link VectorAllele}s.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGeneGenotype<A extends VectorAllele<?>> extends Genotype<A> {
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public VectorGeneGenotype(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a Genotype
	 * with the same alleles and genes as the given genotype. The parent is set to the given prototype.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 */
	public VectorGeneGenotype(VectorGeneGenotype<A> prototype) {
		super(prototype);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a Genotype
	 * with the given ID (should generally be unique) and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. A copy of each allele is made
	 *            and put into a new collection.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	public VectorGeneGenotype(VectorGeneGenotype<A> prototype, Collection<A> alleles, List<Genotype<?>> parents) {
		super(prototype, alleles, parents);
	}
}

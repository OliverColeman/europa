package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collection;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Prototype;

/**
 * Dummy genotype used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyGenotype extends Genotype<Allele<?>> {
	/**
	 * Prototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public DummyGenotype(JsonObject config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param id The ID for the new Genotype.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. These are put in a new
	 *            collection.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public DummyGenotype(DummyGenotype prototype, long id, Collection<Allele<?>> alleles, Genotype<?>... parents) {
		super(prototype, id, alleles, parents);
	}

	@Override
	protected Collection<Allele<?>> newAlleleCollection() {
		return new ArrayList<Allele<?>>();
	}
}

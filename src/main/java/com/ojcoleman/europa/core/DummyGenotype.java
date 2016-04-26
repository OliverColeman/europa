package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;

/**
 * Dummy genotype used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyGenotype extends Genotype<Allele<?>> {
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(Configuration)}.
	 */
	public DummyGenotype(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. These are put in a new
	 *            collection.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner}s).
	 */
	public DummyGenotype(DummyGenotype prototype, Collection<Allele<?>> alleles, List<Genotype<?>> parents) {
		super(prototype, alleles, parents);
	}

	@Override
	protected Collection<Allele<?>> newAlleleCollection() {
		return new ArrayList<Allele<?>>();
	}
}

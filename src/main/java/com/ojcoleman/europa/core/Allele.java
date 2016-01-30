package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;

/**
 * <p>
 * Base class for representations of alleles within a {@link Genotype}. Alleles represent the mutable aspect of
 * {@link Gene}s and as such generally contain values that may be altered by a {@link Mutator} or {@link Recombiner}.
 * </p>
 * <p>
 * <strong>Sub-classes must implement a copy-constructor that accepts a single parameter which is the allele to copy,
 * and which should generally just call <em>super()</em> with the allele to copy.</strong> See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
 * <p>
 * Note that an Allele belongs to a specific Genotype, but may share the same Gene as Alleles in other Genotypes.
 * Alleles and Genotypes directly reference one another, but a Gene is only referenced by Alleles. A given Gene should
 * only be included once in a Genotype.
 * </p>
 * 
 * @author O. J. Coleman
 */
public class Allele<G extends Gene> extends PrototypeBase {
	/**
	 * The gene underlying this allele.
	 */
	public final G gene;

	/**
	 * The genotype this allele belongs to.
	 */
	protected Genotype<?> genotype;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public Allele(Configuration config) {
		super(config);
		gene = null;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. The underlying
	 * {@link Gene} will be the same as the Allele to copy.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param allele The Allele to copy.
	 */
	public Allele(Allele<G> prototype) {
		super(prototype);
		this.gene = prototype.gene;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param gene the underlying Gene for the new Allele.
	 */
	public Allele(Allele<G> prototype, G gene) {
		super(prototype);
		this.gene = gene;
	}

	/**
	 * Marks this Allele as belonging to the specified Genotype. This should generally only be called by Genotype (via
	 * {@link Genotype#addAllele(Allele)}).
	 * 
	 * @throws IllegalStateException If this Allele has already been added to a Genotype.
	 */
	public void setGenotype(Genotype<?> genotype) {
		if (this.genotype != null) {
			throw new IllegalStateException("An Allele may only be added to one Genotype.");
		}
		this.genotype = genotype;
	}

	/**
	 * Marks this Allele as not belonging to any Genotype (if it was in the first place). This should generally only be
	 * called by Genotype (via {@link Genotype#removeAllele(Allele)}).
	 */
	public void clearGenotype() {
		genotype = null;
	}

	/**
	 * Returns the Genotype this Allele belongs to, or null if it does not belong to one.
	 */
	public Genotype<?> getGenotype() {
		return genotype;
	}
}

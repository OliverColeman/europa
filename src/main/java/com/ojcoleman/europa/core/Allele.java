package com.ojcoleman.europa.core;

/**
 * Base class for representations of alleles within a {@link Genotype}. Alleles represent the mutable aspect of
 * {@link Gene}s and as such generally contain values that may be altered by a {@link Mutator} or {@link Recombiner}.
 * 
 * @author O. J. Coleman
 */
public abstract class Allele<G extends Gene> {
	public final G gene;

	public Allele(G gene) {
		this.gene = gene;
	}
}

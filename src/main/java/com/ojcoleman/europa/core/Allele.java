package com.ojcoleman.europa.core;

/**
 * Base class for representations of alleles within a {@link Genotype}. Alleles represent the mutable aspect of
 * {@link Gene}s and as such generally contain values that may be altered by a {@link Mutator} or {@link Recombiner}.
 * 
 * @author O. J. Coleman
 */
public abstract class Allele<G extends Gene> {
	/**
	 * The gene underlying this allele.
	 */
	public final G gene;

	/**
	 * Copy constructor. The new Allele references the gene in the given Allele.
	 * <strong>Sub-classes must provide their own copy constructors and call this (or their super-class') copy constructor from it.</strong>
	 * Sub-class implementations should create a deep copy of the values they define.
	 */
	public Allele(Allele<G> allele) {
		this.gene = allele.gene;
	}
	
	/**
	 * Create a new Allele referencing the given Gene.
	 */
	public Allele(G gene) {
		this.gene = gene;
	}
}

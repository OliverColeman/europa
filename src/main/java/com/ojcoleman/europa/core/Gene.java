package com.ojcoleman.europa.core;

/**
 * Base class for representations of genes within a {@link Genotype}. A gene represents a structural part of the
 * genotype. The set of genes in a genotype may be fixed when the initial population is created or they may be added or
 * removed by a {@link Mutator} or {@link Recombiner}. Any values a gene contains should not be mutable; they should be
 * fixed when the gene is added to the genotype.
 */
public abstract class Gene {
	/**
	 * The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 */
	public final Object type;
	
	/**
	 * Create a gene of the specified type.
	 * @param type The type of gene, if applicable to the evolutionary algorithm in use. Usually an enum constant. May be null.
	 */ 
	public Gene(Object type) {
		this.type = type;
	}
}

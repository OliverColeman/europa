package com.ojcoleman.europa.core;

/**
 * Base class for representations of genes within a {@link Genotype}. A gene represents a structural part of the
 * genotype. The set of genes in a genotype may be fixed when the initial population is created or they may be added or
 * removed by a {@link Mutator} or {@link Recombiner}. Any values a gene contains should not be mutable; they should be
 * fixed when the gene is added to the genotype.
 */
public abstract class Gene {

}

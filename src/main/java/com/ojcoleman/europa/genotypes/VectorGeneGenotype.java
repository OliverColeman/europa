package com.ojcoleman.europa.genotypes;

import java.util.Set;

import com.ojcoleman.europa.core.Genotype;

/**
 * Base class for Genotypes that comprise {@link VectorGene}s.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGeneGenotype<G extends VectorAllele<?>> extends Genotype<G> {
	@SafeVarargs
	public VectorGeneGenotype(Set<G> alleles, Genotype<G>... parents) {
		super(alleles, parents);
	}
}

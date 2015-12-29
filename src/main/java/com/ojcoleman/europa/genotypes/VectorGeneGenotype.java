package com.ojcoleman.europa.genotypes;

import java.util.Collection;
import java.util.Set;

import com.ojcoleman.europa.core.Genotype;

/**
 * Base class for Genotypes that comprise {@link VectorGene}s.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorGeneGenotype<G extends VectorAllele<?>> extends Genotype<G> {
	public VectorGeneGenotype(long id, Collection<G> alleles, Genotype<G>... parents) {
		super(id, alleles, parents);
	}
}

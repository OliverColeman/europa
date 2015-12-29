package com.ojcoleman.europa.algos.neat;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ojcoleman.europa.algos.neat.NEATAllele;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.Genotype;

/**
 * @author O. J. Coleman
 *
 */
public class NEATGenotype extends Genotype<NEATAllele<?>> {
	@SafeVarargs
	public NEATGenotype(long id, SortedSet<NEATAllele<?>> alleles, Genotype<?>... parents) {
		super(id, alleles, parents);
	}

	@Override
	public Genotype<NEATAllele<?>> create(long id, Collection<NEATAllele<?>> alleles, Genotype<?>... parents) {
		if (alleles instanceof SortedSet) {
			return new NEATGenotype(id, (SortedSet<NEATAllele<?>>) alleles, parents);
		}
		return new NEATGenotype(id, new TreeSet<NEATAllele<?>>(alleles), parents);
	
	}
}

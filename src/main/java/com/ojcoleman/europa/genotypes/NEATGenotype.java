package com.ojcoleman.europa.genotypes;

import java.util.SortedSet;

import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.genotypes.NEATAllele;

/**
 * @author O. J. Coleman
 *
 */
public class NEATGenotype extends Genotype<NEATAllele> {

	public NEATGenotype(SortedSet<NEATAllele> alleles, Genotype<NEATAllele>[] parents) {
		super(alleles, parents);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Genotype<NEATAllele> clone() {
		// TODO Auto-generated method stub
		return null;
	}

}

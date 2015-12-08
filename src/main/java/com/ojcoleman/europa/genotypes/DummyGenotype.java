package com.ojcoleman.europa.genotypes;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;

/**
 * Dummy genotype used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyGenotype extends Genotype<Allele<?>> {
	public DummyGenotype(Set<Allele<?>> alleles, Genotype<Allele<?>>[] parents) {
		super(alleles, parents);
	}

	@Override
	public Genotype<Allele<?>> clone() {
		return null;
	}
	
}

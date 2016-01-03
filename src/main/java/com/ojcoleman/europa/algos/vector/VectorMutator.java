package com.ojcoleman.europa.algos.vector;

import java.util.Collection;
import java.util.Random;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Mutator;
import com.ojcoleman.europa.core.Run;

/**
 * 
 * @author O. J. Coleman
 */
public class VectorMutator extends Mutator<VectorGeneGenotype<VectorAllele<?>>> {
	public enum PerturbationType {
		NORMAL, UNIFORM
	}
	
	@Parameter (description="The probability of applying this mutator to any given allele.", defaultValue="0.25", minimumValue="0", maximumValue="1")
	protected double alleleApplyRate;
	
	@Parameter (description="The probability of applying this mutator to any given value within an allele.", defaultValue="0.25", minimumValue="0", maximumValue="1")
	protected double valueApplyRate;
	
	@Parameter (description="The type of probability distribution to use to perturb an allele value. May be 'normal' or 'uniform'.", defaultValue="normal")
	protected PerturbationType perturbationType;
	
	@Parameter (description="The amount to perturb an allele value. For 'normal' perturbation type this is the standard deviation, for 'uniform' this is the maximum amount to perturb by.", defaultValue="1", minimumValue="0")
	protected double perturbationRate;

	@Parameter (description="Whether to multiply the perturbationRate by the allowable range of the value to perturb (for example if the allowable interval of the value is [-1, 1] then the perturbationRate would be multiplied by 2).", defaultValue="true")
	protected boolean perturbationRateNormalise;
	
	
	public VectorMutator(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		// TODO Auto-generated constructor stub
	}
	
	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.core.Mutator#mutate(com.ojcoleman.europa.core.Genotype)
	 */
	@Override
	public void mutate(VectorGeneGenotype<VectorAllele<?>> genotype) {
		Random random = getParentComponent(Run.class).random;
		
		Collection<VectorAllele<?>> alleles = genotype.getAlleles();
		for (VectorAllele<?> allele : alleles) {
			if (random.nextDouble() < alleleApplyRate) {
				for (int i = 0; i < allele.vector.metadata.size(); i++) {
					if (random.nextDouble() < valueApplyRate) {
						double perturbation;
						if (perturbationType == PerturbationType.NORMAL) {
							perturbation = random.nextGaussian();
						}
						else {
							perturbation = random.nextDouble() * 2 -1;
						}
						if (perturbationRateNormalise) {
							perturbation *= allele.vector.metadata.bound(i).range().doubleValue();
						}
						double newVal = allele.vector.get(i) + perturbation;
						allele.vector.set(i, newVal);
					}
				}
			}
		}
	}
}

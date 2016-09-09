package com.ojcoleman.europa.algos.neat;

import com.ojcoleman.europa.algos.vector.VectorAllele;
import com.ojcoleman.europa.algos.vector.VectorMutator;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Mutator for neurons. Extends VectorMutator to prevent mutating bias values for input neurons.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronMutator extends VectorMutator {
	public NEATNeuronMutator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		if (alleleClass != null && alleleClass.isAssignableFrom(NEATNeuronAllele.class)) {
			throw new IllegalArgumentException("The parameter NEATNeuronMutator.alleleClass must be set to NEATNeuronAllele or a superclass thereof.");
		}
		if (alleleClass == null) {
			alleleClass = (Class<VectorAllele<?>>) NEATNeuronAllele.class.asSubclass(VectorAllele.class);
		}
	}
	
	@Override
	protected boolean shouldMutateValue(VectorAllele<?> allele, int index) {
		// Mutate the value if the value is not the bias parameter, or the allele isn't for an input neuron.
		return !allele.vector.metadata.label(index).equals("bias") || !allele.gene.types.contains(NNPart.NEURON_INPUT);
	}
}

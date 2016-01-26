package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.core.Evolver;
import com.ojcoleman.europa.core.Mutator;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.Topology;

/**
 * Mutator that adds neurons to a NEAT genotype. A neuron is added by replacing an existing synapse with a neuron that
 * is connected to the same neurons the original synapse was connected to.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronAddMutator extends Mutator<NEATGenotype> {
	@IsParameter(description = "Specifies the maximum number of neurons that may be added.", defaultValue = "1", minimumValue = "1")
	protected int maximum;

	@IsParameter(description = "For each neuron that may be added (see 'maximum'), specifies the probability of adding the neuron. ", defaultValue = "0.01", minimumValue = "0", maximumValue = "1")
	protected double applyRate;

	public NEATNeuronAddMutator(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ojcoleman.europa.core.Mutator#mutate(com.ojcoleman.europa.core.Genotype)
	 */
	@Override
	public void mutate(NEATGenotype genotype) {
		Run run = getParentComponent(Run.class);
		Random random = run.random;
		NNConfig nnConfig = ((NeuralNetworkTranscriber<?>) run.getTranscriber()).getNeuralNetworkPrototype().getConfig();
		NEATEvolver evolver = this.getParentComponent(NEATEvolver.class);

		// Get a list of the synapses.
		List<NEATSynapseAllele> synapseIDs = new ArrayList<>(genotype.getSynapses().values());
		// Randomise so we pick synapses to replace randomly and without repeats.
		Collections.shuffle(synapseIDs, random);

		// For each neuron that may be added.
		for (int i = 0; i < maximum && i < synapseIDs.size(); i++) {
			// If we should add a neuron according to probabilistic apply rate.
			if (random.nextDouble() < applyRate) {
				// We use the methods on NEATEvolver as evolver keeps a record of all synapses and neurons.
				// The record allows reusing the gene from an existing structurally identical neuron or synapse
				// (assuming the gene parameter values are the same, if applicable).

				// Pick a synapse to replace.
				NEATSynapseAllele synapse = synapseIDs.get(i);

				// Add the new neuron.
				NEATNeuronAllele newNeuron = evolver.newNeuronAllele(genotype, synapse.gene.id);
				genotype.addAllele(newNeuron);

				// Add the new pre and post synapses.
				NEATSynapseAllele preSynapseAllele = evolver.newSynapseAllele(genotype, synapse.gene.sourceID, newNeuron.gene.id);
				genotype.addAllele(preSynapseAllele);
				NEATSynapseAllele postSynapseAllele = evolver.newSynapseAllele(genotype, newNeuron.gene.id, synapse.gene.destinationID);
				genotype.addAllele(postSynapseAllele);
			}
		}
	}
}

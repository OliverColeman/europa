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
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Evolver;
import com.ojcoleman.europa.core.Mutator;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.Topology;
import com.ojcoleman.europa.util.Stringer;

/**
 * Mutator that adds neurons to a NEAT genotype. A neuron is added by replacing an existing synapse with a neuron that
 * is connected to the same neurons the original synapse was connected to.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronAddMutator extends Mutator<NEATGenotype> {
	@Parameter(description = "Specifies the maximum number of neurons that may be added, the actual number added is selected uniformly between 1 and this number.", defaultValue = "1", minimumValue = "1")
	protected int maximum;

	public NEATNeuronAddMutator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
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

		// Get a list of the current synapses.
		List<NEATSynapseAllele> allSynapses = new ArrayList<>(genotype.getSynapses().values());
		// Randomise so we pick synapses to replace randomly and without repeats.
		Collections.shuffle(allSynapses, random);
		
		int numberToAdd = random.nextInt(maximum) + 1;
		int addedCount = 0;
	
		// For each neuron that may be added.
		for (int i = 0; addedCount < numberToAdd && i < allSynapses.size(); i++) {
			// Get the potential synapse to replace.
			NEATSynapseAllele synapse = allSynapses.get(i);

			if (synapse.enabled()) {
				// We use the methods on NEATEvolver as evolver keeps a record of all synapses and neurons.
				// The record allows reusing the gene from an existing structurally identical neuron or synapse
				// (assuming the gene parameter values are the same, if applicable).
				NEATNeuronAllele newNeuron = evolver.newNeuronAllele(genotype, synapse.gene.id);
				
				// If we haven't already added an identical neuron to this genotype by splitting this 
				// synapse previously (same source and dest neurons, same or no params for the new neuron). 
				if (!genotype.hasGene(newNeuron.gene.id)) {
					genotype.addAllele(newNeuron);
					
					// Create the new pre and post synapses.
					NEATSynapseAllele preSynapseAllele = evolver.newSynapseAllele(genotype, synapse.gene.sourceID, newNeuron.gene.id);
					NEATSynapseAllele postSynapseAllele = evolver.newSynapseAllele(genotype, newNeuron.gene.id, synapse.gene.destinationID);
					
					// Set the weights of the new synapses.
					if (preSynapseAllele.vector.metadata.hasLabel("weight")) {
						preSynapseAllele.vector.set("weight", 1);
						postSynapseAllele.vector.set("weight", synapse.vector.get("weight"));
					}
					
					// Add the new pre and post synapses.
					genotype.addAllele(preSynapseAllele);
					genotype.addAllele(postSynapseAllele);
					
					// Disable the original synapse.
					synapse.setEnabled(false);
					
					addedCount++;
				}
			}
		}
	}
}

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
import com.ojcoleman.europa.core.Mutator;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.Topology;

/**
 * Mutator that adds synapses to a NEAT genotype.
 * 
 * @author O. J. Coleman
 */
public class NEATSynapseAddMutator extends Mutator<NEATGenotype> {
	public enum Type {
		/**
		 * A fixed maximum number of synapses may be added.
		 */
		FIXED,
		/**
		 * Synapses may be added anywhere a synapse can exist (no explicit maximum number).
		 */
		ANY
	}

	@IsParameter(description = "Specifies the type of limit on how many synapses may be added. May be 'fixed' (a fixed maximum number synapse may be added) or 'any' (synapses may be added anywhere a synapse can exist).", defaultValue = "fixed")
	protected Type type;

	@IsParameter(description = "If type is set to 'fixed', specifies the maximum number of synapses that may be added.", defaultValue = "1", minimumValue = "1")
	protected int fixedMaximum;

	@IsParameter(description = "For each synapse that may be added, specifies the probability of adding the synapse. ", defaultValue = "0.05", minimumValue = "0", maximumValue = "1")
	protected double applyRate;

	public NEATSynapseAddMutator(Component parentComponent, JsonObject componentConfig) throws Exception {
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
		NeuralNetworkTranscriber<?> transcriber = (NeuralNetworkTranscriber<?>) run.getSubComponent("transcriber", this);
		NNConfig nnConfig = transcriber.getNeuralNetworkPrototype().getConfig();
		NEATEvolver evolver = (NEATEvolver) run.getSubComponent("evolver", this);

		// Get a list of the neuron IDs.
		List<Long> neuronIDs = new ArrayList<>(genotype.getNeurons().keySet());

		// Make a convenient record of the existing synapses.
		Table<Long, Long, Boolean> synapses = HashBasedTable.create();
		for (NEATSynapseAllele allele : genotype.getSynapses().values()) {
			synapses.put(allele.gene.sourceID, allele.gene.destinationID, Boolean.TRUE);
		}

		// For each synapse to add, we iterate over a random ordering of source and destination neurons.
		// This way we'll try every possible combination in a random order.
		List<Long> sources = new ArrayList<>(neuronIDs);
		List<Long> dests = new ArrayList<>(neuronIDs);

		if (type == Type.FIXED) {
			// For each synapse that may be added.
			for (int i = 0; i < fixedMaximum; i++) {
				// If we should add a synapse according to probabilistic apply rate.
				if (random.nextDouble() < applyRate) {
					// For each synapse to add, we iterate over a random ordering of source and destination neurons.
					// This way we can try every possible combination in a random order without repeats.
					Collections.shuffle(sources, random);
					Collections.shuffle(dests, random);

					boolean synapsePermissible = true;
					for (long source : sources) {
						for (long dest : dests) {
							if (synapses.contains(source, dest)) {
								// If this synapse already exists, try again.
								synapsePermissible = false;
							} else if (nnConfig.getTopology() != Topology.RECURRENT) {
								// If recurrent synapses are not allowed, check if adding this synapse would
								// create a cycle.
								if (evolver.synapseWouldCreateCycle(source, dest, synapses)) {
									synapsePermissible = false;
								}
							}

							if (synapsePermissible) {
								addSynapse(genotype, source, dest, synapses, evolver);

								break;
							}
						}

						if (synapsePermissible) {
							break;
						}
					}

					if (!synapsePermissible) {
						// Couldn't find anywhere to add any (more) synapses,
						// nothing else to do, so return.
						return;
					}
				}
			}
		} else {
			// For every possible synapse.
			for (long source : sources) {
				for (long dest : dests) {
					// If the synapse doesn't already exist.
					if (!synapses.contains(source, dest)) {
						// If we should add a synapse here according to probabilistic apply rate.
						if (random.nextDouble() < applyRate) {
							// If recurrent synapses are allowed or adding this synapse wouldn't cause a cycle
							if (nnConfig.getTopology() == Topology.RECURRENT || !evolver.synapseWouldCreateCycle(source, dest, synapses)) {
								// Add the synapse.
								addSynapse(genotype, source, dest, synapses, evolver);
							}
						}
					}
				}
			}
		}
	}

	private void addSynapse(NEATGenotype genotype, long source, long dest, Table<Long, Long, Boolean> synapses, NEATEvolver evolver) {
		// We use the method on NEATEvolver as evolver keeps a record of all synapses created between any two neurons.
		// The record allows reusing the gene from an existing synapse between the same two neurons (assuming the gene
		// parameter values are the same, if applicable).
		NEATSynapseAllele allele = evolver.newSynapseAllele(genotype, source, dest);
		genotype.addAllele(allele);

		// Update record of synapses in this network.
		synapses.put(source, dest, Boolean.TRUE);
	}
}

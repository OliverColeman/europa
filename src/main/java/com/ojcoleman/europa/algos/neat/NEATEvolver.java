package com.ojcoleman.europa.algos.neat;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Evolver;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;

/**
 * Implements the NEAT evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public class NEATEvolver extends Evolver {
	// Central store mapping all innovation ID/gene parameter pairs to genes.
	// private Table<Long, Vector, NEATGene> innovationIDToGene;

	// Central store mapping all connections/connection gene parameter pairs ever created between two neuron IDs to
	// their corresponding gene.
	private Table<ImmutablePair<Long, Long>, Vector, NEATSynapseGene> connectionToGene;

	// Central store mapping from synapse ID/neuron gene parameter pairs to the genes of neurons that were created by
	// splitting that synapse with a neuron with those gene parameters.
	private Table<Long, Vector, NEATNeuronGene> synapseIDToNeuronGene;

	private Run run;
	private NNConfig nnConfig;

	public NEATEvolver(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		// innovationIDToGene = HashBasedTable.create();
		connectionToGene = HashBasedTable.create();
		synapseIDToNeuronGene = HashBasedTable.create();

		run = getParentComponent(Run.class);
		nnConfig = ((NeuralNetworkTranscriber<?>) run.getTranscriber()).getNNConfig();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ojcoleman.europa.core.Evolver#evolvePopulation(com.ojcoleman.europa.core.Population, java.util.List)
	 */
	@Override
	public void evolvePopulation(Population pop, List<List<Individual>> species) {
		// TODO Auto-generated method stub

	}

	/**
	 * Create a new neuron allele for a neuron that is replacing the given synapse (see {@link NEATNeuronAddMutator}).
	 * If a previous mutation added a neuron by splitting this synapse then the gene from that neuron will be reused.
	 * When the corresponding synapse alleles are created the genes from the equivalent synapses will be reused if
	 * possible.
	 * 
	 * @param synapseID The innovation ID of the synapse that is being split to add the new neuron.
	 */
	public NEATNeuronAllele newNeuronAllele(long synapseID) {
		// If gene parameters are not used then this will be an empty vector.
		Vector geneParams = nnConfig.neuron().createGeneVector();

		// If we have already added the same neuron replacing the same synapse in another genotype reuse the gene from
		// it.
		NEATNeuronGene gene = synapseIDToNeuronGene.get(synapseID, geneParams);
		if (gene == null) {
			gene = new NEATNeuronGene(NNPart.NEURON_HIDDEN, run.getNextID(), geneParams);
			synapseIDToNeuronGene.put(synapseID, geneParams, gene);
		}
		return new NEATNeuronAllele(gene, nnConfig.neuron().createAlleleVector());
	}

	/**
	 * Create a new NEAT synapse allele. If there is already a NEATSynapseGene describing a synapse with the same
	 * randomly generated gene parameter values (if applicable) between the given source and destination it will be
	 * reused.
	 * 
	 * @param sourceID the innovation ID of the source neuron.
	 * @param destinationID the innovation ID of the destination neuron.
	 * @return the new allele.
	 */
	public NEATSynapseAllele newSynapseAllele(long sourceID, long destinationID) {
		// If gene parameters are not used then this will be an empty vector.
		Vector geneParams = nnConfig.synapse().createGeneVector();

		ImmutablePair<Long, Long> neuronIDs = ImmutablePair.of(sourceID, destinationID);

		// If we have already added the same connection in another genotype reuse the gene from it.
		NEATSynapseGene gene = connectionToGene.get(neuronIDs, geneParams);
		if (gene == null) {
			gene = new NEATSynapseGene(run.getNextID(), geneParams, sourceID, destinationID);
			connectionToGene.put(neuronIDs, geneParams, gene);
		}

		return new NEATSynapseAllele(gene, nnConfig.synapse().createAlleleVector());
	}
}

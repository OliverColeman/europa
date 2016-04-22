package com.ojcoleman.europa.algos.neat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.DefaultEvolver;
import com.ojcoleman.europa.core.Evolver;
import com.ojcoleman.europa.core.Function;
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
public class NEATEvolver extends DefaultEvolver<NEATGenotype> {
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

	
	public NEATEvolver(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		// innovationIDToGene = HashBasedTable.create();
		connectionToGene = HashBasedTable.create();
		synapseIDToNeuronGene = HashBasedTable.create();

		run = getParentComponent(Run.class);
	}
	
	private NNConfig getNNConfig() {
		if (nnConfig == null) {
			nnConfig = ((NeuralNetworkTranscriber<?>) run.getTranscriber()).getNeuralNetworkPrototype().getConfig();
		}
		return nnConfig;
	}

	
	/**
	 * Create a new neuron allele for a neuron that is replacing the given synapse (see {@link NEATNeuronAddMutator}).
	 * If a previous mutation added a neuron by splitting this synapse then the gene from that neuron will be reused.
	 * When the corresponding synapse alleles are created the genes from the equivalent synapses will be reused if
	 * possible.
	 * 
	 * @param synapseID The innovation ID of the synapse that is being split to add the new neuron.
	 */
	public NEATNeuronAllele newNeuronAllele(NEATGenotype genotype, long synapseID) {
		// If gene parameters are not used then this will be an empty vector.
		Vector geneParams = getNNConfig().neuron().createGeneVector(run.random);

		// If we have already added the same neuron replacing the same synapse in another genotype reuse the gene from
		// it.
		NEATNeuronGene gene = synapseIDToNeuronGene.get(synapseID, geneParams);
		if (gene == null) {
			//new NEATNeuronGene(gene, NNPart.NEURON_HIDDEN, run.getNextID(), geneParams);
			gene = genotype.neuronGenePrototype.newInstance(NNPart.NEURON_HIDDEN, geneParams);

			synapseIDToNeuronGene.put(synapseID, geneParams, gene);
		}

		NEATNeuronAllele allele = genotype.neuronAllelePrototype.newInstance(gene, getNNConfig().neuron().createAlleleVector());
		return allele;
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
	public NEATSynapseAllele newSynapseAllele(NEATGenotype genotype, long sourceID, long destinationID) {
		// If gene parameters are not used then this will be an empty vector.
		Vector geneParams = getNNConfig().synapse().createGeneVector(run.random);

		ImmutablePair<Long, Long> neuronIDs = ImmutablePair.of(sourceID, destinationID);

		// If we have already added the same connection in another genotype reuse the gene from it.
		NEATSynapseGene gene = connectionToGene.get(neuronIDs, geneParams);
		if (gene == null) {
			// new NEATSynapseGene(genotype.synapseGenePrototype, sourceID, destinationID, geneParams);
			gene = genotype.synapseGenePrototype.newInstance(sourceID, destinationID, geneParams);
			
			connectionToGene.put(neuronIDs, geneParams, gene);
		}
		// new NEATSynapseAllele(genotype.synapseAllelePrototype, gene, nnConfig.synapse().createAlleleVector());
		return genotype.synapseAllelePrototype.newInstance(gene, getNNConfig().synapse().createAlleleVector());
	}

	/**
	 * Utility method to check if adding the specified synapse would create a cycle.
	 * 
	 * @param sourceID The source neuron ID of the proposed synapse.
	 * @param destinationID The destination neuron ID of the proposed synapse.
	 * @param synapses a table of synapses. The row keys are source neuron IDs and the column keys are destination
	 *            neuron IDs (the values are a superfluous Boolean, the existence of the mapping row key/column
	 *            key/value indicates the presence of the synapse).
	 * @return returns true iff adding the specified synapse would create a cycle.
	 */
	public boolean synapseWouldCreateCycle(Long sourceID, Long destinationID, Table<Long, Long, Boolean> synapses) {
		// Adding a synapse from source to dest will create a cycle iff there exists a path from dest to source.
		return pathExists(destinationID, sourceID, synapses, new HashSet<Long>());
	}

	/**
	 * Recursively searches the network for a (directed) path from <code>start</code> to <code>end</code>.
	 * 
	 * @param start Start vertex ID
	 * @param end End vertex ID
	 * @param synapses a table of synapses. The row keys are source vertex IDs and the column keys are destination
	 *            vertex IDs (the values are a superfluous Boolean, the existence of the mapping row key/column
	 *            key/value indicates the presence of the synapse).
	 * @param alreadyTraversedSources The set of vertices whose outgoing synapses have already been traversed,
	 *            maintained to avoid redundant searching.
	 * @return returns true if neurons are the same, or a path lies between src and dest in connGenes connected graph
	 */
	private static boolean pathExists(long start, long end, Table<Long, Long, Boolean> synapses, Set<Long> alreadyTraversedSources) {
		// Don't traverse synapses more than once.
		if (alreadyTraversedSources.contains(start)) {
			return false;
		}
		alreadyTraversedSources.add(start);

		// If a path has been found.
		if (start == end) {
			return true;
		}

		// Traverse each synapse from the given star/source, check if a
		// path exists from their destination vertices to the end vertex.
		// Table.row(row key / source vertex ID).keySet() returns only the column
		// keys / dest vertex IDs for that row key for which a value/synapse exists.
		for (long dest : synapses.row(start).keySet()) {
			if (pathExists(dest, end, synapses, alreadyTraversedSources)) {
				return true;
			}
		}

		// No path from the given start to end vertices.
		return false;
	}
}

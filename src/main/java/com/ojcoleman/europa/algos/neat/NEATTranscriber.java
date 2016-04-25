package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.integration.BainNeuralNetwork;
import com.ojcoleman.europa.transcribers.nn.integration.BainParametrisedGeneType;
import com.ojcoleman.europa.util.Stringer;

/**
 * Transcriber for the NEAT algorithm.
 * 
 * @author O. J. Coleman
 */
public class NEATTranscriber extends NeuralNetworkTranscriber<NEATGenotype> {
	@Prototype(description = "The neural network prototype.", defaultClass = BainNeuralNetwork.class)
	protected ParametrisedNeuralNetwork neuralNetwork;

	public NEATTranscriber(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ojcoleman.europa.core.Transcriber#transcribe(com.ojcoleman.europa.core.Genotype,
	 * com.ojcoleman.europa.core.Function)
	 */
	@Override
	public VectorFunction transcribe(NEATGenotype genotype, VectorFunction function) {
		List<NEATAllele<?>> neuronTypes = genotype.getAllelesOfType(NNPart.NEURON_TYPE, null);
		List<NEATAllele<?>> synapseTypes = genotype.getAllelesOfType(NNPart.SYNAPSE_TYPE, null);

		Collection<NEATNeuronAllele> neuronAlleles = genotype.getNeurons().values();
		Collection<NEATSynapseAllele> remainingConnAlleles = genotype.getSynapses().values();

		if (genotype.getAllelesOfType(NNPart.NEURON_INPUT).size() > 2)
			System.out.println(genotype.getAllelesOfType(NNPart.NEURON_INPUT).size());

		// Build the neural network.
		NNInstanceConfig nnConfig = new NNInstanceConfig(neuronAlleles.size(), remainingConnAlleles.size(), genotype.getAllelesOfType(NNPart.NEURON_INPUT).size(), genotype.getAllelesOfType(NNPart.NEURON_OUTPUT).size(), "NN-" + genotype.id);

		// new BainNeuralNetwork(null, nnConfig);
		ParametrisedNeuralNetwork nnWrapper = this.neuralNetwork.newInstance(nnConfig);

		// Add neuron types.
		if (neuronTypes != null) {
			// Map from genotype neuron type reference to neuron type index in ParametrisedNeuralNetwork.
			int[] neuronTypeReftoIndex = new int[neuronTypes.size()];

			for (int ref = 0; ref < neuronTypes.size(); ref++) {
				NEATAllele<?> allele = neuronTypes.get(ref);
				int index = nnWrapper.addNeuronType(allele.getAllValuesAsMap());
				neuronTypeReftoIndex[ref] = index;
			}
		}

		// Add synapse types.
		if (synapseTypes != null) {
			// Map from genotype synapse type reference to synapse type index in ParametrisedNeuralNetwork.
			int[] synapseTypeReftoIndex = new int[synapseTypes.size()];

			for (int ref = 0; ref < synapseTypes.size(); ref++) {
				NEATAllele<?> allele = synapseTypes.get(ref);
				int index = nnWrapper.addNeuronType(allele.getAllValuesAsMap());
				synapseTypeReftoIndex[ref] = index;
			}
		}

		// Add neurons.
		// Map from NEAT innovation ID to neuron index in ParametrisedNeuralNetwork.
		Map<Long, Integer> neuronIDtoIndex = new HashMap<Long, Integer>();
		Map<String, Double> neuronParams = new HashMap<String, Double>(); // Re-usable param label, value map.
		for (NEATNeuronAllele neuronAllele : neuronAlleles) {
			// If this neuron is enabled (should be expressed).
			if (neuronAllele.enabled()) {
				neuronAllele.getAllValuesAsMap(neuronParams);
				// System.out.println("\n\nneuronParams:\n" + Stringer.toString(neuronParams, 1000, 1));
				int index = nnWrapper.addNeuron(neuronParams);
				neuronIDtoIndex.put(neuronAllele.gene.id, index);
			}
		}

		// Synapses.
		Map<String, Double> synapseParams = new HashMap<String, Double>(); // Re-usable param label:value map.
		for (NEATSynapseAllele synapseAllele : remainingConnAlleles) {
			long sourceID = synapseAllele.gene.sourceID;
			long destID = synapseAllele.gene.destinationID;

			// If this synapse is enabled, and the source and destination neurons are enabled.
			if (synapseAllele.enabled() && neuronIDtoIndex.containsKey(sourceID) && neuronIDtoIndex.containsKey(destID)) {
				int srcIdx = neuronIDtoIndex.get(sourceID);
				int destIdx = neuronIDtoIndex.get(destID);

				synapseAllele.getAllValuesAsMap(synapseParams);
				// System.out.println("\n\nsynapseParams:\n" + Stringer.toString(synapseParams, 1000, 1));
				nnWrapper.addSynapse(synapseParams, srcIdx, destIdx);
			}
		}

		nnWrapper.finishedBuilding();

		return nnWrapper;
	}

	@Override
	public ParametrisedNeuralNetwork getNeuralNetworkPrototype() {
		return neuralNetwork;
	}

	@Override
	public NEATGenotype getTemplateGenotype() {
		Run run = this.getParentComponent(Run.class);

		SortedSet<NEATAllele<?>> alleles = new TreeSet<NEATAllele<?>>();

		ParametrisedGeneType neuronConfig = neuralNetwork.getConfig().neuron();
		ParametrisedGeneType synapseConfig = neuralNetwork.getConfig().synapse();

		// Add genes and alleles for neuron and synapse types first as applicable.
		if (neuronConfig.paramsTypeCount > 0) {
			for (int gi = 0; gi < neuronConfig.paramsTypeCount; gi++) {
				NEATGene gene = genotype.genePrototype.newInstance(Gene.typeSet(NNPart.NEURON_TYPE), Vector.EMPTY);
				NEATAllele<NEATGene> allele = genotype.allelePrototype.newInstance(gene, neuronConfig.createTypeVector());
				alleles.add(allele);
			}
		}
		if (synapseConfig.paramsTypeCount > 0) {
			for (int gi = 0; gi < synapseConfig.paramsTypeCount; gi++) {
				NEATGene gene = genotype.genePrototype.newInstance(Gene.typeSet(NNPart.SYNAPSE_TYPE), Vector.EMPTY);
				NEATAllele<NEATGene> allele = genotype.allelePrototype.newInstance(gene, synapseConfig.createTypeVector());
				alleles.add(allele);
			}
		}

		// Add genes for input and output neurons.
		for (int gi = 0; gi < getFunctionInputSize(); gi++) {
			// new NEATNeuronGene(genotype.neuronGenePrototype, NNPart.NEURON_INPUT,
			// neuronConfig.createGeneVector(run.random));
			NEATNeuronGene gene = genotype.neuronGenePrototype.newInstance(NNPart.NEURON_INPUT, neuronConfig.createGeneVector(run.random));
			NEATNeuronAllele allele = genotype.neuronAllelePrototype.newInstance(gene, neuronConfig.createAlleleVector());
			alleles.add(allele);
		}
		for (int gi = 0; gi < getFunctionOutputSize(); gi++) {
			NEATNeuronGene gene = genotype.neuronGenePrototype.newInstance(NNPart.NEURON_OUTPUT, neuronConfig.createGeneVector(run.random));
			NEATNeuronAllele allele = genotype.neuronAllelePrototype.newInstance(gene, neuronConfig.createAlleleVector());
			alleles.add(allele);
		}

		// new NEATGenotype(genotype, alleles, new ArrayList<Genotype<?>>());
		return genotype.newInstance(alleles, new ArrayList<Genotype<?>>());
	}
}

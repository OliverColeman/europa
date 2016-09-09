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
import com.ojcoleman.europa.core.Evolver;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuronGeneType;
import com.ojcoleman.europa.transcribers.nn.integration.BainNeuralNetwork;
import com.ojcoleman.europa.transcribers.nn.integration.BainParametrisedNeuronGeneType;
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
		
		// We add in order of input, hidden and output, as prescribed by ParametrisedNeuralNetwork.addNeuron(Map<String, Double> nnConfig)
		List<NEATNeuronAllele> neuronAlleles = new ArrayList<>();
		for (NEATAllele<?> n : genotype.getAllelesOfType(NNPart.NEURON_INPUT)) {
			neuronAlleles.add((NEATNeuronAllele) n);
		}
		for (NEATAllele<?> n : genotype.getAllelesOfType(NNPart.NEURON_HIDDEN)) {
			neuronAlleles.add((NEATNeuronAllele) n);
		}
		for (NEATAllele<?> n : genotype.getAllelesOfType(NNPart.NEURON_OUTPUT)) {
			neuronAlleles.add((NEATNeuronAllele) n);
		}
		
		Collection<NEATSynapseAllele> synapseAlleles = genotype.getSynapses().values();
		
		// Count enabled neurons.
		int neuronCount = 0;
		for (NEATNeuronAllele neuronAllele : neuronAlleles) {
			// If this neuron is enabled (should be expressed).
			if (neuronAllele.enabled()) {
				neuronCount++;
			}
		}

		// Count enabled synapses.
		int synapseCount = 0;
		for (NEATSynapseAllele synapseAllele : synapseAlleles) {
			long sourceID = synapseAllele.gene.sourceID;
			long destID = synapseAllele.gene.destinationID;

			// If this synapse is enabled, and the source and destination neurons are enabled.
			if (synapseAllele.enabled() && genotype.getAllele(sourceID).enabled() && genotype.getAllele(destID).enabled()) {
				synapseCount++;
			}
		}
		
		// Build the neural network.
		NNInstanceConfig nnConfig = new NNInstanceConfig(neuronCount, synapseCount, genotype.getAllelesOfType(NNPart.NEURON_INPUT).size(), genotype.getAllelesOfType(NNPart.NEURON_OUTPUT).size(), "NN-" + genotype.id);

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
		
		String dbg = "";
		try {
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
			
			dbg += Stringer.toString(neuronIDtoIndex) + "\n";
			
			// Synapses.
			Map<String, Double> synapseParams = new HashMap<String, Double>(); // Re-usable param label:value map.
			for (NEATSynapseAllele synapseAllele : synapseAlleles) {
				long sourceID = synapseAllele.gene.sourceID;
				long destID = synapseAllele.gene.destinationID;
				
				dbg += "\n" + sourceID + " => " + destID;
	
				// If this synapse is enabled, and the source and destination neurons are enabled.
				if (synapseAllele.enabled() && genotype.getAllele(sourceID).enabled() && genotype.getAllele(destID).enabled()) {
					int srcIdx = neuronIDtoIndex.get(sourceID);
					int destIdx = neuronIDtoIndex.get(destID);
					
					dbg += "  (" + srcIdx + " => " + destIdx + ")\n";
	
					synapseAllele.getAllValuesAsMap(synapseParams);
					// System.out.println("\n\nsynapseParams:\n" + Stringer.toString(synapseParams, 1000, 1));
					nnWrapper.addSynapse(synapseParams, srcIdx, destIdx);
				}
				else {
					dbg += " disbaled\n";
				}
			}

			nnWrapper.finishedBuilding();
		}
		catch (Exception ex) {
			System.out.println(Stringer.toString(genotype, 10));
			System.out.println(Stringer.toString(nnWrapper, 10));
			System.out.println(dbg);
			throw ex;
		}

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

		ParametrisedNeuronGeneType neuronConfig = neuralNetwork.getConfig().neuron();
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
		long[] inputIDs = new long[getFunctionInputSize()];
		for (int gi = 0; gi < getFunctionInputSize(); gi++) {
			// new NEATNeuronGene(genotype.neuronGenePrototype, NNPart.NEURON_INPUT,
			// neuronConfig.createGeneVector(run.random));
			NEATNeuronGene gene = genotype.neuronGenePrototype.newInstance(NNPart.NEURON_INPUT, neuronConfig.createInputNeuronGeneVector(run.random));
			NEATNeuronAllele allele = genotype.neuronAllelePrototype.newInstance(gene, neuronConfig.createAlleleVector());
			alleles.add(allele);
			inputIDs[gi] = gene.id;
		}
		long[] outputIDs = new long[getFunctionOutputSize()];
		for (int gi = 0; gi < getFunctionOutputSize(); gi++) {
			NEATNeuronGene gene = genotype.neuronGenePrototype.newInstance(NNPart.NEURON_OUTPUT, neuronConfig.createOutputNeuronGeneVector(run.random));
			NEATNeuronAllele allele = genotype.neuronAllelePrototype.newInstance(gene, neuronConfig.createAlleleVector());
			alleles.add(allele);
			outputIDs[gi] = gene.id;
		}
		
		// Add synapses between input and output neurons.
		NEATEvolver evolver = (NEATEvolver) this.population.getEvoler();
		for (int i = 0; i < getFunctionInputSize(); i++) {
			for (int o = 0; o < getFunctionOutputSize(); o++) {
				// We use the method on NEATEvolver as evolver keeps a record of all synapses created between any two neurons.
				// The record allows reusing the gene from an existing synapse between the same two neurons (assuming the gene
				// parameter values are the same, if applicable).
				NEATSynapseAllele allele = evolver.newSynapseAllele(genotype, inputIDs[i], outputIDs[o]);
				alleles.add(allele);
			}
		}
		
		// new NEATGenotype(genotype, alleles, new ArrayList<Genotype<?>>());
		return genotype.newInstance(alleles, new ArrayList<Genotype<?>>());
	}
}

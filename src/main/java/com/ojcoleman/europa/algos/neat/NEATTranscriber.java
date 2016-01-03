package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;

/**
 * Transcriber for the NEAT algorithm.
 * 
 * @author O. J. Coleman
 */
public abstract class NEATTranscriber extends NeuralNetworkTranscriber<NEATGenotype> {
	public NEATTranscriber(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.core.Transcriber#transcribe(com.ojcoleman.europa.core.Genotype, com.ojcoleman.europa.core.Function)
	 */
	@Override
	public VectorFunction transcribe(NEATGenotype genotype, VectorFunction function) {
		List<NEATAllele<?>> neuronTypes = genotype.getAllelesOfType(NNPart.NEURON_TYPE, null);
		List<NEATAllele<?>> synapseTypes = genotype.getAllelesOfType(NNPart.SYNAPSE_TYPE, null);
		
		Collection<NEATNeuronAllele> neuronAlleles = genotype.getNeurons().values();
		Collection<NEATSynapseAllele> remainingConnAlleles = genotype.getSynapses().values();
		
		// Build the neural network.
		NNInstanceConfig nnConfig = new NNInstanceConfig(neuronAlleles.size(), remainingConnAlleles.size(), genotype.getAllelesOfType(NNPart.NEURON_INPUT).size(), genotype.getAllelesOfType(NNPart.NEURON_OUTPUT).size(), "NN-"+genotype.id);
		
		// Add neuron types.
		int[] neuronTypeReftoIndex = new int[neuronTypes.size()]; // Map from genotype neuron type reference to neuron type index in NNWrapper.
		if (neuronTypes != null){
			for (int ref = 0; ref < neuronTypes.size(); ref++) {
				NEATAllele<?> allele = neuronTypes.get(ref);
				int index = nnWrapper.addNeuronType(allele.getAllValuesAsMap());
				neuronTypeReftoIndex[ref] = index;
			}
		}
		
		// Add synapse types.
		int[] synapseTypeReftoIndex = new int[synapseTypes.size()]; // Map from genotype synapse type reference to synapse type index in NNWrapper.
		if (synapseTypes != null){
			for (int ref = 0; ref < synapseTypes.size(); ref++) {
				NEATAllele<?> allele = synapseTypes.get(ref);
				int index = nnWrapper.addNeuronType(allele.getAllValuesAsMap());
				synapseTypeReftoIndex[ref] = index;
			}
		}
		
		// Add neurons.
		Map<Long, Integer> neuronIDtoIndex = new HashMap<Long, Integer>(); // Map from NEAT innovation ID to neuron index in NNWrapper.
		Map<String, Double> neuronParams = new HashMap<String, Double>(); // Re-usable param label, value map.
		for (NEATNeuronAllele neuronAllele : neuronAlleles) {
			int index = nnWrapper.addNeuron(neuronAllele.getAllValuesAsMap(neuronParams));
			neuronIDtoIndex.put(neuronAllele.gene.id, index);
		}
		
		// Connections.
		Map<String, Double> synapseParams = new HashMap<String, Double>(); // Re-usable param label, value map.
		for (NEATSynapseAllele connAllele : remainingConnAlleles) {
			int src = neuronIDtoIndex.get(connAllele.gene.sourceID);
			int dest = neuronIDtoIndex.get(connAllele.gene.destinationID);
			
			nnWrapper.addSynapse(connAllele.getAllValuesAsMap(synapseParams), src, dest);
		}
		
		return nnWrapper;
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.core.Transcriber#getTemplateGenotype()
	 */
	@Override
	public NEATGenotype getTemplateGenotype() {
		Run run = this.getParentComponent(Run.class);
		
		SortedSet<NEATAllele<?>> alleles = new TreeSet<NEATAllele<?>>();
		
		// Add genes and alleles for neuron and synapse types first as applicable.
		if (config.neuron().paramsTypeCount > 0) {
			for (int gi = 0; gi < config.neuron().paramsTypeCount; gi++) {
				NEATGene gene = new NEATGene(Gene.typeSet(NNPart.NEURON_TYPE), run.getNextID(), Vector.EMPTY);
				NEATAllele<?> allele = new NEATAllele<NEATGene>(gene, config.neuron().createTypeVector());
				alleles.add(allele);
			}
		}
		if (config.synapse().paramsTypeCount > 0) {
			for (int gi = 0; gi < config.synapse().paramsTypeCount; gi++) {
				NEATGene gene = new NEATGene(Gene.typeSet(NNPart.SYNAPSE_TYPE), run.getNextID(), Vector.EMPTY);
				NEATAllele<?> allele = new NEATAllele<NEATGene>(gene, config.synapse().createTypeVector());
				alleles.add(allele);
			}
		}
		
		// Add genes for input and output neurons.
		for (int gi = 0; gi < functionInputSize; gi++) {
			NEATNeuronGene gene = new NEATNeuronGene(NNPart.NEURON_INPUT, run.getNextID(), config.neuron().createGeneVector());
			NEATAllele<?> allele = new NEATNeuronAllele(gene, config.neuron().createAlleleVector());
			alleles.add(allele);
		}
		for (int gi = 0; gi < functionOutputSize; gi++) {
			NEATNeuronGene gene = new NEATNeuronGene(NNPart.NEURON_OUTPUT, run.getNextID(), config.neuron().createGeneVector());
			NEATAllele<?> allele = new NEATNeuronAllele(gene, config.neuron().createAlleleVector());
			alleles.add(allele);
		}
		
		return new NEATGenotype(run.getNextID(), alleles);
	}
}

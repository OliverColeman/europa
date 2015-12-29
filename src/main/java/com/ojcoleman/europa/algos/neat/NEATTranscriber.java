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
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.util.VectorInfo;

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
		List<NEATAllele<?>> neuronTypes = genotype.getAllelesOfType("neuronType", null);
		List<NEATAllele<?>> synapseTypes = genotype.getAllelesOfType("synapseType", null);
		
		List<NEATAllele<?>> inputNeuronAlleles = genotype.getAllelesOfType("neuronInput");
		List<NEATAllele<?>> outputNeuronAlleles = genotype.getAllelesOfType("neuronOutput");
		
		// Collect together all neuron alleles, with input first, hidden next, and output last (this is the order than Bain networks should be in).
		List<NEATAllele<?>> neuronAlleles = new LinkedList<NEATAllele<?>>();
		neuronAlleles.addAll(inputNeuronAlleles);
		if (genotype.hasAllelesOfType("neuronHidden")) {
			neuronAlleles.addAll(genotype.getAllelesOfType("neuronHidden"));
		}
		neuronAlleles.addAll(outputNeuronAlleles);
		
		// Get all connection alleles.
		List<NEATAllele<?>> remainingConnAlleles = genotype.getAllelesOfType(NNPart.SYNAPSE, new LinkedList<NEATAllele<?>>());
		
		// Build the neural network.
		NNInstanceConfig nnConfig = new NNInstanceConfig(neuronAlleles.size(), remainingConnAlleles.size(), inputNeuronAlleles.size(), outputNeuronAlleles.size(), "NN-"+genotype.id);
		
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
		Collection<Long> outputInnoIDs = new ArrayList<Long>(); // Collect a list of output neuron innovation IDs for later use.
		Map<String, Double> neuronParams = new HashMap<String, Double>(); // Re-usable param label, value map.
		for (NEATAllele<?> neuronAllele : neuronAlleles) {
			int index = nnWrapper.addNeuron(neuronAllele.getAllValuesAsMap(neuronParams));
			neuronIDtoIndex.put(neuronAllele.gene.id, index);

			if (neuronAllele.gene.type == NNPart.NEURON_OUTPUT) {
				outputInnoIDs.add(neuronAllele.gene.id);
			}
		}
		
		// Connections.
		Map<String, Double> synapseParams = new HashMap<String, Double>(); // Re-usable param label, value map.
		for (NEATAllele<?> allele : remainingConnAlleles) {
			NEATConnectionAllele connAllele = (NEATConnectionAllele) allele;
			
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
		if (config.neuronConfig().paramsTypeCount > 0) {
			for (int gi = 0; gi < config.neuronConfig().paramsTypeCount; gi++) {
				NEATGene gene = new NEATGene(NNPart.NEURON_TYPE, run.getNextID(), new VectorInfo());
				NEATAllele<?> allele = new NEATAllele<NEATGene>(gene, config.neuronConfig().getParamsType());
				alleles.add(allele);
			}
		}
		if (config.synapseConfig().paramsTypeCount > 0) {
			for (int gi = 0; gi < config.synapseConfig().paramsTypeCount; gi++) {
				NEATGene gene = new NEATGene(NNPart.SYNAPSE_TYPE, run.getNextID(), new VectorInfo());
				NEATAllele<?> allele = new NEATAllele<NEATGene>(gene, config.synapseConfig().getParamsType());
				alleles.add(allele);
			}
		}
		
		// Add genes for input and output neurons.
		for (int gi = 0; gi < functionInputSize; gi++) {
			NEATGene gene = new NEATGene(NNPart.NEURON_INPUT, run.getNextID(), config.neuronConfig().getParamsGene());
			NEATAllele<?> allele = new NEATNeuronAllele(gene, config.neuronConfig().getParamsAllele());
			alleles.add(allele);
		}
		for (int gi = 0; gi < functionOutputSize; gi++) {
			NEATGene gene = new NEATGene(NNPart.NEURON_OUTPUT, run.getNextID(), config.neuronConfig().getParamsGene());
			NEATAllele<?> allele = new NEATNeuronAllele(gene, config.neuronConfig().getParamsAllele());
			alleles.add(allele);
		}
		
		return new NEATGenotype(run.getNextID(), alleles);
	}
}

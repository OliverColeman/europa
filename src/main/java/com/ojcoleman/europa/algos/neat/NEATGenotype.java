package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ojcoleman.europa.algos.neat.NEATAllele;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * @author O. J. Coleman
 *
 */
public class NEATGenotype extends Genotype<NEATAllele<?>> {
	/**
	 * A map view of the neurons in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */
	SortedMap<Long, NEATNeuronAllele> neurons;
	
	/**
	 * A map view of the synapses in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */
	SortedMap<Long, NEATSynapseAllele> synapses;
	
	@SafeVarargs
	public NEATGenotype(long id, SortedSet<NEATAllele<?>> alleles, Genotype<?>... parents) {
		super(id, alleles, parents);
		
		neurons = new TreeMap<Long, NEATNeuronAllele>();
		for (NEATAllele<?> allele : getAllelesOfType(NNPart.NEURON)) {
			neurons.put(allele.gene.id, (NEATNeuronAllele) allele);
		}
		
		synapses = new TreeMap<Long, NEATSynapseAllele>();
		for (NEATAllele<?> allele : getAllelesOfType(NNPart.SYNAPSE, new LinkedList<NEATAllele<?>>())) {
			synapses.put(allele.gene.id, (NEATSynapseAllele) allele);
		}
		
	}

	@Override
	public Genotype<NEATAllele<?>> create(long id, Collection<NEATAllele<?>> alleles, Genotype<?>... parents) {
		if (alleles instanceof SortedSet) {
			return new NEATGenotype(id, (SortedSet<NEATAllele<?>>) alleles, parents);
		}
		return new NEATGenotype(id, new TreeSet<NEATAllele<?>>(alleles), parents);
	
	}
	
	@Override
	public void addAllele(NEATAllele<?> allele) {
		super.addAllele(allele);
		if (allele.gene.types.contains(NNPart.NEURON)) {
			neurons.put(allele.gene.id, (NEATNeuronAllele) allele);
		}
		else if (allele.gene.types.contains(NNPart.SYNAPSE)) {
			synapses.put(allele.gene.id, (NEATSynapseAllele) allele);
		}
	}
	
	/**
	 * Get a map view of the neurons in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */ 
	public SortedMap<Long, NEATNeuronAllele> getNeurons() {
		return Collections.unmodifiableSortedMap(neurons);
	}
	
	/**
	 * Get a map view of the synapses in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */ 
	public SortedMap<Long, NEATSynapseAllele> getSynapses() {
		return Collections.unmodifiableSortedMap(synapses);
	}
}

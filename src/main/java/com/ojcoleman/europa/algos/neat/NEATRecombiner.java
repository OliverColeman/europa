package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Recombiner;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.Topology;
import com.ojcoleman.europa.util.ArrayUtil;

/**
 * Implements recombination as described by the NEAT evolutionary algorithm (See
 * https://www.cs.ucf.edu/~kstanley/neat.html) but generalised to any number of parents: if one parent dominates all
 * others then the new genotype will inherit the genes of this parent, otherwise the genes from all parents will be
 * inherited (except in the case where adding a synapse would result in a cycle and recurrent networks are not allowed).
 * The values of the alleles of inherited genes are based on the values of the alleles of all parents that possess that
 * gene.
 * 
 * @author O. J. Coleman
 */
public class NEATRecombiner extends Recombiner<NEATGenotype> {
	public NEATRecombiner(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ojcoleman.europa.core.Recombiner#recombine(com.ojcoleman.europa.core.Genotype[])
	 */
	@Override
	public NEATGenotype recombine(List<NEATGenotype> parents) {
		Run run = this.getParentComponent(Run.class);
		NEATEvolver evolver = this.getParentComponent(NEATEvolver.class);
		NNConfig nnConfig = ((NeuralNetworkTranscriber<?>) run.getTranscriber()).getNeuralNetworkPrototype().getConfig();
		Random random = run.random;
		Population<NEATGenotype, ?> population = this.getParentComponent(Population.class);

		NEATGenotype child = null;

		SortedMap<Double, NEATGenotype> parentsRanker = new TreeMap<>();
		List<Genotype<?>> genericParents = new ArrayList<>(parents.size());
		for (NEATGenotype parent : parents) {
			parentsRanker.put(population.getIndividual(parent.id).getRank(), parent);
			genericParents.add(parent);
		}
		List<Double> ranks = new ArrayList<>(parentsRanker.keySet());
		List<NEATGenotype> parentsRanked = new ArrayList<>(parentsRanker.values());
		
		// If one parent dominates (all the) other(s).
		if (ranks.get(0) > ranks.get(1)) {
			// Child inherits all structure/genes from dominant parent.
			//new NEATGenotype(parentsRanked.get(0), parentsRanked.get(0).getAlleles(), genericParents);
			child = parentsRanked.get(0).newInstance(parentsRanked.get(0).getAlleles(), genericParents);

			// Set values for each allele in the new genotype based on all values for same allele in all parents with
			// this gene/allele.
			for (NEATAllele<?> childAllele : child.getAlleles()) {
				// Get the matching alleles from each parent with this gene/allele.
				List<NEATAllele<?>> alleleParents = new ArrayList<>(parentsRanked.size());
				for (NEATGenotype parent : parentsRanked) {
					NEATAllele<?> otherAllele = parent.getAllele(childAllele.gene.id);
					if (otherAllele != null) {
						alleleParents.add(otherAllele);
					}
				}

				// Set enabled by randomly picking enabled value from one of the parents with this gene.
				childAllele.setEnabled(alleleParents.get(random.nextInt(alleleParents.size())).enabled());

				// Set the values from one or more of the parents.
				setValues(childAllele, random, alleleParents);
			}
		} else {
			// Highest ranked parent doesn't dominate next highest ranked.
			// Child inherits structure/genes from all parents.

			// Create an empty genotype (genes will be added one by one).
			//new NEATGenotype(parentsRanked.get(0), new ArrayList<NEATAllele<?>>(), genericParents);
			child = parentsRanked.get(0).newInstance(new ArrayList<NEATAllele<?>>(), genericParents); 
			
			// Get a list of all gene IDs across all parents, with associated alleles from each parent.
			TreeMultimap<Long, NEATAllele<?>> allAlleles = TreeMultimap.create();
			for (NEATGenotype parent : parents) {
				for (NEATAllele<?> allele : parent.getAlleles()) {
					allAlleles.put(allele.gene.id, allele);
				}
			}

			// Keep a convenient record of the synapses added. This allows checking if adding a synapse would create a
			// cycle (if applicable).
			Table<Long, Long, Boolean> synapses = HashBasedTable.create();

			// For each gene and set of alleles from each parent for that gene.
			for (Entry<Long, Collection<NEATAllele<?>>> geneIDAlleles : allAlleles.asMap().entrySet()) {
				// Get the gene (from one of the parents, doesn't matter which).
				NEATGene gene = geneIDAlleles.getValue().iterator().next().gene;

				// If this is a synapse gene and recurrent connections are not allowed,
				// make sure it won't create a cycle.
				boolean okayToAdd = true;
				if (gene instanceof NEATSynapseGene && nnConfig.getTopology() != Topology.RECURRENT) {
					NEATSynapseGene synapseGene = (NEATSynapseGene) gene;
					okayToAdd = !evolver.synapseWouldCreateCycle(synapseGene.sourceID, synapseGene.destinationID, synapses);

					if (okayToAdd) {
						// Add synapse to the record of synapses.
						synapses.put(synapseGene.sourceID, synapseGene.destinationID, Boolean.TRUE);
					}
				}

				if (okayToAdd) {
					// Get alleles from all parents with this gene as a list.
					List<NEATAllele<?>> alleleParents = new ArrayList<NEATAllele<?>>(geneIDAlleles.getValue());

					// Create a new allele that is a copy of one of the alleles from the parents.
					NEATAllele<?> newAllele = (NEATAllele<?>) alleleParents.get(0).newInstance();

					// Set enabled by randomly picking enabled value from one of the parents.
					newAllele.setEnabled(alleleParents.get(random.nextInt(alleleParents.size())).enabled());

					// Set the values from one or more of the parents.
					setValues(newAllele, random, alleleParents);

					// Add it to child.
					child.addAllele(newAllele);
				}
			}
		}

		return child;
	}

	private void setValues(NEATAllele<?> childAllele, Random random, List<NEATAllele<?>> alleleParents) {
		// Set values based on a randomly selected method.
		if (random.nextBoolean()) {
			// Use values from just one of the parents.
			NEATAllele<?> parentAllele = alleleParents.get(random.nextInt(alleleParents.size()));
			childAllele.vector.setValues(parentAllele.vector);
		} else {
			// Use value somewhere between those from all parents. Each parent is
			// assigned a weight such that the summed weights over all parents equals one.
			double parentWeight[] = ArrayUtil.normaliseSum(ArrayUtil.newRandom(alleleParents.size(), random));
			for (int valIndex = 0; valIndex < childAllele.vector.size(); valIndex++) {
				double value = 0;
				for (int p = 0; p < alleleParents.size(); p++) {
					value += alleleParents.get(p).vector.get(valIndex) * parentWeight[p];
				}
				childAllele.vector.set(valIndex, value);
			}
		}
	}

	@Override
	public int parentCountMaximum() {
		return 2;
	}
}

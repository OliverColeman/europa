package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Recombiner;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.transcribers.nn.NeuralNetworkTranscriber;
import com.ojcoleman.europa.transcribers.nn.Topology;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.util.Stringer;

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
		
		//boolean dbg = Math.random() < 0.0001;
		
		List<Individual<NEATGenotype, ?>> parentsRanked = new ArrayList<>(parents.size());
		List<Genotype<?>> genericParents = new ArrayList<>(parents.size());
		for (NEATGenotype parentGenotype : parents) {
			parentsRanked.add(population.getIndividual(parentGenotype.id));
			genericParents.add(parentGenotype);
		}
		Collections.sort(parentsRanked);
		Collections.reverse(parentsRanked);
		
		//if (dbg) System.out.println(Stringer.toString(parentsRanked.get(0).evaluationData.getFitnessResults()));
		//if (dbg) System.out.println(Stringer.toString(parentsRanked.get(1).evaluationData.getFitnessResults()));
		
		// Keep a convenient record of the synapses added. This allows checking if adding a synapse would create a
		// cycle (if applicable).
		Table<Long, Long, Boolean> synapses = HashBasedTable.create();
		
		boolean firstParentIsFitter = !parentsRanked.get(0).evaluationData.equals(parentsRanked.get(1).evaluationData);
		
		// Create an empty genotype (genes will be added one by one).
		// new NEATGenotype(parentsRanked.get(0).genotype, new ArrayList<NEATAllele<?>>(), genericParents);
		child = parentsRanked.get(0).genotype.newInstance(new ArrayList<NEATAllele<?>>(), genericParents);

		// Get a list of all gene IDs across all parents, with associated alleles from each parent.
		TreeMultimap<Long, NEATAllele<?>> allAlleles = TreeMultimap.create();
		for (NEATGenotype parent : parents) {
			for (NEATAllele<?> allele : parent.getAlleles()) {
				allAlleles.put(allele.gene.id, allele);
			}
		}
		
		// For each gene and set of alleles from each parent for that gene.
		for (Entry<Long, Collection<NEATAllele<?>>> geneIDAlleles : allAlleles.asMap().entrySet()) {
			// Get the gene (from one of the parents, doesn't matter which).
			NEATGene gene = geneIDAlleles.getValue().iterator().next().gene;
			
			boolean include;
			// If the first parent is fitter, then just include genes from it.
			if (firstParentIsFitter) {
				include = parentsRanked.get(0).genotype.getAllele(gene.id) != null;
				
				if (!include && gene.id == 66) {
					System.err.println("firstParentIsFitter");
				}
			} else {
				// Otherwise include a gene if all parents include it, or with 50% chance otherwise.
				include = geneIDAlleles.getValue().size() == parents.size() || random.nextBoolean();

				if (!include && gene.id == 66) {
					System.err.println("!firstParentIsFitter");
				}
			}
			
			
			if (include) {
				// For synapse genes, check for source and dest neurons, and cycles if applicable.
				boolean okayToAdd = true;
				if (gene instanceof NEATSynapseGene) {
					NEATSynapseGene synapseGene = (NEATSynapseGene) gene;
					
					// Make sure the source and target neurons have been added.
					// The source and dest neurons should be added first as their gene ID will be lower, so it's safe to assume they've already been added.
					okayToAdd = child.hasGene(synapseGene.sourceID) && child.hasGene(synapseGene.destinationID);
					
					// Make sure a synapse with same source and dest (but with different parameters) doesn't already exist.
					if (okayToAdd) {
						okayToAdd = !synapses.contains(synapseGene.sourceID, synapseGene.destinationID);
					}
					
					// If recurrent connections are not allowed, make sure it won't create a cycle.
					if (okayToAdd && nnConfig.getTopology() != Topology.RECURRENT) {
						okayToAdd = !evolver.synapseWouldCreateCycle(synapseGene.sourceID, synapseGene.destinationID, synapses);
	
						if (okayToAdd) {
							// Add synapse to the record of synapses.
							synapses.put(synapseGene.sourceID, synapseGene.destinationID, Boolean.TRUE);
						}
					}
				}
				
				if (okayToAdd) {
					// Get alleles from all parents with this gene as a list.
					List<NEATAllele<?>> alleleParents = new ArrayList<NEATAllele<?>>(geneIDAlleles.getValue());

					// Create a new allele that is a copy of one of the alleles from the parents.
					NEATAllele<?> newAllele = (NEATAllele<?>) alleleParents.get(0).newInstance();

					if (gene instanceof NEATSynapseGene) {
						// Set enabled randomly 2% of the time.
						//if (random.nextInt(50) == 0) {
						//	newAllele.setEnabled(random.nextBoolean());
						//}
						//else {
							// From original paper: "There was a 75% chance that an inherited gene was disabled if it was disabled in either parent."
							boolean disabledInAParent = false;
							boolean disabledInAllParents = true;
							for (NEATAllele<?> ap :  alleleParents) {
								disabledInAParent |= !ap.enabled;
								disabledInAllParents &= !ap.enabled;
							}
							// Enable if it's not disabled in any parent OR disable if it's disabled in all parents, 
							// otherwise it's disabled in some but not all parents so enable with 25% probability.
							boolean enable = !disabledInAParent || (!disabledInAllParents && random.nextDouble() < 0.25);
							newAllele.setEnabled(enable);
						//}
					}
					
					// Set the values from one or more of the parents.
					setValues(newAllele, random, alleleParents);

					// Add it to child.
					child.addAllele(newAllele);
				}
			}
		}
		
		//if (dbg) System.exit(0);
		
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

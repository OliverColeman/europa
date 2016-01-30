package com.ojcoleman.europa.algos.neat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.neat.NEATAllele;
import com.ojcoleman.europa.algos.vector.VectorGeneGenotype;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Genotype for the NEAT algorithm.
 * 
 * @author O. J. Coleman
 */
public class NEATGenotype extends VectorGeneGenotype<NEATAllele<?>> {
	@Prototype(description = "The prototype configuration for the NEATGene (used only for the \"type\" system, see paramsType in the neuron and synapse configs). This is only useful if a custom sub-class of NEATGene is to be used.")
	protected NEATGene genePrototype;

	@Prototype(description = "The prototype configuration for the NEATAllele (used only for the \"type\" system, see paramsType in the neuron and synapse configs). This is only useful if a custom sub-class of NEATAllele is to be used.")
	protected NEATAllele<NEATGene> allelePrototype;

	@Prototype(description = "The prototype configuration for the NEATNeuronGene. This is only useful if a custom sub-class of NEATNeuronGene is to be used.")
	protected NEATNeuronGene neuronGenePrototype;

	@Prototype(description = "The prototype configuration for the NEATNeuronAllele. This is only useful if a custom sub-class of NEATNeuronAllele is to be used.")
	protected NEATNeuronAllele neuronAllelePrototype;

	@Prototype(description = "The prototype configuration for the NEATSynapseGene. This is only useful if a custom sub-class of NEATSynapseGene is to be used.")
	protected NEATSynapseGene synapseGenePrototype;

	@Prototype(description = "The prototype configuration for the NEATSynapseAllele. This is only useful if a custom sub-class of NEATSynapseAllele is to be used.")
	protected NEATSynapseAllele synapseAllelePrototype;

	/**
	 * A map view of all alleles in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */
	Map<Long, NEATAllele<?>> allAlleles;

	/**
	 * A map view of the neurons in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */
	SortedMap<Long, NEATNeuronAllele> neurons;

	/**
	 * A map view of the synapses in this genotype, mapping from NEAT innovation ID ({@link NEATGene#id}) to allele.
	 */
	SortedMap<Long, NEATSynapseAllele> synapses;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public NEATGenotype(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a
	 * Genotype with the same alleles and genes as the given genotype. The parent is set to the given prototype.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 */
	public NEATGenotype(NEATGenotype prototype) {
		super(prototype);

		init();
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a
	 * Genotype with the given alleles and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. A copy of each allele is made
	 *            and put into a new collection. If the copy should be a clone then {@link #getAlleles()} may be passed.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	public NEATGenotype(NEATGenotype prototype, Collection<NEATAllele<?>> alleles, List<Genotype<?>> parents) {
		super(prototype, alleles, parents);

		init();
	}

	private void init() {
		allAlleles = new HashMap<>();
		for (NEATAllele<?> allele : alleles) {
			allAlleles.put(allele.gene.id, (NEATNeuronAllele) allele);
		}

		neurons = new TreeMap<>();
		for (NEATAllele<?> allele : getAllelesOfType(NNPart.NEURON)) {
			neurons.put(allele.gene.id, (NEATNeuronAllele) allele);
		}

		synapses = new TreeMap<>();
		for (NEATAllele<?> allele : getAllelesOfType(NNPart.SYNAPSE, new LinkedList<NEATAllele<?>>())) {
			synapses.put(allele.gene.id, (NEATSynapseAllele) allele);
		}
	}

	@Override
	public void addAllele(NEATAllele<?> allele) {
		super.addAllele(allele);
		if (allele.gene.types.contains(NNPart.NEURON)) {
			neurons.put(allele.gene.id, (NEATNeuronAllele) allele);
		} else if (allele.gene.types.contains(NNPart.SYNAPSE)) {
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

	public NEATAllele<?> getAllele(long geneID) {
		return allAlleles.get(geneID);
	}

	@Override
	protected Collection<NEATAllele<?>> newAlleleCollection() {
		return new TreeSet<NEATAllele<?>>();
	}
}

package com.ojcoleman.europa.algos.neat;

import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorAllele;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * <p>
 * Base class for representations of NEAT alleles.
 * </p>
 * <p>
 * <strong>Sub-classes must implement a copy-constructor that accepts a single parameter which is the allele to copy,
 * and which should generally just call <em>super()</em> with the allele to copy.</strong> See
 * {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
 * <p>
 * 
 * @author O. J. Coleman
 */
public class NEATAllele<G extends NEATGene> extends VectorAllele<G> implements Comparable<NEATAllele<G>> {
	/**
	 * When false indicates that the allele/gene pair is disabled in the alleles genotype and should not be expressed in
	 * the phenotype. For a neuron this means the neuron and all its pre and post synapses will not be expressed. For
	 * synapses it means the single synapse will not be expressed.
	 */
	protected boolean enabled = true;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
	 */
	public NEATAllele(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}. Create a
	 * new NEATAllele referencing the same underlying Gene but storing an independent copy of the original parameter
	 * Vector.
	 * 
	 * @param prototype The allele to copy.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public NEATAllele(NEATAllele<G> prototype) {
		super(prototype);
		
		enabled = prototype.enabled;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}. Create a
	 * new NEATAllele with the specified underlying Gene and storing the specified parameter Vector.
	 * 
	 * @param prototype The allele to copy.
	 * @param gene the underlying gene for the new allele.
	 * @param paramVector The parameter vector for the new allele, copied by reference.
	 * 
	 * @throws IllegalArgumentException if the Vector is not set as mutable.
	 */
	public NEATAllele(NEATAllele<G> prototype, G gene, Vector paramVector) {
		super(prototype, gene, paramVector);
	}

	@Override
	public int compareTo(NEATAllele<G> other) {
		// Compare by gene ID first as this is generally what we're interested in for sorting purposes.
		int comparison = gene.compareTo(other.gene);
		
		// If these alleles have the same gene, compare by their own IDs.
		if (comparison == 0) {
			if (id < other.id)
				return -1;
			if (id > other.id)
				return 1;
			return 0;
		}
		
		return comparison;
	}

	/**
	 * Returns true iff this allele/gene pair is enabled in the alleles genotype.
	 * 
	 * @see #enabled
	 */
	public boolean enabled() {
		return enabled;
	}

	/**
	 * Sets whether this allele/gene pair is enabled in the alleles genotype.
	 * 
	 * @see #enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		super.getStringableMap(map);
		map.put("enabled", enabled);
	}
}

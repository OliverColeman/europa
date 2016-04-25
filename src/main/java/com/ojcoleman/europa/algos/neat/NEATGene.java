package com.ojcoleman.europa.algos.neat;

import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorGene;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.configurable.ConfigurableBase;

/**
 * Base class for representations of NEAT genes. Note that the innovation ID required by NEAT is implemented with
 * {@link ConfigurableBase#id} (which this class extends).
 * 
 * @author O. J. Coleman
 */
public class NEATGene extends VectorGene implements Comparable<NEATGene> {
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public NEATGene(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a
	 * new VectorGene with specified type(s) and parameter Vector.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param type The type(s) of the gene, if applicable to the evolutionary algorithm in use. Usually these are enum
	 *            constants. May be empty.
	 * @param paramVector The parameter vector for this gene.
	 */
	public NEATGene(NEATGene prototype, Set<Object> type, Vector paramVector) {
		super(prototype, type, paramVector);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a
	 * new VectorGene with the specified parameter Vector.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param paramVector The parameter vector for this gene.
	 */
	public NEATGene(NEATGene prototype, Vector paramVector) {
		super(prototype, paramVector);
	}

	@Override
	public int compareTo(NEATGene other) {
		if (id < other.id)
			return -1;
		if (id > other.id)
			return 1;
		return 0;
	}
}

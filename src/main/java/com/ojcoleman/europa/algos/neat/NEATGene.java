package com.ojcoleman.europa.algos.neat;

import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorGene;
import com.ojcoleman.europa.configurable.Prototype;

/**
 * Base class for representations of NEAT genes.
 * 
 * @author O. J. Coleman
 */
public class NEATGene extends VectorGene implements Comparable<NEATGene> {
	/**
	 * The NEAT innovation ID.
	 */
	public final long id;

	/**
	 * Prototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public NEATGene(JsonObject config) {
		super(config);
		id = -1;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * VectorGene with specified type(s) and parameter Vector.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param type The type(s) of the gene, if applicable to the evolutionary algorithm in use. Usually these are enum
	 *            constants. May be empty.
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT
	 *            algorithm.
	 * @param paramVector The parameter vector for this gene.
	 */
	public NEATGene(NEATGene prototype, Set<Object> type, long id, Vector paramVector) {
		super(prototype, type, paramVector);
		this.id = id;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a new
	 * VectorGene with the specified parameter Vector.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param id The ID of this Gene within the {@link NEATGenotype}. This is the historical marking used in the NEAT
	 *            algorithm.
	 * @param paramVector The parameter vector for this gene.
	 */
	public NEATGene(NEATGene prototype, long id, Vector paramVector) {
		super(prototype, paramVector);
		this.id = id;
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

package com.ojcoleman.europa.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for representations of genes within a {@link Genotype}. A gene represents a structural part of the
 * genotype. The set of genes in a genotype may be fixed when the initial population is created or they may be added or
 * removed by a {@link Mutator} or {@link Recombiner}. Any values a gene contains should not be mutable; they should be
 * fixed when the gene is added to the genotype.
 */
public abstract class Gene {
	/**
	 * The type(s) of the gene, if applicable to the evolutionary algorithm in use. Usually these are enum constants. May be empty.
	 */
	public final Set<Object> types;
	
	/**
	 * Create a gene of the specified type.
	 * @param type The type(s) of the gene, if applicable to the evolutionary algorithm in use. Usually these are enum constants. May be empty.
	 */ 
	public Gene(Set<Object> types) {
		this.types = types;
	}
	
	/**
	 * Convenience method to create a Set of a list of types.
	 */
	public static Set<Object> typeSet(Object... types) {
		HashSet<Object> set = new HashSet<Object>();
		set.addAll(Arrays.asList(types));
		// Remove "null" type in case this was included in the list somewhere along the line.
		set.remove(null);
		return set;
	}
}

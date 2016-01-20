package com.ojcoleman.europa.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Prototype;

/**
 * <p>
 * Base class for representations of genes within a {@link Genotype}. A Gene represents a structural part of the
 * genotype. The set of Genes in a Genotype may be fixed when the initial population is created or they may be added or
 * removed by a {@link Mutator} or {@link Recombiner}. Any values a Gene contains should not be mutable; they should be
 * fixed when the Gene is created.
 * </p>
 * <p>
 * Note that an Allele belongs to a specific Genotype, but may share the same Gene as Alleles in other Genotypes.
 * Alleles and Genotypes directly reference one another, but a Gene is only referenced by Alleles. A given Gene should
 * only be included once in a Genotype.
 * </p>
 */
public class Gene extends Prototype {
	/**
	 * The type(s) of the gene, if applicable to the evolutionary algorithm in use. Usually these are enum constants.
	 * May be empty. The set is immutable (actually an {@link Collections#unmodifiableSet(Set)}, but this class will
	 * never modify the set).
	 */
	public final Set<Object> types;

	/**
	 * IsPrototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public Gene(JsonObject config) {
		super(config);
		types = Collections.unmodifiableSet(typeSet());
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(IsPrototype)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param type The type(s) of the gene. Usually these are enum constants. May be empty. The given Set is passed
	 *            through {@link Collections#unmodifiableSet(Set)} and then assigned to the {@link #types} field, for
	 *            this reason it should generally not be modified after being passed to this constructor.
	 * 
	 * @see #typeSet(Object...)
	 */
	public Gene(Gene prototype, Set<Object> types) {
		super(prototype);
		this.types = Collections.unmodifiableSet(types);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(IsPrototype)}. Creates a Gene
	 * with no {@link #types}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 */
	public Gene(Gene prototype) {
		super(prototype);
		this.types = Collections.unmodifiableSet(typeSet());
	}

	/**
	 * Convenience method to create a Set of a list of types.
	 */
	public static Set<Object> typeSet(Object... types) {
		HashSet<Object> set = new HashSet<Object>();
		set.addAll(Arrays.asList(types));
		// Remove "null" type in case this was included in the list somewhere along the line.
		set.remove(null);
		return Collections.unmodifiableSet(set);
	}
}

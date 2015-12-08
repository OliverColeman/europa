package com.ojcoleman.europa.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents the inheritable genetic material of an {@link Individual}.
 * 
 * @author O. J. Coleman
 */
public abstract class Genotype<A extends Allele<?>> {
	/**
	 * The parent(s) of this Genotype (as an unmodifiable List).
	 */
	public final List<Genotype<A>> parents;
	
	/**
	 * The alleles in this genotype (as an unmodifiable Set). Each Allele references a {@link Gene}.
	 */
	public final Set<A> alleles;
	
	/**
	 * Create a new Genotype instance. Sub-classes should generally call this constructor from their own constructor(s).
	 * @param parents The parents that were used to create this genotype (this is for record keeping only, implementations of this class
	 * do not need to create new instances from multiple parents (this is the job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public Genotype(Set<A> alleles, Genotype<A>... parents) {
		this.alleles = Collections.unmodifiableSet(alleles);
		this.parents = Collections.unmodifiableList(Arrays.asList(parents));
	}
	 
	 /**
	  * Create and return a deep copy of this Genotype.
	  */
	 public abstract Genotype<A> clone();
}

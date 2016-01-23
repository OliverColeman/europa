package com.ojcoleman.europa.core;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.reflect.TypeToken;
import com.ojcoleman.europa.configurable.Prototype;

/**
 * <p>
 * Represents the inheritable genetic material of an {@link Individual}.
 * </p>
 * <p>
 * <strong>Sub-classes must implement a {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)} type
 * copy-constructor accepting only the allele to copy, which should generally just call <em>super()</em> with the given
 * allele.</strong>
 * </p>
 * <p>
 * Note that an Allele belongs to a specific Genotype, but may share the same Gene as Alleles in other Genotypes.
 * Alleles and Genotypes directly reference one another, but a Gene is only referenced by Alleles. A given Gene should
 * only be included once in a Genotype.
 * </p>
 * 
 * @author O. J. Coleman
 */
public abstract class Genotype<A extends Allele<?>> extends Prototype {
	/**
	 * A unique identifier for this genotype.
	 */
	public final long id;

	/**
	 * The parent(s) of this Genotype (as an unmodifiable List).
	 */
	public final List<Genotype<?>> parents;

	/**
	 * The alleles in this genotype. Each Allele references a {@link Gene}.
	 */
	protected final Collection<A> alleles;

	private final ArrayListMultimap<Object, A> allelesByGeneType;

	/**
	 * IsPrototype constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(JsonObject)}.
	 */
	public Genotype(JsonObject config) {
		super(config);
		id = 0;
		parents = Collections.unmodifiableList(new ArrayList<Genotype<?>>());
		alleles = newAlleleCollection();
		allelesByGeneType = ArrayListMultimap.create();
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a Genotype
	 * with the given ID (should generally be unique) and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param id The ID for the new Genotype.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public Genotype(Genotype<A> prototype, long id, Genotype<?>... parents) {
		super(prototype);
		this.id = id;

		this.alleles = newAlleleCollection();

		if (parents == null) {
			this.parents = Collections.unmodifiableList(new ArrayList<Genotype<?>>());
		} else {
			this.parents = Collections.unmodifiableList(Arrays.asList(parents));
		}

		allelesByGeneType = ArrayListMultimap.create();
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.Prototype#Prototype(Prototype)}. Create a Genotype
	 * with the given ID (should generally be unique), alleles and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param id The ID for the new Genotype.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. A copy of each allele is made
	 *            and put into a new collection.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public Genotype(Genotype<A> prototype, long id, Collection<A> alleles, Genotype<?>... parents) {
		super(prototype);
		this.id = id;

		this.alleles = newAlleleCollection();
		for (A allele : alleles) {
			A newAllele = allele.newInstance();
			this.alleles.add(newAllele);
		}

		if (parents == null) {
			this.parents = Collections.unmodifiableList(new ArrayList<Genotype<?>>());
		} else {
			this.parents = Collections.unmodifiableList(Arrays.asList(parents));
		}

		allelesByGeneType = ArrayListMultimap.create();
		for (A allele : this.alleles) {
			updateAllelesByGeneTypeForAdd(allele);
		}
	}

	/**
	 * Create and return a new Collection to be used to store Alleles. This is used by the Genotype constructors. This
	 * allows sub-classes to specify the kind of Collection to use to store alleles. For example should it be a
	 * {@link List}, a {@link Set}, a {@link SortedSet}, a {@link HashSet}...?
	 */
	protected abstract Collection<A> newAlleleCollection();

	/**
	 * Get the alleles of this genotype as an unmodifiable collection. The iterator of the returned collection will
	 * iterate over the alleles in the same order as the collection provided to the constructor or
	 * {@link #newPrototypeInstance(Class, Object...)}.
	 */
	public Collection<A> getAlleles() {
		return Collections.unmodifiableCollection(alleles);
	}

	/**
	 * Returns true iff this genotype contains alleles for {@link Gene}s of the specified type.
	 * 
	 * @param type Object representing the type, usually an enum constant.
	 */
	public boolean hasAllelesOfType(Object type) {
		return allelesByGeneType.containsKey(type);
	}

	/**
	 * Get the list of alleles in this genotype that are for {@link Gene}s of the specified type. Note that an
	 * allele/gene pair may have multiple types.
	 * 
	 * @param type Object representing the type, usually an enum constant.
	 * @return The list of alleles in this genotype that are for {@link Gene}s of the specified type, in the same order
	 *         as the Set provided to {@link #Genotype(Set, Genotype...)} iterates over those alleles. If no alleles of
	 *         the specified type exist then an empty list is returned.
	 * @see hasAllelesOfType(String)
	 */
	public List<A> getAllelesOfType(Object type) {
		return Collections.unmodifiableList(allelesByGeneType.get(type));
	}

	/**
	 * Get the list of alleles in this genotype that are for {@link Gene}s of the specified type, or a provided default
	 * list if there are no alleles of specified type present. Note that an allele/gene pair may have multiple types.
	 * 
	 * @param type Object representing the type, usually an enum constant.
	 * @param type defaultList A list of default values to return if there are no alleles of specified type present. May
	 *            be null.
	 * @return The list of alleles in this genotype that are for {@link Gene}s of the specified type, in the same order
	 *         as the Set provided to {@link #Genotype(Set, Genotype...)} iterates over those alleles.
	 * @see hasAllelesOfType(String)
	 */
	public List<A> getAllelesOfType(Object type, List<A> defaultList) {
		if (!allelesByGeneType.containsKey(type)) {
			return defaultList;
		}
		return allelesByGeneType.get(type);
	}

	/**
	 * Add the given Allele to this genotype. This method should generally only be called by {@link Mutator}s and
	 * {@link Recombiner}s.
	 * 
	 * @throws IllegalStateException If the given Allele has already been added to a Genotype. It must be removed from
	 *             the Genotype it currently belongs to first by calling {@link #removeAllele(Allele)}.
	 */
	public void addAllele(A allele) {
		alleles.add(allele);
		allele.setGenotype(this);
		updateAllelesByGeneTypeForAdd(allele);
	}

	/**
	 * Remove the given allele from this genotype. This method should generally only be called by {@link Mutator}s and
	 * {@link Recombiner}s.
	 */
	public void removeAllele(A allele) {
		alleles.remove(allele);
		allele.clearGenotype();
		updateAllelesByGeneTypeForRemove(allele);
	}

	private void updateAllelesByGeneTypeForAdd(A allele) {
		if (allele.gene.types != null) {
			for (Object type : allele.gene.types) {
				allelesByGeneType.put(type, allele);
			}
		}
	}

	private void updateAllelesByGeneTypeForRemove(A allele) {
		if (allele.gene.types != null) {
			for (Object type : allele.gene.types) {
				allelesByGeneType.remove(type, allele);
			}
		}
	}
}

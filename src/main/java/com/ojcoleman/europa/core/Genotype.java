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
import java.util.TreeMap;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.reflect.TypeToken;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.util.Stringer;

/**
 * <p>
 * Represents the inheritable genetic material of an {@link Individual}.
 * </p>
 * <p>
 * <strong>Sub-classes must implement a {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}
 * type copy-constructor accepting only the allele to copy, which should generally just call <em>super()</em> with the
 * given allele.</strong>
 * </p>
 * <p>
 * Note that an Allele belongs to a specific Genotype, but may share the same Gene as Alleles in other Genotypes.
 * Alleles and Genotypes directly reference one another, but a Gene is only referenced by Alleles. A given Gene should
 * only be included once in a Genotype.
 * </p>
 * 
 * @author O. J. Coleman
 */
public abstract class Genotype<A extends Allele<?>> extends PrototypeBase implements Stringable {
	/**
	 * The parent(s) of this Genotype (as an unmodifiable List).
	 */
	public final List<Genotype<?>> parents;

	/**
	 * The alleles in this genotype. Each Allele references a {@link Gene}.
	 */
	protected final Collection<A> alleles;

	private ArrayListMultimap<Object, A> allelesByGeneType;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(Configuration)}.
	 */
	public Genotype(Configuration config) {
		super(config);

		parents = Collections.unmodifiableList(new ArrayList<Genotype<?>>());
		alleles = newAlleleCollection();
		allelesByGeneType = ArrayListMultimap.create();
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}. Create a
	 * Genotype with the same alleles and genes as the given genotype. The parent is set to the given prototype.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 */
	public Genotype(Genotype<A> prototype) {
		super(prototype);

		List<Genotype<?>> p = new ArrayList<>();
		p.add(prototype);
		this.parents = Collections.unmodifiableList(p);

		alleles = newAlleleCollection();

		init(prototype.alleles);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}. Create a
	 * Genotype with the given alleles and parents.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 * @param alleles The Alleles (backed by {@link Gene}s) that make up the new Genotype. A copy of each allele is made
	 *            and put into a new collection.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only,
	 *            implementations of this class do not need to create new instances from multiple parents (this is the
	 *            job of {@link Recombiner}s).
	 */
	public Genotype(Genotype<A> prototype, Collection<A> alleles, List<Genotype<?>> parents) {
		super(prototype);

		this.parents = Collections.unmodifiableList(new ArrayList<>(parents));
		this.alleles = newAlleleCollection();

		init(alleles);
	}

	private void init(Collection<A> alleles) {
		for (A allele : alleles) {
			A newAllele = allele.newInstance();
			this.alleles.add(newAllele);
		}
		
		allelesByGeneType = ArrayListMultimap.create();
		for (A allele : this.alleles) {
			updateAllelesByGeneTypeForAdd(allele);
		}
	}

	/**
	 * Create and return a new Collection to be used to store Alleles. This is used by the Genotype constructors. This
	 * allows sub-classes to specify the kind of Collection to use to store alleles. For example should it be a
	 * {@link java.util.List}, a {@link java.util.Set}, a {@link java.util.SortedSet}, a {@link java.util.HashSet}...?
	 */
	protected abstract Collection<A> newAlleleCollection();

	/**
	 * Get the alleles of this genotype as an unmodifiable collection. The iterator of the returned collection will
	 * iterate over the alleles in the same order as the collection provided to the constructor.
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
	 *         as the Collection provided to {@link #Genotype(Genotype, Collection, List)} iterates over those alleles. If no alleles of
	 *         the specified type exist then an empty list is returned.
	 * @see #hasAllelesOfType(Object)
	 */
	public List<A> getAllelesOfType(Object type) {
		return Collections.unmodifiableList(allelesByGeneType.get(type));
	}

	/**
	 * Get the list of alleles in this genotype that are for {@link Gene}s of the specified type, or a provided default
	 * list if there are no alleles of specified type present. Note that an allele/gene pair may have multiple types.
	 * 
	 * @param type Object representing the type, usually an enum constant.
	 * @param defaultList A list of default values to return if there are no alleles of specified type present. May
	 *            be null.
	 * @return The list of alleles in this genotype that are for {@link Gene}s of the specified type, in the same order
	 *         as the Set provided to {@link #Genotype(Genotype, Collection, List)} iterates over those alleles.
	 * @see #hasAllelesOfType(Object)
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

	@Override
	public void getStringableMap(Map<String, Object> map) {
		super.getStringableMap(map);
		map.put("alleles", alleles);
	}
}

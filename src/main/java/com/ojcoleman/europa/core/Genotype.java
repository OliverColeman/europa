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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.reflect.TypeToken;

/**
 * Represents the inheritable genetic material of an {@link Individual}.
 * 
 * @author O. J. Coleman
 */
public abstract class Genotype<A extends Allele<?>> {
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
	 * Create a new Genotype instance. Sub-classes should generally call this constructor from their own constructor(s).
	 * @param alleles The alleles (backed by {@link Gene}s) that make up the new genotype. This Collection is not copied but only stored as a reference.
	 * @param parents The parents that were used to create this genotype (this is for record keeping only, implementations of this class
	 * do not need to create new instances from multiple parents (this is the job of {@link Recombiner)s.
	 */
	@SafeVarargs
	public Genotype(long id, Collection<A> alleles, Genotype<?>... parents) {
		this.id = id;
		this.alleles = alleles;
		if (parents == null) {
			this.parents = Collections.unmodifiableList(new ArrayList<Genotype<?>>());
		}
		else {
			this.parents = Collections.unmodifiableList(Arrays.asList(parents));
		}
		
		allelesByGeneType = ArrayListMultimap.create();
		for (A allele : this.alleles) {
			updateAllelesByGeneTypeForAdd(allele);
		}
	}
	
	/**
	 * Get the alleles of this genotype as an unmodifiable collection. 
	 * The iterator of the returned collection will iterate over the alleles in the same order as the collection provided to (
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
	 * Get the list of alleles in this genotype that are for {@link Gene}s of the specified type.
	 * Note that an allele/gene pair may have multiple types.
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
	 * Add the given allele to this genotype. This method should generally only be called by {@link Mutator}s and {@link Recombiner}s.
	 */
	public void addAllele(A allele) {
		alleles.add(allele);
		updateAllelesByGeneTypeForAdd(allele);
	}
	
	/**
	 * Remove the given allele from this genotype. This method should generally only be called by {@link Mutator}s and {@link Recombiner}s.
	 */
	public void removeAllele(A allele) {
		alleles.remove(allele);
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
	
	
	/**
	 * Create a new Genotype of this class with the given id, alleles and (optionally) parents.
	 */
	public abstract Genotype<A> create(long id, Collection<A> alleles, Genotype<?>... parents);
	
	
	@SafeVarargs
	public final Genotype<A> createNew(long id, Collection<?> alleles, Genotype<?>... parents) {
		Collection<A> typedAlleles = new ArrayList<A>(alleles.size());
		
		for (Object o : alleles) {
			try {
				A allele = (A) o;
				
				Constructor<A> alleleConstructor = (Constructor<A>) allele.getClass().getConstructor(allele.getClass());
				
				typedAlleles.add(alleleConstructor.newInstance(allele));
			}
			catch (ClassCastException e) {
				throw new RuntimeException("The alleles passed to Genotype.new do not match the allele type of the genotype.");
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("The allele class " + o.getClass().getName() + " must define a copy constructor.");
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		
		return create(id, typedAlleles, parents);
	}
}

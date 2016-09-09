package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.util.Stringer;

/**
 * Represents a species in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public class Species<G extends Genotype<?>> extends PrototypeBase {
	/**
	 * The members of this Species.
	 */
	protected Set<Individual<G, ?>> members;

	/**
	 * The number of generations this Species has persisted for.
	 */
	protected int age;
	
	/**
	 * Indicates that this species should be considered defunct. Typically the {@link Evolver} may mark a species as being defunct, 
	 * and the {@link Population} will subsequently remove the species.
	 */ 
	protected boolean defunct;
	
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(Configuration)}.
	 */
	public Species(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
	 * 
	 * @param prototype The (prototype) instance to copy.
	 */
	public Species(Species<G> prototype) {
		super(prototype);

		members = new HashSet<>();
	}

	/**
	 * Adds the given Individual to this Species. This should generally only be called by a {@link Speciator}.
	 */
	public synchronized void addMember(Individual<G, ?> member) {
		if (members.contains(member)) {
			throw new IllegalStateException("Attempted to add an Individual to the Species it's already in.");
		}
		if (member.species != null) {
			throw new IllegalStateException("Attempted to add an Individual to a Species when it's already in a Species.");
		}

		members.add(member);
		member.species = this;
		
		boolean found = false;
		for (Individual<G, ?> m : members) {
			if (member.id == m.id) {
				found = true;
				break;
				
			}
		}
		
		if (!found) {
			System.out.println((found ? "" : "NOT ") + "FOUND");
		}
		
		assert members.contains(member);
	}

	/**
	 * Removes the given Individual from this Species. This should generally only be called by a {@link Speciator}.
	 */
	public synchronized void removeMember(Individual<G, ?> member) {
		if (!members.contains(member)) {
			throw new IllegalStateException("Attempted to remove an Individual from a Species it's not in. This species: " + this.id + ", Individual species: " + member.getSpecies().id);
		}
		members.remove(member);
		member.species = null;
	}

	/**
	 * Adds the given Individual to this Species, removing it if necessary from it's current Species. This should
	 * generally only be called by a {@link Speciator}.
	 */
	public synchronized void addMemberRemoveFromCurrent(Individual<G, ?> member) {
		if (member.hasSpecies()) {
			member.getSpecies().removeMember(member);
		}
		addMember(member);
	}

	/**
	 * Returns an unmodifiable view of the members of this Species.
	 */
	public Set<Individual<G, ?>> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	/**
	 * Returns the number of members in this Species.
	 */
	public int size() {
		return members.size();
	}

	/**
	 * Returns the number of generations this species has persisted for.
	 */
	public int getAge() {
		return age;
	}

	/**
	 * Increments the number of generations this species has persisted for.
	 */
	public void incrementAge() {
		age++;
	}
	
	/**
	 * Mark this species as defunct. Typically the {@link Evolver} may mark a species as being defunct, 
	 * and the {@link Population} will subsequently remove the species.
	 */ 
	public void setDefunct() {
		defunct = true;
	}
	
	/**
	 * Indicates that this species should be considered defunct. Typically the {@link Evolver} may mark a species as being defunct, 
	 * and the {@link Population} will subsequently remove the species.
	 */ 
	public boolean isDefunct() {
		return defunct;
	}
	
	/**
	 * Returns true iff there are no Individuals in this Species.
	 */
	public boolean isEmpty() {
		return members.size() == 0;
	}
	
	/**
	 * Removes all the members from this Species.
	 */
	public synchronized void clear() {
		for (Individual<G, ?> ind : members) {
			this.removeMember(ind);
		}
	}
}

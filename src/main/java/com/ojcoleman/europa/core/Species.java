package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;

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
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public Species(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
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
	public void addMember(Individual<G, ?> member) {
		if (members.contains(member)) {
			throw new IllegalStateException("Attempted to add an Individual to the Species it's already in.");
		}
		if (member.species != null) {
			throw new IllegalStateException("Attempted to add an Individual to a Species when it's already in a Species.");
		}

		members.add(member);
		member.species = this;
	}

	/**
	 * Removes the given Individual from this Species. This should generally only be called by a {@link Speciator}.
	 */
	public void removeMember(Individual<G, ?> member) {
		if (!members.contains(member)) {
			throw new IllegalStateException("Attempted to remove an Individual from a Species it's not in.");
		}
		members.remove(member);
		member.species = null;
	}

	/**
	 * Adds the given Individual to this Species., removing it if necessary from it's current Species. This should
	 * generally only be called by a {@link Speciator}.
	 */
	public void addMemberRemoveFromCurrent(Individual<G, ?> member) {
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
	 * Get the average rank of the members of this species. This is used for "fitness sharing"
	 */
	public double getAverageRank() {
		double totalRank = 0;
		for (Individual<G, ?> ind : members) {
			totalRank += ind.getRank();
		}
		return totalRank / members.size();
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
	public void clear() {
		for (Individual<G, ?> ind : members) {
			this.removeMember(ind);
		}
	}
}

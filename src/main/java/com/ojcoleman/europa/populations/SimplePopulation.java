package com.ojcoleman.europa.populations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;

/**
 * DefaultEvolver population class. Stores {@link Individual}s as a HashSet.
 * 
 * @author O. J. Coleman
 */
public class SimplePopulation extends Population {
	private final HashSet<Individual> members;
	
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public SimplePopulation(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		members = new HashSet<Individual>();
	}
	
	@Override
	public Set<Individual> getMembers() {
		return Collections.unmodifiableSet(members);
	}

	@Override
	public void addIndividual(Individual individual) {
		members.add(individual);
	}

	@Override
	public void removeIndividual(Individual individual) {
		members.remove(individual);
	}
}

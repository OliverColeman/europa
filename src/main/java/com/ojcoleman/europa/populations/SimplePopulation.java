package com.ojcoleman.europa.populations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;

/**
 * DefaultEvolver population class. Stores {@link Individual}s as a HashSet.
 * 
 * @author O. J. Coleman
 */
public class SimplePopulation extends Population {
	private final Map<Long, Individual<?>> members;

	/**
	 * Constructor for {@link Component}.
	 */
	public SimplePopulation(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		members = new HashMap<>();
	}

	@Override
	public Collection<Individual<?>> getMembers() {
		return Collections.unmodifiableCollection(members.values());
	}

	@Override
	public void addIndividual(Individual<?> individual) {
		members.put(individual.genotype.id, individual);
	}

	@Override
	public void removeIndividual(Individual<?> individual) {
		members.remove(individual);
	}

	public Individual<?> getIndividual(long genotypeID) {
		return members.get(genotypeID);
	}
}

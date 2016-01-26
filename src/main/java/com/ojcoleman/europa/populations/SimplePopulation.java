package com.ojcoleman.europa.populations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;

/**
 * DefaultEvolver population class. Stores {@link Individual}s as a HashSet.
 * 
 * @author O. J. Coleman
 */
public class SimplePopulation<G extends Genotype<?>, F extends Function<?, ?>> extends Population<G, F> {
	private final Map<Long, Individual<G, F>> members;

	/**
	 * Constructor for {@link Component}.
	 */
	public SimplePopulation(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		members = new HashMap<>();
	}

	@Override
	public Collection<Individual<G, F>> getMembers() {
		return Collections.unmodifiableCollection(members.values());
	}

	@Override
	public void addIndividual(Individual<G, F> individual) {
		members.put(individual.genotype.id, individual);
	}

	@Override
	public void removeIndividual(Individual<G, F> individual) {
		members.remove(individual);
	}

	public Individual<G, F> getIndividual(long genotypeID) {
		return members.get(genotypeID);
	}
}

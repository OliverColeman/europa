package com.ojcoleman.europa.populations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;

/**
 * DefaultEvolver population class. Stores {@link Individual}s as a HashMap.
 * 
 * @author O. J. Coleman
 */
public class SimplePopulation<G extends Genotype<?>, F extends Function<?, ?>> extends Population<G, F> {
	private final Map<Long, Individual<G, F>> members;

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public SimplePopulation(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		members = Collections.synchronizedMap(new HashMap<Long, Individual<G, F>>());
	}

	@Override
	public Collection<Individual<G, F>> getMembers() {
		return Collections.unmodifiableCollection(members.values());
	}

	@Override
	public void addIndividual(Individual<G, F> individual) {
		members.put(individual.genotype.id, individual);
	}

	public Individual<G, F> getIndividual(long genotypeID) {
		return members.get(genotypeID);
	}

	@Override
	public int size() {
		return members.size();
	}

	@Override
	protected void remove(Individual<G, ?> ind) {
		members.remove(ind.genotype.id);
	}
}

package com.ojcoleman.europa.speciators;

import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Speciator;

/**
 * The default speciator performs no speciation but simply returns the given population as a single species.
 * 
 * @author O. J. Coleman
 */
public class NoSpeciation extends Speciator {
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public NoSpeciation(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public List<List<Individual>> speciate(Population pop) {
		List<Individual> speciesOne = new ArrayList<Individual>();
		speciesOne.addAll(pop.getMembers());
		
		List<List<Individual>> species = new ArrayList<List<Individual>>();
		species.add(speciesOne);
		
		return species;
	}
}

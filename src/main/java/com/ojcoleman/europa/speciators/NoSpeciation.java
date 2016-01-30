package com.ojcoleman.europa.speciators;

import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Speciator;
import com.ojcoleman.europa.core.Species;

/**
 * The default speciator simply assigns all individuals of the population to a single species.
 * 
 * @author O. J. Coleman
 */
public class NoSpeciation<G extends Genotype<?>, F extends Function<?, ?>> extends Speciator<G, F> {
	private final Species<G, F> theOnlySpecies;
	
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public NoSpeciation(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		theOnlySpecies = this.speciesPrototype.newInstance();
	}

	@Override
	public void speciate(Population<G, F> population, List<Species<G, F>> species) {
		if (species.isEmpty()) {
			species.add(theOnlySpecies);
		}
		for (Individual<G, F> ind : population.getMembers()) {
			if (ind.getSpecies() == null) {
				theOnlySpecies.addMember(ind);
			}
		}
	}

}

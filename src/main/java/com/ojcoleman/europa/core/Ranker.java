package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;

/**
 * A component that ranks the individuals in a population. This information is then used to select parents and elites.
 * 
 * @author O. J. Coleman
 */
public abstract class Ranker<G extends Genotype<?>, F extends Function<?, ?>> extends ComponentBase {
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Ranker(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * 
	 * @param population The speciated (if applicable) population.
	 */
	public abstract void rank(Population<G, F> population);
}

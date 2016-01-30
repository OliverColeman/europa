package com.ojcoleman.europa.core;

import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Prototype;

/**
 * Base class of classes that divide a {@link Population} into {@link Species}. 
 * 
 * @author O. J. Coleman
 */
public abstract class Speciator<G extends Genotype<?>, F extends Function<?, ?>> extends ComponentBase {
	@Prototype (description="The common (prototype) configuration for Species.", defaultClass=Species.class)
	protected Species<G, F> speciesPrototype;
	
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Speciator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Divide the given Population into species.
	 * 
	 * @param population The population to speciate.
	 * @param species The current set of Species.
	 */
	public abstract void speciate(Population<G, F> population, List<Species<G, F>> species);
}

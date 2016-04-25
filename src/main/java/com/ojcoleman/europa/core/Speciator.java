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
public abstract class Speciator<G extends Genotype<?>, S extends Species<G>> extends ComponentBase {
	@Prototype(description = "The common (prototype) configuration for Species.", defaultClass = Species.class)
	protected S speciesPrototype;

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Speciator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Divide the given Population into species. Species may be added/removed to/from the given list of Species.
	 * Individuals in the Population should be added/removed to/from Species with the methods
	 * {@link Species#addMember(Individual)} and {@link Species#removeMember(Individual)}.
	 * 
	 * @param population The population to speciate.
	 * @param species The (current) Species.
	 */
	public abstract void speciate(Population<G, ?> population, List<S> species);
}

package com.ojcoleman.europa.core;

import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsPrototype;

/**
 * Base class of classes that divide a {@link Population} into {@link Species}. 
 * 
 * @author O. J. Coleman
 */
public abstract class Speciator<G extends Genotype<?>, F extends Function<?, ?>> extends Component {
	@IsPrototype (description="The common (prototype) configuration for Species.", defaultClass=Species.class)
	protected Species<G, F> speciesPrototype;
	
	/**
	 * Constructor for {@link Component}.
	 */
	public Speciator(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Divide the given Population into species.
	 * 
	 * @param population The population to speciate.
	 * @param currentSpeciesMap The current mapping from Species to the members of the Species.
	 * 
	 * @return A mapping from Species to the members of the Species.
	 */
	public abstract ArrayListMultimap<Species<G, F>, Individual<G, F>> speciate(Population<G, F> population, ArrayListMultimap<Species<G, F>, Individual<G, F>> currentSpeciesMap);
}

package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;

/**
 * A component that ranks the individuals in a population. This information is then used to select parents and elites.
 * 
 * @author O. J. Coleman
 */
public abstract class Ranker extends Component {
	/**
	 * Constructor for {@link Component}.
	 */
	public Ranker(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * 
	 * @param population The speciated (if applicable) population.
	 */
	public abstract void rank(Population<?, ?> population);
}

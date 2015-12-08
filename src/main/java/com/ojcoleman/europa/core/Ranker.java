package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;

/**
 * A component that ranks the individuals in a population. This information is then used to select parents and elites.
 * 
 * @author O. J. Coleman
 */
public abstract class Ranker extends ConfigurableComponent {
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public Ranker(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	/**
	 * Sub-classes should select from the given speciated (if applicable) population:<ul>
	 * <li>a set of parents that are used to produce the children in the next generation (for example via cloning or recombination); and</li>
	 * <li>optionally a set of elites that will survive to the next generation unchanged</li>
	 * </ul>
	 * @param pop The speciated (if applicable) population.
	 * @param parents Selected parents should be added to this.
	 * @param parents Selected elites should be added to this.
	 */
	public abstract void rank(Population pop);
}

package com.ojcoleman.europa.rankers;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Ranker;

/**
 * This is a simple ranker that only considers the primary fitness objective when ranking indivuduals.
 * 
 * @author O. J. Coleman
 */ 
public class DefaultRanker extends Ranker {
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public DefaultRanker(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	public void rank(Population pop) {
		
	}
}

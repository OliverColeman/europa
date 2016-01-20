package com.ojcoleman.europa.rankers;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Ranker;

/**
 * This is a simple ranker that only considers the primary fitness objective when ranking indivuduals.
 * 
 * @author O. J. Coleman
 */
public class DefaultRanker extends Ranker {
	/**
	 * Constructor for {@link Component}.
	 */
	public DefaultRanker(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	public void rank(Population pop) {

	}
}

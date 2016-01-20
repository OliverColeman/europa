package com.ojcoleman.europa.core;

import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;

/**
 * Base class of classes that divide a {@link Population} into species.
 * 
 * @author O. J. Coleman
 */
public abstract class Speciator extends Component {
	/**
	 * Constructor for {@link Component}.
	 */
	public Speciator(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Divide the given Population into species.
	 * 
	 * @return A Collection of Collections of Individuals, where the sub-collections represent species.
	 */
	public abstract List<List<Individual>> speciate(Population pop);
}

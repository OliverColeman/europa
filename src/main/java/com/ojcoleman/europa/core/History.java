package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;

/**
 * Base class of classes that store information about previous generations of populations and individuals.
 * 
 * @author O. J. Coleman
 */
public class History extends ConfigurableComponent {
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public History(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		// TODO Auto-generated constructor stub
	}

}

package com.ojcoleman.europa.configurable;

import com.eclipsesource.json.JsonObject;

/**
 * Describes the configuration for a {@link ConfigurableBase}.
 * 
 * @author O. J. Coleman
 */
public class Configuration extends JsonObject {
	/**
	 * Indicates this configuration is for a dummy instance, typically used for printing out configuration options.
	 */
	public final boolean isDummy;
	
	/**
	 * The ID factory used to create unique IDs for the target Configurable.
	 */
	public final IDFactory idFactory;
	
	public Configuration(JsonObject config, boolean isDummy, IDFactory idFactory) {
		super(config);
		this.isDummy = isDummy;
		this.idFactory = idFactory;
	}
}

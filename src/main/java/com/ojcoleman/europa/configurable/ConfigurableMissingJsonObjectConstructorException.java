package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link IsPrototype} does not implement a constructor accepting a single parameter of type
 * JsonObject.
 * 
 * @author O. J. Coleman
 */
public class ConfigurableMissingJsonObjectConstructorException extends ConfigurableException {
	public ConfigurableMissingJsonObjectConstructorException(String message) {
		super(message);
	}
}

package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link Component} does not implement a constructor accepting a Component and JsonObject as parameter.
 * 
 * @author O. J. Coleman
 */
public class ComponentMissingConstructorException extends ConfigurableException {
	public ComponentMissingConstructorException(String message) {
		super(message);
	}
}

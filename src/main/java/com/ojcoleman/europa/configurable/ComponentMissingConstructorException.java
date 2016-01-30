package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link ComponentBase} does not implement a constructor accepting a ComponentBase and JsonObject as parameter.
 * 
 * @author O. J. Coleman
 */
public class ComponentMissingConstructorException extends ConfigurableException {
	public ComponentMissingConstructorException(String message) {
		super(message);
	}
}

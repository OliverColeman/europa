package com.ojcoleman.europa.configurable;

/**
 * Indicates that an exception occurred in the constructor of a {@link ComponentBase} when trying to instantiate
 * it as a sub-component of another component.
 * 
 * @author O. J. Coleman
 */
public class ComponentConstructorException extends ConfigurableException {
	public ComponentConstructorException(String message) {
		super(message);
	}
}

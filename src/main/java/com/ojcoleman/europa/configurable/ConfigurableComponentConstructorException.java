package com.ojcoleman.europa.configurable;

/**
 * Indicates that an exception occurred in the constructor of a 
 * {@link ConfigurableComponent} when trying to instantiate it as
 * a sub-component of another component.
 * 
 * @author O. J. Coleman
 */
public class ConfigurableComponentConstructorException extends RuntimeException {
	public ConfigurableComponentConstructorException(String message, Throwable ex) {
		super(message, ex);
	}
}

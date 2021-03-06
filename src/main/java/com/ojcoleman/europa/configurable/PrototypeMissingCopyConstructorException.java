package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link Prototype} does not implement a copy constructor accepting a single parameter of
 * the sub-class type.
 * 
 * @author O. J. Coleman
 */
public class PrototypeMissingCopyConstructorException extends ConfigurableException {
	public PrototypeMissingCopyConstructorException(String message) {
		super(message);
	}
}

package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link IsPrototype} does not implement a copy constructor accepting a single parameter of
 * the sub-class type.
 * 
 * @author O. J. Coleman
 */
public class PrototypeMissingCopyConstructorException extends RuntimeException {
	public PrototypeMissingCopyConstructorException(String message) {
		super(message);
	}
}

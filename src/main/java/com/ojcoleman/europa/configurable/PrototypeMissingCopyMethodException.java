package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link IsPrototype} does not override {@link IsPrototype#copy()}.
 * 
 * @author O. J. Coleman
 */
public class PrototypeMissingCopyMethodException extends ConfigurableException {
	public PrototypeMissingCopyMethodException(String message) {
		super(message);
	}
}

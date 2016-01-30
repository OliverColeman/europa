package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link Prototype} does not override {@link Prototype#copy()}.
 * 
 * @author O. J. Coleman
 */
public class PrototypeMissingCopyMethodException extends ConfigurableException {
	public PrototypeMissingCopyMethodException(String message) {
		super(message);
	}
}

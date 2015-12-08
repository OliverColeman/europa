package com.ojcoleman.europa.configurable;

/**
 * Indicates that a field annotated with {@link @SubComponent} does not 
 * extend {@link ConfigurableComponent}.
 * 
 * @author O. J. Coleman
 */
public class InvalidSubComponentFieldException extends RuntimeException {
	public InvalidSubComponentFieldException(String message) {
		super(message);
	}
}

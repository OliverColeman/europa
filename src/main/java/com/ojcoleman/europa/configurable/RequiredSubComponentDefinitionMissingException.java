package com.ojcoleman.europa.configurable;

/**
 * Indicates that a {@link @SubComponent} field in a {@link ConfigurableComponent} 
 * which has no default implementation specified and is not optional has not 
 * had a definition specifying an implementation provided for it.
 * 
 * @author O. J. Coleman
 */
public class RequiredSubComponentDefinitionMissingException extends RuntimeException {
	public RequiredSubComponentDefinitionMissingException(String message) {
		super(message);
	}
}

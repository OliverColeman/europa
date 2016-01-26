package com.ojcoleman.europa.configurable;

/**
 * Indicates that a {@link @IsComponent} field in a {@link Component} which has no default implementation
 * specified and is not optional has not had a definition specifying an implementation provided for it.
 * 
 * @author O. J. Coleman
 */
public class RequiredComponentDefinitionMissingException extends ConfigurableException {
	public RequiredComponentDefinitionMissingException(String message) {
		super(message);
	}
}

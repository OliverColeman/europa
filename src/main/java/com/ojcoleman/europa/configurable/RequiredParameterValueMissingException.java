package com.ojcoleman.europa.configurable;

/**
 * Indicates that a {@link @Parameter} field in a {@link ComponentBase} which has no default value and is not optional
 * has not had a value provided for it.
 * 
 * @author O. J. Coleman
 */
public class RequiredParameterValueMissingException extends ConfigurableException {
	public RequiredParameterValueMissingException(String message) {
		super(message);
	}
}

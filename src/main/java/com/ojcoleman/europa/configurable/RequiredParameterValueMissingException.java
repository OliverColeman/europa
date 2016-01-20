package com.ojcoleman.europa.configurable;

/**
 * Indicates that a {@link @IsParameter} field in a {@link Component} which has no default value and is not
 * optional has not had a value provided for it.
 * 
 * @author O. J. Coleman
 */
public class RequiredParameterValueMissingException extends RuntimeException {
	public RequiredParameterValueMissingException(String message) {
		super(message);
	}
}

package com.ojcoleman.europa.configurable;

/**
 * Indicates that the value provided for a {@link @Parameter} field in a {@link ComponentBase} is invalid
 * (failed validation such as acceptable range or regex matching).
 * 
 * @author O. J. Coleman
 */
public class InvalidParameterValueException extends ConfigurableException {
	public InvalidParameterValueException(String message) {
		super(message);
	}
}

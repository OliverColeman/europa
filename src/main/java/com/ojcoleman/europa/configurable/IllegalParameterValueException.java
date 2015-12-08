package com.ojcoleman.europa.configurable;

/**
 * Indicates that the value provided for a {@link @Parameter} field in a 
 * {@link ConfigurableComponent} is invalid (failed validation such as 
 * acceptable range or regex matching).
 * 
 * @author O. J. Coleman
 */
public class IllegalParameterValueException extends RuntimeException {
	public IllegalParameterValueException(String message) {
		super(message);
	}
}

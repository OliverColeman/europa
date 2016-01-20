package com.ojcoleman.europa.configurable;

/**
 * Indicates that the value provided for a {@link @IsParameter} field in a {@link Component} is invalid
 * (failed validation such as acceptable range or regex matching).
 * 
 * @author O. J. Coleman
 */
public class InvalidParameterValueException extends RuntimeException {
	public InvalidParameterValueException(String message) {
		super(message);
	}
}

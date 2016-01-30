package com.ojcoleman.europa.configurable;

/**
 * Indicates an error with a field annotated {@link @Parameter}. 
 * 
 * @author O. J. Coleman
 */
public class InvalidParameterFieldException extends ConfigurableException {
	public InvalidParameterFieldException(String message) {
		super(message);
	}
	
	public InvalidParameterFieldException(String message, Throwable cause) {
		super(message, cause);
	}
}

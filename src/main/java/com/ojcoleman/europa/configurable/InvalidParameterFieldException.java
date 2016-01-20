package com.ojcoleman.europa.configurable;

/**
 * Indicates an error with a field annotated {@link @IsParameter}. 
 * 
 * @author O. J. Coleman
 */
public class InvalidParameterFieldException extends RuntimeException {
	public InvalidParameterFieldException(String message) {
		super(message);
	}
	
	public InvalidParameterFieldException(String message, Throwable cause) {
		super(message, cause);
	}
}

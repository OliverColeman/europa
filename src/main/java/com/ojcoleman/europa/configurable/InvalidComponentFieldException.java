package com.ojcoleman.europa.configurable;

/**
 * Indicates that a field annotated with {@link @IsComponent} does not extend {@link Component}.
 * 
 * @author O. J. Coleman
 */
public class InvalidComponentFieldException extends RuntimeException {
	public InvalidComponentFieldException(String message) {
		super(message);
	}
}

package com.ojcoleman.europa.configurable;

/**
 * Indicates that the element values for an {@link @IsParameter} annotation are invalid.
 * 
 * @author O. J. Coleman
 */
public class InvalidConfigurableAnnotationException extends ConfigurableException {
	public InvalidConfigurableAnnotationException(String message) {
		super(message);
	}
	
	public InvalidConfigurableAnnotationException(String message, Throwable cause) {
		super(message, cause);
	}
}

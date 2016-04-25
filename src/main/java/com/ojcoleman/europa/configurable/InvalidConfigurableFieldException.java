package com.ojcoleman.europa.configurable;

/**
 * Indicates an invalid configuration for a field annotated with {@link Parameter}, {@link Configurable},
 * {@link Prototype} or {@link Component}.
 * 
 * @author O. J. Coleman
 */
public class InvalidConfigurableFieldException extends ConfigurableException {
	public InvalidConfigurableFieldException(String message) {
		super(message);
	}

	public InvalidConfigurableFieldException(String message, Exception cause) {
		super(message, cause);
	}
}

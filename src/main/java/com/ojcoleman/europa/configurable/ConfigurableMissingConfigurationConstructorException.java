package com.ojcoleman.europa.configurable;

/**
 * Indicates that a sub-class of {@link ConfigurableBase} does not implement a constructor accepting a single parameter
 * of type Configuration.
 * 
 * @author O. J. Coleman
 */
public class ConfigurableMissingConfigurationConstructorException extends ConfigurableException {
	public ConfigurableMissingConfigurationConstructorException(String message) {
		super(message);
	}
}

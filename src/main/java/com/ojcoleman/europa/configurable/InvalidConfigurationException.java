package com.ojcoleman.europa.configurable;

/**
 * Exception indicating an invalid set of configuration values has been provided.
 * 
 * @author O. J. Coleman
 */
public class InvalidConfigurationException extends ConfigurableException {
	public InvalidConfigurationException(String string) {
		super(string);
	}
	public InvalidConfigurationException(String string, Exception cause) {
		super(string, cause);
	}
}

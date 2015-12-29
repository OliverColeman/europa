package com.ojcoleman.europa.configurable;

/**
 * Exception indicating an invalid set of configuration values has been provided.
 * 
 * @author O. J. Coleman
 */
public class InvalidConfigurationException extends Exception {
	public InvalidConfigurationException(String string) {
		super(string);
	}
}

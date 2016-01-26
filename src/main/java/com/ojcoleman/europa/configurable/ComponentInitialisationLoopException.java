package com.ojcoleman.europa.configurable;

/**
 * Indicates that two or more components are requesting access to each other during their initialisation.
 */
public class ComponentInitialisationLoopException extends ConfigurableException {
	public ComponentInitialisationLoopException(String message) {
		super(message);
	}
}

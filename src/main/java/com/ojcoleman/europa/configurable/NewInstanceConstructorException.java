package com.ojcoleman.europa.configurable;

/**
 * Indicates that an exception occurred in the constructor of an object being instantiated by
 * {@link ComponentBase#newGenericInstance(Class)}.
 * 
 * @author O. J. Coleman
 */
public class NewInstanceConstructorException extends ConfigurableException {
	public NewInstanceConstructorException(String message) {
		super(message);
	}

	public NewInstanceConstructorException(String message, Throwable ex) {
		super(message, ex);
	}
}

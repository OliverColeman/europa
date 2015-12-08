package com.ojcoleman.europa.configurable;

/**
 * Indicates that an exception occurred in the constructor of an object
 * being instantiated by {@link ConfigurableComponent#newInstance(Class, Object...)}.
 * 
 * @author O. J. Coleman
 */
public class NewInstanceConstructorException extends RuntimeException {
	public NewInstanceConstructorException(String message, Throwable ex) {
		super(message, ex);
	}
}

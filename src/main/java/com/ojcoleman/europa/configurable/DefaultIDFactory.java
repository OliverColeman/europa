package com.ojcoleman.europa.configurable;

/**
 * Default class to generate unique UDs. Each call to {@link #getNextID()} increments a counter and returns its current
 * value.
 * 
 * @author O. J. Coleman
 */
public class DefaultIDFactory implements IDFactory {
	private long nextID;

	@Override
	public synchronized long getNextID() {
		return ++nextID;
	}
}

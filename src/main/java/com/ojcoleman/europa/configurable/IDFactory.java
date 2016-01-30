package com.ojcoleman.europa.configurable;

import com.ojcoleman.europa.core.Run;

/**
 * Interface for factory that creates a unique ID on every request. The default ID factory in Europa is {@link Run}.
 * 
 * @author O. J. Coleman
 */
public interface IDFactory {
	/**
	 * Get the next unique ID. Implementations should ensure this method is thread-safe.
	 */
	public long getNextID();
}

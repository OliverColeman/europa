package com.ojcoleman.europa.core;

import java.util.Map;

import com.ojcoleman.europa.util.Stringer;

/**
 * Interface for classes whose current state may be converted into a human-readable String by
 * {@link Stringer#toString(Object)}.
 * 
 * @author O. J. Coleman
 */
public interface Stringable {
	/**
	 * Populate the given map, which is typically to be converted into a String by {@link Stringer#toString(Object)}.
	 * When overriding this method in a sub-class the super-class implementation should usually be called from the
	 * overriding implementation.
	 * 
	 * @param map Mapping from descriptive labels to Objects that can be converted into a String by
	 *            {@link Stringer#toString(Object)}.
	 */
	public void getStringableMap(Map<String, Object> map);
}

package com.ojcoleman.europa.core;

import java.util.Map;

import com.ojcoleman.europa.util.StructuredStringableStringer;

/**
 * Interface for classes that implement a method to generate an object that can be converted into a sensible String by
 * {@link StructuredStringableStringer#objectToString(Object)}.
 * 
 * @author O. J. Coleman
 */
public interface StructuredStringable {
	/**
	 * Populate the given map, which is typically to be converted into a String by {@link StructuredStringableStringer#objectToString(Object)}.
	 * When overriding this method in a sub-class the super-class implementation should usually be called from the overriding implementation.
	 * 
	 * @param map Mapping from descriptive labels to Objects that can be converted into a String by {@link StructuredStringableStringer#objectToString(Object)}.
	 */
	public void getStructuredStringableObject(Map<String, Object> map);
}

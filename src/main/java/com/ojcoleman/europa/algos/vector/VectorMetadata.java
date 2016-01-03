package com.ojcoleman.europa.algos.vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.ojcoleman.europa.util.Interval;
import com.ojcoleman.europa.util.IntervalDouble;
import com.ojcoleman.europa.util.IntervalLong;

/**
 * Class for containing meta-data about each value in a numeric vector.
 * 
 * @author O. J. Coleman
 */
public class VectorMetadata {
	/**
	 * Metadata for an empty (zero length/size) vector.
	 */ 
	public final static VectorMetadata EMPTY = new VectorMetadata(new JsonObject());
	
	private String[] labels;
	private Interval<?>[] bounds;
	private boolean[] isInteger;
	private HashMap<String, Integer> labelIndexMap;
	
	/**
	 * Create a VectorMetadata with the given properties.
	 * 
	 * @param labels The label for each value in the vector.
	 * @param bounds The minimum and maximum (inclusive) values for each value in the vector.
	 * @param isInteger Whether a value in the vector should only take on integer values.
	 * @throws IllegalArgumentException If the lengths of the bounds and isInteger arrays are not equal.
	 */
	public VectorMetadata(String[] labels, Interval<?>[] bounds, boolean[] isInteger) {
		if (bounds.length != isInteger.length || bounds.length != labels.length) {
			throw new IllegalArgumentException("The lengths of the labels, bounds and isInteger arrays must be equal.");
		}
		
		this.labels = labels;
		this.bounds = bounds;
		this.isInteger = isInteger;
		
		labelIndexMap = new HashMap<String, Integer>();
		for (int i= 0; i < labels.length; i++) {
			labelIndexMap.put(labels[i], i);
		}
	}
	
	/**
	 * Create a VectorMetadata from the given JsonObject. An example JSON object: 
	 * <pre>
	 * {
	 *   "_defaults" : {"min": -1, "max": 2, "int": true}
	 *   "myParam" : {"min": -1}
	 *   "myOtherParam" : {"min": -2, "max": 3, "int": false}
	 * }
	 * </pre>
	 * The "_defaults" specify default values to use if values are not specified for a specific parameter. This is 
	 * optional, as is any of the keys within the defaults. If, for a given parameter, no value is specified for 
	 * the "min, "max" or "int" keys either explicitly or in the defaults then 0, 1 or false are used respectively.
	 */
	public VectorMetadata(JsonObject config) {
		initFromJsonObject(config);
	}
	
	private void initFromJsonObject(JsonObject config) {
		double defaultMin = 0;
		double defaultMax = 1;
		boolean defaultIsInt = false;
		if (config.get("_defaults") != null) {
			JsonObject defaultConfig = config.get("_defaults").asObject();
			defaultMin = defaultConfig.getDouble("min", defaultMin);
			defaultMax = defaultConfig.getDouble("max", defaultMax);
			defaultIsInt = defaultConfig.getBoolean("int", defaultIsInt);
		}
		
		int size = config.size() - (config.get("_defaults") != null ? 1 : 0); 
		labels = new String[size];
		bounds = new Interval[size];
		isInteger = new boolean[size];
		
		int index = 0;
		for (Member param : config) {
			if (!param.getName().equals("_defaults")) {
				JsonObject paramConfig = param.getValue() != null ? param.getValue().asObject() : new JsonObject();
				
				labels[index] = param.getName();
				isInteger[index] = paramConfig.getBoolean("int", defaultIsInt);
				bounds[index] = isInteger[index] ? 
						new IntervalLong(paramConfig.getLong("min", Math.round(defaultMin)), paramConfig.getLong("min", Math.round(defaultMax))) :
						new IntervalDouble(paramConfig.getDouble("min", defaultMin), paramConfig.getDouble("min", defaultMax));
				
			}
			index++;
		}
		
		labelIndexMap = new HashMap<String, Integer>();
		for (int i= 0; i < labels.length; i++) {
			labelIndexMap.put(labels[i], i);
		}
	}
	
	/**
	 * Returns the size of the vector(s) this VectorMetadata is intended to contain information about.
	 */ 
	public int size() {
		return bounds.length;
	}
	
	public boolean isEmpty() {
		return bounds.length == 0;
	}
	
	public Interval<?> bound(int index) {
		return bounds[index];
	}
	
	public boolean isInteger(int index) {
		return isInteger[index];
	}

	public List<String> getLabels() {
		return Collections.unmodifiableList(Arrays.asList(labels));
	}
	
	public String label(int index) {
		return labels[index];
	}
	
	public boolean hasLabel(String label) {
		return labelIndexMap.containsKey(label);
	}
	
	public Interval<?> bound(String label) {
		return bounds[labelIndexMap.get(label)];
	}

	public boolean isInteger(String label) {
		return isInteger[labelIndexMap.get(label)];
	}
	
	/**
	 * Two VectorMetadata objects are considered equal if they have the same labels, bounds and isInteger specifications in the same order.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other instanceof VectorMetadata) {
			VectorMetadata otherVI = (VectorMetadata) other;
			return Arrays.equals(labels, otherVI.labels) &&
					Arrays.equals(bounds, otherVI.bounds) &&
					Arrays.equals(isInteger, otherVI.isInteger);
		}
		return false;
	}
}

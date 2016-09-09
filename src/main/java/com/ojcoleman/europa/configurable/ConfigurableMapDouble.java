package com.ojcoleman.europa.configurable;

import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * A wrapper for a Map (actually a HashMap) to allow {@link Parameter)s 
 * that are a simple mapping from string names to double values.
 * 
 * @author O. J. Coleman
 */
public class ConfigurableMapDouble extends HashMap<String, Double> {
	public ConfigurableMapDouble(JsonValue config) {
		if (!(config.isObject())) {
			throw new InvalidParameterValueException("A JSON object is required.");
		}
		for (JsonObject.Member member : config.asObject()) {
			this.put(member.getName(), member.getValue().asDouble());
		}
	}
	
	/**
	 * Returns JSON string representing this ConfigurableMap.
	 */
	@Override
	public String toString() {
		JsonObject json = new JsonObject();
		for (Map.Entry<String, Double> entry : this.entrySet()) {
			json.add(entry.getKey(), entry.getValue());
		}
		return json.toString();
	}
}

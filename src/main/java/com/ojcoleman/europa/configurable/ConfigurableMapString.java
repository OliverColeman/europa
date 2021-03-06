package com.ojcoleman.europa.configurable;

import java.util.HashMap;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * A wrapper for a Map (actually a HashMap) to allow {@link Parameter)s 
 * that are a simple mapping from string names to String values.
 * 
 * @author O. J. Coleman
 */
public class ConfigurableMapString extends HashMap<String, String> {
	public ConfigurableMapString(JsonValue config) {
		if (!(config.isObject())) {
			throw new InvalidParameterValueException("A JSON object is required.");
		}
		for (JsonObject.Member member : config.asObject()) {
			this.put(member.getName(), member.getValue().asString());
		}
	}
}

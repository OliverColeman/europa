package com.ojcoleman.europa.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class to specify and record log items. Used in methods like {@link Transcriber#transcribe(Genotype, Function)}
 * and {@link Evaluator#evaluate(Individual, Log)} so that they may record implementation-specific details of the
 * process they perform.
 * 
 * @author O. J. Coleman
 */
public class Log {
	/**
	 * Log object to indicate no logging is required.
	 */
	public static final Log NO_LOG = new Log();

	// Keys are the specified log item labels, non-null values are the log item values.
	HashMap<String, Object> logs = new HashMap<>();

	/**
	 * Creates a new Log for which the specified log items should be recorded.
	 */
	public Log(String... items) {
		for (String l : items) {
			logs.put(l, null);
		}
	}

	/**
	 * Returns true iff this Log specifies the given log item.
	 */
	public boolean specifiesItem(String item) {
		return logs.containsKey(item);
	}

	/**
	 * Returns true iff the given log item has been set.
	 */
	public boolean hasItem(String item) {
		return logs.get(item) != null;
	}

	/**
	 * Get all the log items that have been specified (note that they may not have been set).
	 */
	public Set<String> getItemLabels() {
		return Collections.unmodifiableSet(logs.keySet());
	}

	/**
	 * Set the specified log item.
	 */
	public void setLog(String item, Object log) {
		if (!logs.containsKey(item)) {
			throw new IllegalArgumentException("Attempt to set log for non-existent item: " + item);
		}
		logs.put(item, log);
	}

	/**
	 * Get the specified log item.
	 */
	public Object getLog(String item) {
		if (!logs.containsKey(item)) {
			throw new IllegalArgumentException("Attempt to get log for non-existent item: " + item);
		}
		return logs.get(item);
	}

	/**
	 * Get the specified log item as a String, using the toString() method.
	 */
	public String getStringLog(String item) {
		return getLog(item).toString();
	}

	/**
	 * Get all the items and their corresponding log values (which may not be set).
	 */
	public Map<String, Object> getAllLogs() {
		return Collections.unmodifiableMap(logs);
	}

	/**
	 * Get all the items for which a log value has been set.
	 */
	public Map<String, Object> getSetLogs() {
		HashMap<String, Object> setLogs = new HashMap<>();
		for (Entry<String, Object> entry : logs.entrySet()) {
			if (entry.getValue() != null) {
				setLogs.put(entry.getKey(), entry.getValue());
			}
		}
		return setLogs;
	}
}

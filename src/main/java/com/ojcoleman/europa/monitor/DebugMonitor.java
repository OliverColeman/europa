package com.ojcoleman.europa.monitor;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.core.Monitor;
import com.ojcoleman.europa.util.Stringer;

/**
 * 
 * Implementation of {@link Monitor} that prints out debug information.
 * 
 * @see com.ojcoleman.europa.core.Stringable
 * 
 * @author O. J. Coleman
 */
public class DebugMonitor extends SelectiveMonitor {
	public DebugMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void log(Observable observed, Object event, Object state) {
		if (state != null) {
			String stateStr = Stringer.toString(state, 10, 1);
			if (stateStr.contains("\n")) {
				write(observed, event, state, observed.getClass().getSimpleName() + ": " + event + ":\n" + stateStr);
			}
			else {
				write(observed, event, state, observed.getClass().getSimpleName() + ": " + event + ": " + stateStr.trim());
			}
				
		} else {
			write(observed, event, state, observed.getClass().getSimpleName() + ": " + event);
		}
	}
}

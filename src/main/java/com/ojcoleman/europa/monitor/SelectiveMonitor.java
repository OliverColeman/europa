package com.ojcoleman.europa.monitor;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Monitor;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.util.Stringer;

/**
 * 
 * Base class for a {@link Monitor} that prints info for the user-specified classes and event names to the standard
 * output stream (console) and/or a file.
 * 
 * @author O. J. Coleman
 */
public abstract class SelectiveMonitor extends FileOrCLIMonitor {
	@Parameter(description = "The class to listen to events for, or leave blank to listen to all classes.", optional=true)
	protected Class<?> listenTo;

	@Parameter(description = "The names of events to print info for. This only works if the event object is an enum. Leave blank to listen to all events. This is case-insensitive.", defaultValue = "[]")
	protected String[] logEvents;
	
	@Parameter (description = "Only log events when the current iteration/generation is a factor of the iterationPeriod.", defaultValue = "1", minimumValue="1")
	protected int iterationPeriod;
	
	
	private HashSet<String> logEventsSet;
	private Run run;
	
	public SelectiveMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		logEventsSet = new HashSet<>();
		if (logEvents != null && logEvents.length > 0) {
			for (String event : logEvents) {
				logEventsSet.add(event.toLowerCase());
			}
		}
		
		run = this.getParentComponent(Run.class);
	}

	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (run.getCurrentIteration() % iterationPeriod == 0 && 
				(listenTo == null || listenTo.isAssignableFrom(observed.getClass())) && 
				(logEventsSet.isEmpty() || logEventsSet.contains(event.toString().toLowerCase()))) {
			log (observed, event, state);
		}
	}
	
	/**
	 * Called when an event has been selected to be logged.
	 * 
	 * @param observed The subject (the object being observed).
	 * @param event An object indicating the type of change or event, this is usually an {@Link java.lang.Enum} defined
	 *            by the Subject. If null is given then the Observer(s) will be passed
	 *            {@link Observable.Event.Unspecified}.
	 * @param state An arbitrary object typically describing the current state of the Observable or information about the event.
	 */
	protected abstract void log(Observable observed, Object event, Object state);
}

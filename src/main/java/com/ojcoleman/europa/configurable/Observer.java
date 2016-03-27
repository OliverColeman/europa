package com.ojcoleman.europa.configurable;

/**
 * Simple implementation of the Observer part of the Observer pattern.
 * 
 * @see Observable
 * 
 * @author O. J. Coleman
 */
public interface Observer extends java.util.EventListener {
	/**
	 * Called when the subject being observed triggers an event (generally when it has changed state in a significant
	 * way).
	 * 
	 * @param observed The subject (the object being observed).
	 * @param event An object indicating the type of change or event, this is usually an {@Link java.lang.Enum} defined
	 *            by the Subject. If null is given then the Observer(s) will be passed
	 *            {@link Observable.Event.Unspecified}.
	 * @param state An arbitrary object typically describing the current state of the Observable or information about the event.
	 */
	public void eventOccurred(Observable observed, Object event, Object state);
}

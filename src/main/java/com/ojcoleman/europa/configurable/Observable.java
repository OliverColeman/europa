package com.ojcoleman.europa.configurable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of the Subject part of the Observer pattern.
 * Sub-classes should override {@link EventType}.
 * @see Observer
 * 
 * @author O. J. Coleman
 */
public abstract class Observable {
	/**
	 * Basic event types.
	 */
	public static enum Event {
		/**
		 * An event type indicating the the Observable subject did not specify an event type.
		 */
		Unspecified 
	}
	
	private List<Observer> listeners = new ArrayList<Observer>();
	
	/**
	 * Add the given observer to this subject.
	 */
	public void addEventListener(Observer listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remove the given observer from this subject.
	 */
	public void removeEventListener(Observer listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifies all observers of the given event or change.
	 * 
	 * @param type The type of event or change. This should be tied to a constant defined in the Subject sub-class. 
	 * 
	 * @see Observer
	 * @see #addEventListener(Observer)
	 */
	protected void fireEvent(Object event) {
		if (event == null) {
			event = Event.Unspecified;
		}
		for (Observer listener : listeners) {
			listener.eventOccurred(this, event);
		}
	}
}

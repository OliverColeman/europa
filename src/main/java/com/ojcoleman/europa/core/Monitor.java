package com.ojcoleman.europa.core;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Observer;

/**
 * <p>Base class of classes that monitor the state of the evolutionary process.</p>
 * 
 * <p>Typically an {@link Observable} will call {@link Run#monitor(Observable)} so that it may be monitored.</p>
 * 
 * @author O. J. Coleman
 */
public abstract class Monitor extends ComponentBase implements Observer {
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Monitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
}
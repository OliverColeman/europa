package com.ojcoleman.europa.monitor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale.Category;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.eclipsesource.json.WriterConfig;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Monitor;
import com.ojcoleman.europa.core.Run;

/**
 * 
 * Implementation of {@link Monitor} that prints out the configuration for the Run in JSON format to a file called
 * nnConfig.json.
 * 
 * @author O. J. Coleman
 */
public class ConfigMonitor extends Monitor {
	public ConfigMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (event == Run.Event.Initialised) {
			try {
				Run run = this.getParentComponent(Run.class);
				FileWriter outputFile;
				outputFile = new FileWriter(run.getOutputDirectory().resolve("nnConfig.json").toFile());
				outputFile.append(run.getConfiguration(false).toString(WriterConfig.PRETTY_PRINT));
				outputFile.close();
			} catch (IOException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}

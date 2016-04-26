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
import com.ojcoleman.europa.configurable.Observer;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Monitor;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.util.Stringer;

/**
 * 
 * Base class for a {@link Monitor} that prints to the standard output stream (console) and/or a file.
 * 
 * @author O. J. Coleman
 */
public abstract class FileOrCLIMonitor extends Monitor {
	@Parameter(description = "The name of this monitor. Output to console will be prefixed with this. Optional.", defaultValue = "")
	protected String name;

	@Parameter(description = "Whether to print the output to the standard output stream.", defaultValue = "false")
	protected boolean printToConsole;

	@Parameter(description = "The name of the file(s) to print to, or leave blank to not print to any files. " + "The filename may contain tokens that will be replaced with values. " + "Valid tokens are %itr (iteration number), %observed (class name of observed object) and %event (event name, if an enum)", optional = true)
	protected String printToFile;

	private String previousFileName;
	private FileWriter outputFile;
	private Run run;

	public FileOrCLIMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		run = this.getParentComponent(Run.class);
	}

	/**
	 * Write the given CharSequence as a new line (for example a String) to the console and/or file according to the
	 * settings.
	 * 
	 * @param observed The subject (the object being observed). Provided for context, may be ignored.
	 * @param event An object indicating the type of change or event, this is usually an {@link java.lang.Enum} defined
	 *            by the Subject. If null is given then the Observer(s) will be passed
	 *            {@link com.ojcoleman.europa.configurable.Observable.Event#Unspecified}. Provided for context, may be ignored.
	 * @param state An arbitrary object typically describing the current state of the Observable or information about
	 *            the event. Provided for context, may be ignored.
	 * @param s The character sequence to write.
	 * 
	 * @see Observer#eventOccurred(Observable, Object, Object)
	 */
	protected void write(Observable observed, Object event, Object state, CharSequence s) {
		if (printToFile != null) {
			try {
				String eventName = event.getClass().isEnum() ? event.toString() : "";
				String newFileName = printToFile.replace("%itr", "" + run.getCurrentIteration()).replace("%observed", observed.getClass().getSimpleName()).replace("%event", eventName);

				if (previousFileName == null || !previousFileName.equals(newFileName)) {
					if (outputFile != null) {
						outputFile.close();
					}

					outputFile = new FileWriter(run.getOutputDirectory().resolve(newFileName).toFile());

					previousFileName = newFileName;
				}

				outputFile.append(s);
				outputFile.append("\n");
				outputFile.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (printToConsole) {
			if (!name.equals("")) {
				System.out.print(name + ": ");
			}
			System.out.println(s);
		}
	}
}

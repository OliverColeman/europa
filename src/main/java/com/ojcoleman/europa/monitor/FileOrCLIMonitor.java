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
import com.ojcoleman.europa.util.StructuredStringableStringer;

/**
 * 
 * Base class for a {@link Monitor} that prints to the standard output stream (console) and/or a file.
 * 
 * @author O. J. Coleman
 */
public abstract class FileOrCLIMonitor extends Monitor {
	@Parameter(description = "The name of this monitor. Output to console will be prefixed with this. Optional.", defaultValue="")
	protected String name;
	
	@Parameter(description = "Whether to print the output to the standard output stream.", defaultValue="false")
	protected boolean printToConsole;

	@Parameter(description = "The name of the file to print to, or leave blank to not print to file.", optional=true)
	protected String printToFile;
	
	FileWriter outputFile;

	public FileOrCLIMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		if (printToFile != null) {
			outputFile = new FileWriter(this.getParentComponent(Run.class).getOutputDirectory().resolve(printToFile).toFile());
		}
	}
	
	/**
	 * Write the given CharSequence (for example a String) to the console and/or file according to the settings.
	 */
	protected void write(CharSequence s) {
		if (outputFile != null) {
			try {
				outputFile.append(s);
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

package com.ojcoleman.europa;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Observer;
import com.ojcoleman.europa.core.Run;
import com.thoughtworks.xstream.XStream;

/**
 * Accepts command line arguments and launches a Run. Do something like <code>Java -jar [jarfile]</code> to see
 * available options.
 * 
 * @author O. J. Coleman
 */
public class Main {
	@Parameter(description = "<Configuration file (.json extension) or previously saved run (.europa extension)>")
	List<String> configOrSavedRun;

	@Parameter(names = "--printConfig", description = "Prints an example configuration file showing available parameters and default sub-components. May be combined with a custom input configuration file or snapshot to see options for custom components or the configuration of the snapshot.")
	private boolean printConfig = false;

	@Parameter(names = "--noMetadata", description = "If printConfig is provided, disables including the metadata for parameters and components.")
	private boolean noMetadata = false;

	@Parameter(names = "--strictJSON", description = "If printConfig is provided, disables \"commenting out\" the metadata in the JSON output, thus producing valid JSON. Metadata about parameters and components is commented out by default to improve readability. Note that this program will accept configuration files with comments.")
	private boolean strictJSON = false;

	public static void main(String[] args) {
		try {
			Main main = new Main();
			JCommander jcom = new JCommander(main, args);

			String configOrSavedRun = null;

			if (main.configOrSavedRun != null) {
				if (main.configOrSavedRun.size() != 1) {
					jcom.usage();
					System.exit(-1);
				}
				configOrSavedRun = main.configOrSavedRun.get(0);
			}

			if (main.printConfig) {
				if (configOrSavedRun == null) {
					main.printConfigOptionsFromDefault();
				} else if (configOrSavedRun.endsWith(".json")) {
					main.printConfigOptionsFromConfig(configOrSavedRun);
				} else if (configOrSavedRun.endsWith(".europa")) {
					main.printConfigOptionsFromSaved(configOrSavedRun);
				} else {
					jcom.usage();
					System.exit(-1);
				}
			} else {
				if (main.configOrSavedRun == null) {
					jcom.usage();
					System.exit(-1);
				}

				if (configOrSavedRun.endsWith(".json")) {
					main.launchFromConfig(configOrSavedRun);
				} else if (configOrSavedRun.endsWith(".europa")) {
					main.launchFromSaved(configOrSavedRun);
				} else {
					jcom.usage();
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized void launchFromSaved(String configOrSavedRun) throws Exception {
		XStream xstream = new XStream();
		xstream.setMarshallingStrategy(null);
		Base Base = (Base) xstream.fromXML(new FileReader(configOrSavedRun));
		Base.run.run();
	}

	private synchronized void launchFromConfig(String configOrSavedRun) throws Exception {
		JsonObject config = Json.parse(new FileReader(configOrSavedRun)).asObject();

		config.add("configFilePath", configOrSavedRun);

		Base base = new Base(null, config);
		base.run.run();
	}

	private synchronized void launch(final Base base) throws Exception {
		base.run.addEventListener(new Observer() {
			@Override
			public void eventOccurred(Observable observed, Object event) {
				if (event == Run.Event.IterationComplete) {
					if (base.saveFrequency > 0 && base.run.getCurrentIteration() % base.saveFrequency == 0) {
						XStream xstream = new XStream();
						xstream.setMarshallingStrategy(null);
						try {
							xstream.toXML(base, new FileWriter(base.savePath));
						} catch (IOException e) {
							base.logger.error("Could not save Run state to file.", e);
						}
					}
				}
			}

		});

		base.run.run();
	}

	private void printConfigOptionsFromSaved(String configOrSavedRun) throws Exception {
		XStream xstream = new XStream();
		xstream.setMarshallingStrategy(null);
		Base base = (Base) xstream.fromXML(new FileReader(configOrSavedRun));
		JsonObject config = base.getConfiguration(false);
		System.out.println(config.toString(WriterConfig.PRETTY_PRINT));

	}

	private void printConfigOptionsFromConfig(String configFile) throws Exception {
		List<String> configLines = Files.readAllLines(Paths.get(configFile), StandardCharsets.UTF_8);
		StringBuffer config = new StringBuffer();
		for (String line : configLines) {
			if (!line.matches("^\\s*//.*$")) {
				config.append(line);
			}
		}
		String configStr = config.toString();

		// Remove commas before closing brace (occurs as an artifact of the commenting process).
		configStr = Pattern.compile(",\\s*}").matcher(configStr).replaceAll(" }");
		
		JsonObject configObject = Json.parse(configStr).asObject();
		configObject.add("_isDummy", true);

		Base base = new Base(null, configObject);
		printConfig(base);
	}

	private void printConfigOptionsFromDefault() throws Exception {
		JsonObject configObject = new JsonObject();
		configObject.add("_isDummy", true);
		Base base = new Base(null, configObject);
		printConfig(base);
	}

	private void printConfig(Base base) throws Exception {
		JsonObject configJO = base.getConfiguration(!noMetadata);
		String configOut = configJO.toString(WriterConfig.PRETTY_PRINT);

		if (!strictJSON) {
			// Comment out meta data. This produces totally illegal JSON. However they totally should have have included
			// comments in the standard.
			configOut = Pattern.compile("^(\\s*)\"_metadata(?:.*?) ([^\"]*)\"\\s*:\\s*\"?(.*?)\"?,?$", Pattern.MULTILINE).matcher(configOut).replaceAll("$1// $2: $3");
			// Blank lines.
			configOut = Pattern.compile("^\\s*//\\s*:\\s*(\"\")?\\s*$", Pattern.MULTILINE).matcher(configOut).replaceAll(""); 
		}

		System.out.println(configOut);
	}
}

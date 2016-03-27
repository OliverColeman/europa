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
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.eclipsesource.json.WriterConfig;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.DefaultIDFactory;
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

			if (main.printConfig) {
				if (main.configOrSavedRun == null || main.configOrSavedRun.isEmpty()) {
					main.printConfigOptionsFromDefault();
				} else if (main.configOrSavedRun.get(0).endsWith(".json")) {
					main.printConfigOptionsFromConfig(mergeConfigs(main.configOrSavedRun));
				} else if (main.configOrSavedRun.get(0).endsWith(".europa")) {
					main.printConfigOptionsFromSaved(main.configOrSavedRun.get(0));
				} else {
					jcom.usage();
					System.exit(-1);
				}
			} else {
				if (main.configOrSavedRun == null) {
					jcom.usage();
					System.exit(-1);
				}

				if (main.configOrSavedRun.get(0).endsWith(".json")) {
					main.launchFromConfig(mergeConfigs(main.configOrSavedRun));
				} else if (main.configOrSavedRun.get(0).endsWith(".europa")) {
					main.launchFromSaved(main.configOrSavedRun.get(0));
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
		
		Run run = (Run) xstream.fromXML(new FileReader(configOrSavedRun));
		run.run();
	}
	
	private synchronized void launchFromConfig(JsonObject config) throws Exception {
		JsonArray configFilePaths = new JsonArray();
		for (String confPath : configOrSavedRun) {
			configFilePaths.add(confPath);
		}
		config.add("configFilePaths", configFilePaths);
		
		Run run = new Run(null, new Configuration(config, false, new DefaultIDFactory()));
		run.run();
	}
	
	private synchronized void launch(final Run run) throws Exception {
		run.run();
	}

	private void printConfigOptionsFromSaved(String configOrSavedRun) throws Exception {
		XStream xstream = new XStream();
		xstream.setMarshallingStrategy(null);
		Run run = (Run) xstream.fromXML(new FileReader(configOrSavedRun));
		printConfig(run);
	}

	private void printConfigOptionsFromConfig(JsonObject configObject) throws Exception {
		Run run = new Run(null, new Configuration(configObject, true, new DefaultIDFactory()));
		printConfig(run);
	}

	private void printConfigOptionsFromDefault() throws Exception {
		JsonObject configObject = new JsonObject();
		
		Run run = new Run(null, new Configuration(configObject, true, new DefaultIDFactory()));
		printConfig(run);
	}

	private void printConfig(Run run) throws Exception {
		JsonObject configJO = run.getConfiguration(!noMetadata);
		String configOut = configJO.toString(WriterConfig.PRETTY_PRINT);

		if (!strictJSON) {
			// Comment out meta data. This produces totally illegal JSON. However they totally should have have included
			// comments in the standard.
			configOut = Pattern.compile("^(\\s*)\"_metadata(?:.*?) ([^\"]*)\"\\s*:\\s*\"?(.*?)\"?,?$", Pattern.MULTILINE).matcher(configOut).replaceAll("$1// $2: $3");
			// Blank lines.
			configOut = Pattern.compile("^\\s*//\\s*:\\s*(\"\")?\\s*$", Pattern.MULTILINE).matcher(configOut).replaceAll(""); 
		}
		
		// Replace \" with " in comment lines.
		StringBuilder configOutSB = new StringBuilder(configOut.length());
		Pattern comment = Pattern.compile("^\\s*//.*");
		for (String line : configOut.split("\n")) {
			if (comment.matcher(line).matches()) {
				configOutSB.append(line.replace("\\\"", "\""));
			} else {
				configOutSB.append(line);
			}
			configOutSB.append("\n");
		}
		
		System.out.println(configOutSB.toString());
	}
	
	private static String stripComments(String jsonWithComments) {
		List<String> jsonLines = Arrays.asList(jsonWithComments.split("\n"));
		return stripComments(jsonLines);
	}
	
	private static String stripComments(List<String> jsonWithComments) {
		StringBuffer config = new StringBuffer();
		for (String line : jsonWithComments) {
			if (!line.matches("^\\s*//.*$")) {
				config.append(line + "\n");
			}
		}
		String json = config.toString();

		// Remove commas before closing brace (occurs as an artifact of the commenting process).
		json = Pattern.compile(",\\s*}").matcher(json).replaceAll(" }");
		
		return json;
	}
	
	private static JsonObject mergeConfigs(List<String> configPaths) throws IOException {
		JsonObject mergedConfig = new JsonObject();
		
		for (String confPath : configPaths) {
			List<String> configLines = Files.readAllLines(Paths.get(confPath), StandardCharsets.UTF_8);
			String configStr = stripComments(configLines);
			try {
				JsonObject config = Json.parse(configStr).asObject();

				recursiveMerge(mergedConfig, config);
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse configuration file " + confPath, ex);
			}
		}
		
		return mergedConfig;
	}
	
	/**
	 * Recursively merge o2 into o1. Values in o2 overwrite those in o1 if set.
	 */
	private static void recursiveMerge(JsonObject o1, JsonObject o2) {
		for (String key : o2.names()) {
			JsonValue val2 = o2.get(key);
			// If o1 does not define this value or the value is not an object then replace/set the value in o1.
			if (o1.get(key) == null || !o1.get(key).isObject()) {
				o1.set(key, val2);
			}
			else if (o1.get(key).isObject()) {
				// Otherwise recursively merge the object values.
				JsonObject merged = o1.get(key).asObject();
				recursiveMerge(merged, val2.asObject());
				o1.set(key, merged);
			}
		}
	}
}

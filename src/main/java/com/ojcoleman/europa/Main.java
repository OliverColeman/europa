package com.ojcoleman.europa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.ParserException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.eclipsesource.json.WriterConfig;
import com.ojcoleman.collections.LSListMap;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.DefaultIDFactory;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.util.ArrayUtil;
import com.thoughtworks.xstream.XStream;

/**
 * Accepts command line arguments and launches a Run. Do something like <code>Java -jar [jarfile]</code> to see
 * available options.
 * 
 * @author O. J. Coleman
 */
public class Main {
	@Parameter(description = "<Configuration file (.json or .yaml extension) or previously saved run (.europa extension)>")
	List<String> configOrSavedRun;

	@Parameter(names = "--printConfig", description = "Prints an example configuration file showing available parameters and default sub-components. May be combined with a custom input configuration file or snapshot to see options for custom components or the configuration of the snapshot.")
	private boolean printConfig = false;

	@Parameter(names = "--printFormat", description = "The format to print the configuration in, either 'json' or 'yaml'. Deault is 'yaml'")
	private String printFormat = "yaml";
	
	@Parameter(names = "--noMetadata", description = "If printConfig is provided, disables including the metadata for parameters and components.")
	private boolean noMetadata = false;

	@Parameter(names = "--strictJSON", description = "If printConfig is provided and printFormat is 'json', disables \"commenting out\" the metadata in the JSON output, thus producing valid JSON. Metadata about parameters and components is commented out by default to improve readability. Note that this program will accept configuration files with comments.")
	private boolean strictJSON = false;

	@Parameter(names = "--runCount", description = "The number of runs to perform. This is only applicable when launching from a configuration, not a saved run. Default is 1.")
	private int runCount = 1;

	public static void main(String[] args) {
		try {
			Main main = new Main();
			JCommander jcom = new JCommander(main, args);
			
			boolean firstFileIsEmpty = main.configOrSavedRun == null || main.configOrSavedRun.isEmpty();
			String firstFile = firstFileIsEmpty ? null : main.configOrSavedRun.get(0);
			boolean firstFileIsSavedRun = firstFile == null ? false : firstFile.endsWith(".europa");
			boolean firstFileIsConfig = firstFile == null ? false : (firstFile.endsWith(".json") || firstFile.endsWith(".yaml"));
			
			if (main.printConfig) {
				if (firstFileIsEmpty) {
					main.printConfigOptionsFromDefault();
				} else if (firstFileIsConfig) {
					main.printConfigOptionsFromConfig(mergeConfigs(main.configOrSavedRun));
				} else if (firstFileIsSavedRun) {
					main.printConfigOptionsFromSaved(firstFile);
				} else {
					jcom.usage();
					System.exit(-1);
				}
			} else {
				if (firstFileIsEmpty) {
					jcom.usage();
					System.exit(-1);
				}
				
				if (firstFileIsConfig) {
					main.launchFromConfig(mergeConfigs(main.configOrSavedRun));
				} else if (firstFileIsSavedRun) {
					main.launchFromSaved(firstFile);
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
		ZipFile zipFile = new ZipFile(configOrSavedRun);
		ZipEntry zipEntry = zipFile.getEntry("europarun.xml");
		BufferedInputStream is = new BufferedInputStream (zipFile.getInputStream(zipEntry));

		XStream xstream = new XStream();
		Run run = (Run) xstream.fromXML(is);
		
		zipFile.close();
		
		run.run();
	}

	private synchronized void launchFromConfig(JsonObject config) throws Exception {
		JsonArray configFilePaths = new JsonArray();
		for (String confPath : configOrSavedRun) {
			configFilePaths.add(confPath);
		}
		
		config.set("configFilePaths", configFilePaths);
		
		String outputDir = getUniqueOutputDirectory(config);
		
		config.set("runCount", runCount);
		int solvedCount = 0;
		double[] performances = new double[runCount];
		EvaluationDescription evDesc = null;
		for (int runIndex = 0; runIndex < runCount; runIndex++) {
			config.set("runIndex", runIndex);
			if (runCount > 1) {
				config.set("outputDirectory", outputDir + File.separator + runIndex);
			}
			
			Run run = new Run(null, new Configuration(config, false, new DefaultIDFactory()));
			
			run.run();
			
			if (run.getCurrentIteration() < run.getMaximumIterations()) {
				solvedCount++;
			}
			
			evDesc = getEvalDescription(run);
			
			if (evDesc.isPerformanceIndicator) {
				performances[runIndex] = run.getPopulation().getBestPerforming().evaluationData.getPerformanceResults().get(evDesc);
			}
			else {
				performances[runIndex] = run.getPopulation().getFittest().evaluationData.getFitnessResults().get(evDesc);
			}
		}
		
		double avgPerf = ArrayUtil.mean(performances);
		double sdPerf = ArrayUtil.getStdDev(performances);
		System.out.println("Solved count: " + solvedCount);
		System.out.println(evDesc.name + "\n\tmean: " + avgPerf + "\n\tstd. dev.:" + sdPerf + "\n\tall: " + Arrays.toString(performances));
		
	}

	private synchronized void launch(final Run run) throws Exception {
		run.run();
	}

	private void printConfigOptionsFromSaved(String configOrSavedRun) throws Exception {
		FileInputStream fis = new FileInputStream(configOrSavedRun);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		zis.getNextEntry();
		
		XStream xstream = new XStream();
		
		Run run = (Run) xstream.fromXML(zis);
		
		zis.close();
		
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
		
		if (printFormat.equals("json")) { 
			String configOut = configJO.toString(WriterConfig.PRETTY_PRINT);
			
			if (!strictJSON) {
				// Comment out meta data. This produces totally illegal JSON. However they totally should have have included
				// comments in the standard.
				configOut = Pattern.compile("^(\\s*)\"_metadata<[^>]*>([^\"]*)\"\\s*:\\s*\"?(.*?)\"?,?$", Pattern.MULTILINE).matcher(configOut).replaceAll("$1//$2: $3");
				// Blank lines.
				configOut = Pattern.compile("^(\\s*)//\\s*:\\s*(\"\")?\\s*$", Pattern.MULTILINE).matcher(configOut).replaceAll("$1");
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
		else {
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		    dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		    dumperOptions.setSplitLines(false);
		    dumperOptions.setPrettyFlow(true);
		    Yaml yaml = new Yaml(dumperOptions);
			
			String configOut = yaml.dump(convertConfigToMapsAndLists(configJO));
			
			// Convert meta data elements to comments.
			configOut = Pattern.compile("^( *)'?_metadata<[^>]*>([^':]*)'?: *('?)(.*)(\\3)$", Pattern.MULTILINE).matcher(configOut).replaceAll("$1#$2: $4");
			// Blank lines.
			configOut = Pattern.compile("^( *)# *: *$", Pattern.MULTILINE).matcher(configOut).replaceAll("$1");
			
			System.out.println(configOut);
		}
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

	/**
	 * Merge the given JSON files together into one JsonObject. Latter values override former values.
	 */ 
	public static JsonObject mergeConfigs(List<String> configPaths) throws IOException {
		JsonObject mergedConfig = new JsonObject();

		for (String confPath : configPaths) {
			try {
				JsonObject config = null;
	
				if (confPath.endsWith(".json")) {
					List<String> configLines = Files.readAllLines(Paths.get(confPath), StandardCharsets.UTF_8);
					String configStr = stripComments(configLines);
					config = Json.parse(configStr).asObject();
				}
				
				else if (confPath.endsWith(".yaml")) {
					Yaml yaml = new Yaml();
				    config = convertConfigFromMapsAndLists(yaml.load(new FileInputStream(confPath))).asObject();
				}
				
				else {
					throw new IllegalArgumentException("Invalid extension on configuration file " + confPath + ". Must be .yaml or .json.");
				}
				
				recursiveMerge(mergedConfig, config);
			}
			catch (ParseException | ParserException ex) {
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
			} else if (o1.get(key).isObject()) {
				// Otherwise recursively merge the object values.
				JsonObject merged = o1.get(key).asObject();
				recursiveMerge(merged, val2.asObject());
				o1.set(key, merged);
			}
		}
	}
	

	private String getUniqueOutputDirectory(JsonObject config) throws IOException {
		String outputDir = config.getString("outputDirectory", null);
		if (outputDir == null) {
			String name = config.getString("name", null);;
			if (name == null) {
				name = Run.getDefaultName(config);
			}
			outputDir = Run.getDefaultOutputDirectory(name);
		}
		outputDir = Run.getUniqueDirectoryName(outputDir);
		return outputDir;
	}
	
	
	/**
	 * Determine which performance (or fitness) evaluation to use.
	 * Use (first) performance indicator from primary evaluator if available.
	 * Otherwise the first fitness indicator from the (most) primary evaluator is used.
	 */
	private EvaluationDescription getEvalDescription(Run run) {
		EvaluationDescription fitnessEvDesc = null;
		
		for (Evaluator ev : run.getEvaluators()) {
			for (EvaluationDescription desc : ev.getEvaluationDescriptions()) {
				if (desc.isPerformanceIndicator) {
					return desc;
				}
				else if (fitnessEvDesc == null) {
					fitnessEvDesc = desc;
				}
			}
		}
		return fitnessEvDesc;
	}
	
	
	/**
	 * Convert a configuration specified in regular Java Maps and Lists.
	 * This is used to convert a configuration file loaded via SnakeYAML.
	 */  
	static private JsonValue convertConfigFromMapsAndLists(Object c) {
		if (c instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) c;
			JsonObject jo = new JsonObject();
			map.forEach((k, v) -> {
				jo.add(k, convertConfigFromMapsAndLists(v));
			});
			return jo;
		}
		else if (c instanceof List) {
			List<Object> list = (List<Object>) c;
			JsonArray ja = new JsonArray();
			list.forEach(v -> {
				ja.add(convertConfigFromMapsAndLists(v));
			});
			return ja;
		}
		else {
			if (c instanceof Boolean) return Json.value((Boolean) c);
			if (c instanceof Integer) return Json.value((Integer) c);
			if (c instanceof Long) return Json.value((Long) c);
			if (c instanceof Float) return Json.value((Float) c);
			if (c instanceof Double) return Json.value((Double) c);
			return Json.value(c.toString());
		}
	}
	

	/**
	 * Convert a configuration to regular Java Maps and Lists.
	 * This is used to convert a configuration to YAML via SnakeYAML.
	 */  
	static private Object convertConfigToMapsAndLists(JsonValue c) {
		if (c.isObject()) {
			Map<String, Object> map = new LSListMap<>();
			JsonObject jo = c.asObject();
			jo.names().forEach(k -> {
				map.put(k, convertConfigToMapsAndLists(jo.get(k)));
			});
			return map;
		}
		else if (c.isArray()) {
			JsonArray ja = c.asArray();
			List<Object> list = new ArrayList<>(ja.size());
			ja.forEach(v -> {
				list.add(convertConfigToMapsAndLists(v));
			});
			return list;
		}
		else {
			if (c.isNumber()) {
				try {
					return c.asLong();
				}
				catch (NumberFormatException e) {}
				return c.asDouble();
			}
			if (c.isBoolean()) return c.asBoolean();
			if (c.isString()) return c.asString();
			return c.toString();
		}
	}
}

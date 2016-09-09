package com.ojcoleman.europa.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.slf4j.Logger;

import com.beust.jcommander.JCommander;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.google.common.collect.Lists;
import com.eclipsesource.json.JsonValue;
import com.ojcoleman.europa.Main;
import com.ojcoleman.europa.configurable.ConfigurableBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.DefaultIDFactory;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Run;

/**
 * <p>
 * Attempts to fine tune one or more {@link Parameter}s for an experiment described by a JSON configuration file. Parameters are
 * adjusted iteratively, with each property adjusted in turn each iteration. The underlying assumption is that the
 * fitness landscape represented by the parameters to adjust is unimodal.
 * </p>
 * <p>
 * For each property for each iteration the fitness value is calculated by adjusting the property up or down by a
 * multiplicative factor and running the experiment. If either of these adjustments results in a higher fitness then the
 * adjusted value will be adopted (at least until the next iteration when a new value may be adopted).
 * </p>
 * <p>
 * The default initial multiplicative factor is 2 (thus the initial downward adjustment of a property p is p/2 and the
 * upward adjustment is p*2). A different initial multiplicative factor may be specified in the configuration file. For each
 * iteration in which no property adjustments are adopted the multiplicative factor is halved.
 * </p>
 * <p>
 * If fitness was not increased by adjusting a property in the previous iteration then it will not be adjusted in the
 * current iteration, to save time. If adjusting the property in the next iteration still doesn't yield an increase in
 * fitness then the property will be ignored for the next two iterations, and so on.
 * </p>
 * <p>
 * To specify that Parameters should be tuned add an array labelled "parameterTuning" to each object containing the 
 * parameters to tune, "parameterTuning" lists objects that describe which parameters to adjust and how to adjust them.
 * for example:
 * <code>
 * {
 *	"transcriber":{
 *		...
 *		"population":{
 *			...
 *			"evolver":{
 *              ...
 *				"relativeCloneProportion":0.25,
 *				"parentsProportion":0.2,
 *				"elitismProportion":0.1,
 *				"elitismMax":1,
 *				"speciesFitnessSharing":true,
 *
 *				"parameterTuning": {
 *					"relativeCloneProportion": {  [key is the name of the Parameter to tune]
 *						"min": 0,  [the minimum value to increase to, if not specified it will be taken from the Parameter definition if available]
 *						"max": 1,  [the maximum value to decrease to, if not specified it will be taken from the Parameter definition if available]
 *						"magnitude": 1.5,  [the initial multiplicative factor or delta, defaults to 2 or 1 respectively]
 *						"adjustType": "factor"  [Either "factor" or "delta", default is "factor"]
 *					},
 *					"speciesFitnessSharing": {  [parameters of type boolean, integer/long and float/double may be tuned.]
 *						"order": 1  [to tune some parameters before others give them a lower value for 'order', the default value is 100]
 *					}
 *					...
 *				}
 *			}
 *			...
 * }
 */
public class ParameterTuner implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final DecimalFormat nf6 = new DecimalFormat("0.00000");
	private static final DecimalFormat nf2 = new DecimalFormat("0.00");
	private static final DateFormat df = new SimpleDateFormat("HH:mm:ss");
	
	@com.beust.jcommander.Parameter(description = "<Configuration files (.json extension)> or a checkpoint file (.europapt extenstion)..")
	List<String> configArg;

	@com.beust.jcommander.Parameter(names = "--minRunCount", description = "The number of runs to perform when determining fitness for a set of Parameter values. Default is 100.")
	private int runCount = 100;

	@com.beust.jcommander.Parameter(names = "--maxRunCount", description = "The maximum number of runs to perform when determining fitness for a set of Parameter values. Default is 200.")
	private int maxRunCount = 200;

	@com.beust.jcommander.Parameter(names = "--maxIterations", description = "The maximum number of tuning iterations to perform. Default is 500.")
	private int maxIterations = 200;
	
	@com.beust.jcommander.Parameter(names = "--out", description = "Directory to save output to. If resuming from a checkpoint file the default is the checkpoint files directory, otherwise default is './ptout_<timestamp>'.")
	private String output = null;
		
	
	private JsonObject config;
	private List<Param> parameters;
	private Result bestResult;
	private int iteration;
	private int property;
	private int stagnantCount = 0;
	private boolean finished;
	
	public static void main(String[] args) {
		// Disable all logging produced by Run et al except errors.
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
		
		ParameterTuner pt = new ParameterTuner();
		JCommander jcom = new JCommander(pt, args);

		if (pt.configArg == null || pt.configArg.isEmpty()) {
			jcom.usage();
			System.exit(0);
		}
		
		if (pt.configArg.get(0).endsWith(".europapt")) {
			File checkPointFile = new File(pt.configArg.get(0));
			
			if (!checkPointFile.exists() || !checkPointFile.isFile()) {
				System.out.println("Specified checkpoint file is invalid.");
				System.exit(1);
			}
				
			try {
				if (pt.output != null) {
					checkOutDir(pt.output);
				}
				
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(checkPointFile));
				ParameterTuner resumed = (ParameterTuner) ois.readObject();
				ois.close();
				resumed.output = (pt.output == null) ? checkPointFile.getParent() : pt.output;
				resumed.go(true);
				System.exit(0);
			} catch (Exception e) {
				System.err.println("Unable to resume from check point.");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		if (pt.output == null) {
			pt.output  = "./ptout_" + System.currentTimeMillis();
		}
		checkOutDir(pt.output);
		
		pt.go(false);
	}
	
	
	private static void checkOutDir(String output) {
		Path outDir = Paths.get(output);
		
		if (outDir.toFile().exists()) {
			if (!outDir.toFile().isDirectory()) {
				System.err.println("Specified output directory, " + outDir + ", already exists and is a file, aborting.");
				System.exit(1);
			}
			try {
				if (Files.newDirectoryStream(outDir).iterator().hasNext()) {
					System.err.println("Specified output directory, " + outDir + ", already exists and is non-empty, aborting.");
					System.exit(1);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		else {
			if (!outDir.toFile().mkdirs()) {
				System.err.println("Unable to create output directory, " + outDir.toAbsolutePath().toString() + ", aborting.");
				System.exit(1);
			}
		}
	}
	
	
	public void go(boolean resume) {
		try {
			BufferedWriter resultFile;

			if (config == null) {
				config = Main.mergeConfigs(configArg);
			}
			
			// Prevent output.
			config.set("monitors", new JsonObject());
			
			// Get the list of fields to tune. We have to do this even if resuming because we can't serialise Field in the Param class.
			// Create a temporary run to get Fields from.
			Run run = new Run(null, new Configuration(config, false, new DefaultIDFactory()));
			List<Param> parametersTemp = new ArrayList<>();
			getParams(config, "", run, parametersTemp);
			Collections.sort(parametersTemp);
			
			Path outDir = Paths.get(output);
			
			MannWhitneyUTest uTest = new MannWhitneyUTest();
			
			if (!resume) {
				parameters = parametersTemp;
				
				// Determine which performance (or fitness) evaluation to use.
				getEvalDescription(run, true);
				
				resultFile = new BufferedWriter(new FileWriter(outDir.resolve("results.csv").toFile()));
				resultFile.append("iteration, tuned property, ");
				for (Param p : parameters) {
					resultFile.append(p.toString() + ", ");
				}
				resultFile.append("iterations, mean perf\n");
				
				System.out.println("Initial values:");
				for (Param p : parameters) {
					System.out.println("  " + p + "=" + p.currentBest);
				}
				
				bestResult = new Result("initial", getCurrentBest());
				bestResult.doRuns(runCount);
				System.out.println("Initial performance: " + nf6.format(bestResult.performance()));
				addResult(bestResult, resultFile);
				
				iteration = 1;
			}
			else { //resume from previous checkpoint
				System.out.println("Resuming from iteration " + iteration + ", property " + parameters.get(property).name);
				
				System.out.println("  Previous best result was " + bestResult + " using values:");
				for (Param p : parameters) {
					System.out.println("  " + p + "=" + p.currentBest);
				}
				for (Param p : parameters) {
					System.out.println("  Value adjust amount for " + p.name + " is " + nf6.format(p.adjustAmountOrFactor));
				}
				System.out.println("\n");
				
				// Repopulate Field in Param class.
				for (int p = 0; p < parameters.size(); p++) {
					parameters.get(0).field = parametersTemp.get(p).field;
				}
				
				resultFile = new BufferedWriter(new FileWriter(outDir.resolve("results.csv").toFile(), true));
			}
			
			// Start/resume tuning.
			for ( ; iteration <= maxIterations; iteration++) {
				System.out.println("Start iteration " + iteration);
				
				boolean adjustedAnyParams = false;
				boolean adjustCountDownZeroExists = false; // Indicates if any parameters are due to be adjusted this iteration.
				boolean noImprovement = true;
				boolean triedAtLeastOneParamAdjustment = false;
				
				// Adjust each property in turn.
				if (!resume) property = 0; // If we're resuming then continue on the property we left off at.
				for (; property < parameters.size(); property++) {
					// If we're not resuming create a checkpoint.
					if (!resume) {
						ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outDir.resolve("checkpoint.europapt").toFile()));
						oos.writeObject(this);
						oos.close();
					}
					resume = false;
					
					Param param = parameters.get(property);
					
					// Sample performance/fitness either side of current property value.
					if (param.adjustCountDown == 0) {
						System.out.println("\tTuning " + param + " (current value is " + param.currentBest + "). ");
						
						Param.Value[] variations = param.currentBest.variations();
						int varCount = variations.length;
						
						for (int var = 0; var < varCount; var++) {
							System.out.println("\t\tTrying value: " + variations[var] + ".");
						}
						
						Param.Value newVal = null;
						
						long start = System.currentTimeMillis();
						for (int var = 0; var < varCount; var++) {
							Param.Value[] params = getCurrentBest();
							params[property] = variations[var];

							Result adjustResult = new Result(iteration + "." + param.name + "=" + variations[var].toString(), params);
							adjustResult.doRuns(runCount);
							
							double p = uTest.mannWhitneyUTest(adjustResult.performances, bestResult.performances);
							
							System.out.println("\n\t\tValue " + variations[var] +  " gave " + adjustResult + " (p=" + nf2.format(p) + ").");
							
							// If/while this result looks like it might be better than the current best by 2% but the 
							// significance is insufficient (p >= 0.05), and the significance is not hopelessly low 
							// (p < 0.5), and we haven't reached the maximum run count,
							// do some more runs to try to increase significance.
							while (adjustResult.performance() > bestResult.performance() + 0.02 && p >= 0.05 && p < 0.5 && 
									(bestResult.sampleSize() < maxRunCount || adjustResult.sampleSize() < maxRunCount)) {
								
								if (bestResult.sampleSize() < adjustResult.sampleSize()) {
									bestResult.doRuns(adjustResult.sampleSize() - bestResult.sampleSize());
									p = uTest.mannWhitneyUTest(adjustResult.performances, bestResult.performances);
									System.out.println("\n\t\t\t" + bestResult + " (p=" + nf2.format(p) + ").");
								}
								else if (adjustResult.sampleSize() < bestResult.sampleSize()) {
									adjustResult.doRuns(bestResult.sampleSize() - adjustResult.sampleSize());
									p = uTest.mannWhitneyUTest(adjustResult.performances, bestResult.performances);
									System.out.println("\n\t\t\t" + adjustResult + " (p=" + nf2.format(p) + ").");
								}
								else {
									int increment = maxRunCount / 5;
									increment = Math.min(maxRunCount - bestResult.sampleSize(), increment);
									
									bestResult.doRuns(increment);
									p = uTest.mannWhitneyUTest(adjustResult.performances, bestResult.performances);
									System.out.println("\n\t\t\t" + bestResult + " (p=" + nf2.format(p) + ").");
									
									adjustResult.doRuns(increment);
									p = uTest.mannWhitneyUTest(adjustResult.performances, bestResult.performances);
									System.out.println("\n\t\t\t" + adjustResult + " (p=" + nf2.format(p) + ").");
								}
							}
							
							boolean better = p < 0.05 && (adjustResult.solvedPercent() > bestResult.solvedPercent() || adjustResult.solvedPercent() == bestResult.solvedPercent() && adjustResult.performance() > bestResult.performance());
							
							if (better) {
								System.out.println("\n\t\tBETTER THAN CURRENT BEST.");
								bestResult = adjustResult;
								newVal = variations[var];
							}
							
							addResult(adjustResult, resultFile);
							
							triedAtLeastOneParamAdjustment = true;
						}
						
						long finish = System.currentTimeMillis();
						
						System.out.println("\t\tTook " + DurationFormatUtils.formatPeriod(start, finish, "d:HH:mm:ss") + ".\n");
						
						resultFile.append("\n");
						
						// If the fitness was increased by an adjustment.
						if (newVal != null) {
							param.adjustIneffectiveCount = 0;
							param.currentBest = newVal;
							adjustedAnyParams = true;
							noImprovement = false;
						} else {
							// If the fitness did not improve by adjusting this property then hold off adjusting it
							// for a few iterations (dependent on how many times adjusting it has been ineffective in
							// previous generations).
							param.adjustIneffectiveCount++;
							param.adjustCountDown = param.adjustIneffectiveCount;

						}
					} 
					else {
						param.adjustCountDown--;
						System.out.println("\tSkipping " + param + " for " + param.adjustCountDown + " more iterations.");
					}
					
					adjustCountDownZeroExists |= param.adjustCountDown == 0;
				}
				
				resultFile.append("\n");

				System.out.println("\nFinished iteration. Best result is " + bestResult + ". Current best values are:");
				for (Param p : parameters) {
					System.out.println("  " + p + "=" + p.currentBest);
				}
				
				if (!adjustedAnyParams) {
					System.out.println();
					for (Param p : parameters) {
						if (p.reduceAdjustAmountOrFactor())
							System.out.println("Value adjust amount for " + p + " is now " + nf6.format(p.adjustAmountOrFactor));
					}
				}
				System.out.println("\n");

				// Make sure that at least one property is due to be adjusted. 
				while (!adjustCountDownZeroExists) {
					for (Param p : parameters) {
						if (p.adjustCountDown > 0) p.adjustCountDown--;
						adjustCountDownZeroExists |= p.adjustCountDown == 0;
					}
				}
				
				if (triedAtLeastOneParamAdjustment) {
					if (noImprovement) {
						stagnantCount++;
						// Finish if no improvement after 3 iterations.
						if (stagnantCount > 3) {
							break;
						}
					}
					else {
						stagnantCount = 0;
					}
				}
			}
			System.out.println("Finished adjusting parameters.");
			System.out.println("Final best property values, giving result " + bestResult + ", were:");
			for (Param p : parameters) {
				System.out.println(p + "=" + p.currentBest);
			}
			System.out.println("num.generations=" + maxIterations);
			
			finished= true;
			resultFile.append("\n");
			addResult(bestResult, resultFile);
			resultFile.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Determine which performance (or fitness) evaluation to use.
	 * Use (first) performance indicator from primary evaluator if available.
	 * Otherwise the first fitness indicator from the (most) primary evaluator is used.
	 */
	private EvaluationDescription getEvalDescription(Run run, boolean log) {
		EvaluationDescription fitnessEvDesc = null;
		
		for (Evaluator ev : run.getEvaluators()) {
			for (EvaluationDescription desc : ev.getEvaluationDescriptions()) {
				if (desc.isPerformanceIndicator) {
					if (log) System.out.println("Using performance metric " + desc.name);
					return desc;
				}
				else if (fitnessEvDesc == null) {
					fitnessEvDesc = desc;
				}
			}
		}
		if (log) System.out.println("Using fitness metric " + fitnessEvDesc.name);
		return fitnessEvDesc;
	}


	private Param.Value[] getCurrentBest() {
		Param.Value[] params = new Param.Value[parameters.size()];
		for (int p = 0; p < parameters.size(); p++) {
			params[p] = parameters.get(p).currentBest;
		}
		return params;
	}
	
	
	/**
	 * Find the parameters to tune in the given nnConfig.
	 * TODO handle params in arrays.
	 */
	private void getParams(JsonObject json, String path, Run run, List<Param> toTune) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// If this 
		if (json.get("parameterTuning") != null) {
			// For each parameter to tune.
			for (Member member : json.get("parameterTuning").asObject()) {
				if (member.getValue().isObject()) {
					Param p = new Param(member.getValue().asObject(), path, member.getName(), run);
					toTune.add(p);
					System.out.println("Found parameter to tune: " + p);
				}
			}
			//json.remove("parameterTuning");
		}
		
		// Recursively search for parameters.
		for (Member member : json) {
			if (member.getValue().isObject()) {
				getParams(member.getValue().asObject(), (path.length() == 0 ? "" : path + ".") + member.getName(), run, toTune);
			}
		}
	}


	private void addResult(Result r, BufferedWriter resultFile) {
		try {
			if (finished) {
				resultFile.append("\"0\",\"<best>\",");
			}
			else if (iteration > 0) {
				resultFile.append("\"" + iteration + "\",\"" + parameters.get(property).name + "\",");
			}
			else {
				resultFile.append("\"\",\"<initial>\",");
			}
			
			for (int p = 0; p < parameters.size(); p++) {
				resultFile.append("\"" + r.paramValues[p] + "\",");
			}
			resultFile.append("\"" + maxIterations + "\",\"" + r.performance() + "\"\n");
			resultFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * command line usage
	 */
	private static void usage() {
		System.out.println("Usage:\n" + "Parameter tuning can be run with:\n  <cmd> <properties-file>");
	}
	
	
	/**
	 * Created a copy of the nnConfig with modified parameter values.
	 */
	private JsonObject getTunedConfig(Param.Value[] paramValues) {
		// Get a deep copy of the nnConfig (the copy constructor makes a shallow copy).
		JsonObject configCopy = Json.parse(config.toString()).asObject();
		for (int p = 0; p < parameters.size(); p++) {
			// Get the part of the JSON containing the param.
			JsonObject obj = configCopy;
			for (String field : parameters.get(p).path.split("\\.")) {
				obj = obj.get(field).asObject();
			}
			// Set the value.
			obj.set(parameters.get(p).name, paramValues[p].toString());
		}
		return configCopy;
	}
	
	
	private class Result implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public final String name;
		private double[] performances;
		private double meanPerformance;
		private double solvedCount;
		final Param.Value[] paramValues;
		
		public Result(String name, Param.Value[] paramValues) {
			this.name = name;
			this.paramValues = paramValues;
			performances = new double[0];
		}

		public synchronized double performance() {
			return meanPerformance;
		}
		public synchronized double solvedPercent() {
			return performances.length == 0 ? 0 : solvedCount / performances.length;
		}
		
		public int sampleSize(){
			return performances.length;
		}
		
		/**
		 * A result is considered better if the solved count is greater, 
		 * or if this is the same then by average final performance.
		 */
		public synchronized boolean betterThan(Result r) {
			if (solvedPercent() > r.solvedPercent()) {
				return true;
			}
			return meanPerformance > r.meanPerformance;
		}
		
		public synchronized void doRuns(int count ) {
			System.out.println("\t\tPerforming " + count + (performances.length > 0 ? " more" : "") + " runs for " + name);
			System.out.print("\t\t\t");
			
			int origCount = performances.length;
			performances = Arrays.copyOf(performances, performances.length + count);
			
			for (int r = origCount; r < performances.length; r++) {
				JsonObject tunedConfig = getTunedConfig(paramValues);
				Run run;
				try {
					run = new Run(null, new Configuration(tunedConfig, false, new DefaultIDFactory()));
					run.run();
					
					if (run.getCurrentIteration() < run.getMaximumIterations()) {
						solvedCount++;
					}
					
					EvaluationDescription evDesc = getEvalDescription(run, false);
					
					if (evDesc.isPerformanceIndicator) {
						performances[r] = run.getPopulation().getBestPerforming().evaluationData.getPerformanceResults().get(evDesc);
					}
					else {
						performances[r] = run.getPopulation().getFittest().evaluationData.getFitnessResults().get(evDesc);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				System.out.print(".");
				if (r > 0 && r % 25 == 0) System.out.print("("+r+")");					
			}
			System.out.println();
			
			meanPerformance = ArrayUtil.mean(performances);
		}
			
		@Override
		public synchronized String toString() {
			return "(solved " + nf6.format(solvedPercent()) + "  performance " + nf6.format(meanPerformance) + ")";
		}
	}
	
	
	public static class Param implements Serializable, Comparable<Param> {
		private static final long serialVersionUID = 1L;
		public static enum AdjustType implements Serializable { DELTA, FACTOR };
		
		// Path to the parameter field to tune, in the form of field names concatenated with periods.
		String path;
		// Field name to tune.
		String name;
		// Field for the field to tune (transient because we can't serialise).
		transient Field field;
		// Parameter annotation for the field to tune.
		Parameter param;
		// The type of the field to tune.
		Class<?> type;
		AdjustType adjustType;
		double order;
		double adjustAmountOrFactor;
		double min, max;
		Value currentBest;
		int adjustIneffectiveCount;
		int adjustCountDown;
		
		public Param(JsonObject config, String path, String name, Run run) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
			this.path = path;
			this.name = name;
			Object object = getObject(run);
			field = getField(object, name);
			param = field.getAnnotation(Parameter.class);
			adjustType = AdjustType.valueOf(config.getString("adjustType", "factor").toUpperCase());
			adjustAmountOrFactor = config.getDouble("magnitude", adjustType == AdjustType.DELTA ? 1 : 2);
			type = field.getType();
			if (!type.equals(boolean.class) && !type.equals(int.class) && !type.equals(long.class) && !type.equals(float.class) && !type.equals(double.class)) {
				throw new IllegalArgumentException("Can only tune parameters of primitive type boolean, int, long, float and double. Field " + field.getName() + " is of type " + type.getSimpleName());
			}
			min = config.getDouble("min", param.minimumValue().equals("") ? 0 : Double.parseDouble(param.minimumValue()));
			max = config.getDouble("max", param.maximumValue().equals("") ? 1 : Double.parseDouble(param.maximumValue()));
			currentBest = new Value(field.get(object));
			order = config.getDouble("order", 100);
		}
		
		@Override
		public String toString() {
			return path + "." + name;
		}
		
		public Value getValue(double v) {
			return new Value(v);
		}
		
		public boolean reduceAdjustAmountOrFactor() {
			if (type.equals(boolean.class))
				return false;
			
			double orig = adjustAmountOrFactor;
			if (adjustType == AdjustType.FACTOR)
				adjustAmountOrFactor = (adjustAmountOrFactor-1)/2+1;
			else
				adjustAmountOrFactor /= 2;
			
			// Handle int/long delta.
			if (adjustType == AdjustType.DELTA && (type.equals(int.class) || type.equals(long.class))) {
				adjustAmountOrFactor = Math.round(adjustAmountOrFactor);
				if (adjustAmountOrFactor == orig) {
					adjustAmountOrFactor--;
				}
				adjustAmountOrFactor = Math.max(1, adjustAmountOrFactor);
			}
			
			return adjustAmountOrFactor != orig;
		}
		
		/**
		 * Get the object containing the parameter field for the given Run object.
		 */
		private Object getObject(Run run) throws SecurityException, IllegalArgumentException, IllegalAccessException {
			Object o = run;
			for (String field : path.split("\\.")) {
				Field f = getField(o, field);
				o = f.get(o);
				
			}
			return o;
		}
		
		private Field getField(Object o, String field) {
			Field f = null;
			for (Class<?> clazz : getSuperClasses(o.getClass())) {
				try {
					f= clazz.getDeclaredField(field);
					break;
				}
				catch (NoSuchFieldException ex) {}
			}
			if (f == null) {
				throw new IllegalStateException("Could not find the field '" + field + "' for param " + this);
			}
			f.setAccessible(true);
			return f;
		}
			
		/**
		 * Gets a list containing the given class and all its super-classes. 
		 * The list is ordered from the given class to the most super class.
		 */
		private List<Class<?>> getSuperClasses(Class<?> clazz) {
			List<Class<?>> classes = new ArrayList<Class<?>>();
			while (!clazz.equals(Object.class)) {
				classes.add(clazz);
				clazz = clazz.getSuperclass();
			}
			return classes;
		}

		@Override
		public int compareTo(Param other) {
			return Double.compare(order, other.order);
		}
		
		
		public class Value implements Serializable {
			private static final long serialVersionUID = 1L;
			private double value;
			
			public Value(Object v) {
				if (v instanceof Boolean) {
					value = ((boolean) v) ? 1 : 0;
				}
				else {
					value = (double) v;
					if (type.equals(int.class) || type.equals(long.class)) {
						value = Math.round(value);
					}
					value = Math.max(min, Math.min(max, value));
				}
			}
			
			public double getValue() {
				return value;
			}
			
			@Override
			public String toString() {
				if (type.equals(float.class) || type.equals(double.class)) return ""+value;
				int vi = (int) Math.round(value);
				if (type.equals(int.class) || type.equals(long.class)) return ""+vi;
				// boolean
				return vi == 0 ? "false" : "true";
			}
			
			public Value up() {
				Value v = new Value(adjustType == AdjustType.DELTA ? value + adjustAmountOrFactor : value * adjustAmountOrFactor);
				return (v.value == value) ? null : v;
			}
			public Value down() {
				Value v = new Value(adjustType == AdjustType.DELTA ? value - adjustAmountOrFactor : value / adjustAmountOrFactor);
				return (v.value == value) ? null : v;
			}
			
			public Value[] variations() {
				if (type.equals(boolean.class)) {
					return new Value[]{new Value(value == 0 ? true : false)};
				}
				Value down = down();
				Value up = up();
				if (up != null && down != null) {
					return new Value[]{down, up};
				} else if (up != null) {
					return new Value[]{up};
				} else if (down != null) {
					return new Value[]{down};
				}
				return new Value[0];
			}
		}
	}
}

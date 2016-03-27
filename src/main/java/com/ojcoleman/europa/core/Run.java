package com.ojcoleman.europa.core;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Observer;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.monitor.OverviewMonitor;
import com.ojcoleman.europa.util.DecimalFormatConfigurable;
import com.thoughtworks.xstream.XStream;
import com.google.common.collect.Table;
import com.ojcoleman.europa.configurable.Component;


/**
 * <p>
 * Initiates and manages an evolutionary run, implementing the basic core cycle of evaluating a population and then
 * producing a new population according to the results. A run can be started from a saved Run or a configuration file
 * (see {@link Main}). Run is designed as a singleton, there should only ever be one instance of a Run.
 * </p>
 * <p>
 * The stages of the cycle are implemented by various components whose implementation and parameters can be specified by
 * a JSON configuration file (see {@link com.ojcoleman.europa.configurable.ComponentBase and
 * {@link com.ojcoleman.europa.Main}). The specific steps of the initialisation and evolutionary cycle, and their
 * corresponding (abstract) classes or interfaces and methods, are:
 * <ul>
 * <li>Create an initial {@link Population} with {@link Evolver#createPopulation(Population)}.</li>
 * <li>Evolutionary cycle
 * <ul>
 * <li>Evaluate the fitness of the population with one or more {@link Evaluator}s.</li>
 * <li>Produce a "rank" value for each member of population according to their overall fitness (accounting for multiple
 * fitness scores/objectives if applicable) via {@link Ranker#rank(Population)}.</li>
 * <li>Optionally speciate the population via {@link Speciator#speciate(Population)}.</li>
 * <li>Replace some members of the population with new individuals (usually based on the fittest members) via
 * {@link Evolver#evolvePopulation(Population, List)}</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author O. J. Coleman
 */
public class Run extends ComponentBase {
	private final Logger logger = LoggerFactory.getLogger(Run.class);

	
	@Parameter(description = "The path(s) to the original configuration file(s) (.json extension). NOTE: this is informational only, setting it has no effect.", optional = true)
	protected String[] configFilePaths;

	@Parameter(description = "How many iterations/generations to perform before saving the Run state to a file (for later resume or examination). A value <= 0 indicates no saving.", defaultValue = "1000")
	protected int saveFrequency;

	@Parameter(description = "The path of the file to save the Run state to (for later resume or examination). Default is the name of the Run in the current working directory.", optional = true)
	protected String savePath;

	@Parameter(description = "The default format for printing floating point numbers. Pattern string format is defined by java.text.DecimalFormat.", defaultValue = "0.00")
	protected DecimalFormatConfigurable defaultNumberFormat;
	
	/**
	 * Static reference to the (most recent) {@link #defaultNumberFormat}. This should only be used in classes which do not have a reference to the Component hierarchy. 
	 */
	protected static DecimalFormat defaultNumberFormatStatic;

	@Parameter(description = "The name of the run. Default is the name of the configuration file appended with the current date and time.", optional = true)
	protected String name;

	@Parameter(description = "File output directory. Default is run name (with an integer appended to make it unique if necessary).", optional = true)
	protected String outputDirectory;

	@Parameter(description = "The random seed. Default value is the system time.", optional = true)
	protected long randomSeed;

	@Parameter(description = "The class to use to generate random numbers. It must extend Java.util.Random.", defaultValue = "java.util.Random")
	protected Class<? extends Random> randomClass;

	@Parameter(description = "The maximum number of iterations/generations to perform. Values <= 0 indicate no limit.", defaultValue = "0")
	protected int maximumIterations;

	@Component(description = "Component for transcribing a genotype to a 'phenotype' function to be evaluated (these may be one and the same).", defaultClass = DummyTranscriber.class)
	protected Transcriber<?, ?> transcriber;

	@Component(description = "Component(s) for the fitness evaluator(s). By default the first evaluator is considered the Primary evaluator, which may be used by the Transcriber to obtain information about how the genotype should be constructed.", defaultClass = DummyEvaluator.class)
	protected Evaluator[] evaluators;

	@Component(description = "Components for monitoring the evolutionary process.", defaultClass = OverviewMonitor.class)
	protected Monitor[] monitors;
	
	@Configurable(description = "Configuration for utility class to perform operations in parallel.", defaultClass = Parallel.class)
	protected Parallel parallel;
	
	
	/**
	 * The current iteration/generation.
	 */
	protected int currentIteration;

	/**
	 * A random number generator. May be used by any part of the system.
	 * 
	 * @see #randomSeed
	 * @see #randomClass
	 */
	public final Random random;

	/**
	 * If set then this Run will stop (exit {@link #mainLoop()} when the current cycle is complete.
	 */
	protected boolean stop;
	
	private final List<Evaluator> evaluatorsList;
	
	// Running average of how long each iteration takes in seconds.
	private double avgIterationTime;

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Run(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		defaultNumberFormatStatic = defaultNumberFormat;

		if (savePath == null || savePath.equals("")) {
			savePath = getName();
		}
		
		evaluatorsList = Collections.unmodifiableList(Arrays.asList(evaluators));

		// if (singleton != null) {
		// throw new Exception("There should only be a single instance of Run.");
		// }
		// singleton = this;

		if (name == null) {
			name = getConfigFilePath().getFileName().toString();
			name += (new SimpleDateFormat("_yyyyMMdd_HHmmss")).format(new Date());
		}

		if (randomSeed == 0) {
			randomSeed = System.currentTimeMillis();
		}

		random = this.newGenericInstance(randomClass);

		currentIteration = 0;
		
		monitor(this);
		
		// If we should save the run state periodically.
		if (saveFrequency > 0) {
			addEventListener(new Observer() {
				@Override
				public void eventOccurred(Observable observed, Object event, Object state) {
					if (event == Run.Event.IterationComplete) {
						if (currentIteration > 0 && currentIteration % saveFrequency == 0) {
							XStream xstream = new XStream();
							xstream.setMarshallingStrategy(null);
							try {
								xstream.toXML(this, new FileWriter(savePath));
							} catch (IOException e) {
								logger.error("Could not save Run state to file.", e);
							}
						}
					}
				}
			});
		}
	}

	/**
	 * Execute the run.
	 * 
	 * @throws Exception
	 */
	public final synchronized void run() throws Exception {
		mainLoop();
	}

	/**
	 * Get a reference to the Transcriber component.
	 */
	protected void mainLoop() {
		// Get a reference to the Population Component.
		Population<?, ?> population = transcriber.getPopulation();
		
		// If this is the first iteration.
		if (currentIteration == 0) {
			// Create initial population.
			population.generate();
		}

		double prevTime = System.currentTimeMillis();
				
		// For each iteration/generation...
		while ((maximumIterations <= 0 || currentIteration < maximumIterations) && !stop) {
			this.fireEvent(Event.IterationBegin, currentIteration);
			
			// Evaluate the population (transcribing from genotype to phenotype as necessary).
			population.evaluate();
			
			// Produce a ranking over the population, if applicable.
			population.rank();
			
			// Speciate population if applicable.
			population.speciate();
			
			// Evolve population.
			population.evolve();
			
			// Time keeping.
			double currentTime = System.currentTimeMillis();
			double duration = (currentTime - prevTime) / 1000d;
			prevTime = currentTime;
			if (avgIterationTime == 0)
				avgIterationTime = duration;
			else
				avgIterationTime = avgIterationTime * 0.9 + duration * 0.1;
			
			this.fireEvent(Event.IterationComplete, currentIteration);
			
			currentIteration++;
		}
	}

	
	/**
	 * Get a reference to the Transcriber component.
	 */
	public Transcriber<?, ?> getTranscriber() {
		return transcriber;
	}
	
	/**
	 * Get a reference to the Population component (specified in the Transcriber).
	 */
	public Population<?, ?> getPopulation() {
		return transcriber.getPopulation();
	}


	/**
	 * Get a reference to the evaluators (as an unmodifiable list).
	 */	
	public List<Evaluator> getEvaluators() {
		return evaluatorsList;
	}
	
	/**
	 * Get a reference to the utility component for performing operations in parallel.
	 */
	public Parallel getParallel() {
		return parallel;
	}

	
	/**
	 * If set then this Run will stop when the current cycle is complete.
	 */
	public void stop() {
		stop = true;
		this.fireEvent(Event.Stopping);
	}

	/**
	 * Returns true iff this Run is set to stop when the current cycle is complete.
	 */
	public boolean isStopping() {
		return stop;
	}

	
	/**
	 * Returns the current iteration/generation.
	 */
	public int getCurrentIteration() {
		return currentIteration;
	}

	/**
	 * Returns the name of this Run.
	 * 
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the path to the directory where output files should be saved to.
	 * 
	 * @see #outputDirectory
	 */
	public Path getOutputDirectory() {
		if (outputDirectory == null) {
			try {
				outputDirectory = Paths.get(".", "output", name).toFile().getCanonicalPath().toString();
			} catch (IOException e) {
				logger.error(" Could not get canonical path for output directory.", e);
			}

			int id = 0;
			DecimalFormat formatter = new DecimalFormat("000");
			while (Files.exists(Paths.get(outputDirectory + (id == 0 ? "" : "_" + formatter.format(id))))) {
				id++;
			}
			outputDirectory = outputDirectory + (id == 0 ? "" : "_" + formatter.format(id));
		}
		
		if (!Files.exists(Paths.get(outputDirectory))) {
			try {
				Files.createDirectories(Paths.get(outputDirectory));
			} catch (IOException e) {
				logger.error(" Could not create output directory.", e);
			}
		}
		
		System.out.println(outputDirectory);
		return Paths.get(outputDirectory);
	}

	/**
	 * Returns the primary evaluator, which is the first evaluator in the list of evaluators by default.
	 */
	public Evaluator getPrimaryEvaluator() {
		return evaluators[0];
	}
	
	/**
	 * Set the {@link #monitors} to monitor the given Observable.
	 * This is typically called by the Observable, for example with:
	 * <code>
	 * this.getParentComponent(Run.class).monitor(this);
	 * </code>
	 */
	public void monitor(Observable o) {
		// Use getSubComponent as this method may be called from the constructors of other Components before monitor is initialised.
		Monitor[] monitors =  (Monitor[]) getSubComponent("monitors", this);
		for (Monitor monitor : monitors) {
			o.addEventListener(monitor);
		}
	}
	

	/**
	 * Returns the path to the (first) original configuration file, or null if none specified.
	 */
	public Path getConfigFilePath() {
		if (configFilePaths != null) {
			return Paths.get(configFilePaths[0]);
		}
		return null;
	}
	

	/**
	 * @return The default formatter for printing floating point numbers.
	 */
	public DecimalFormat getDefaultNumberFormat() {
		return defaultNumberFormat;
	}
	
	/**
	 * @return Reference to the formatter returned by {@link #getDefaultNumberFormat()} for the most recently instantiated Base. 
	 * This should only be used in classes which do not have a reference to the Component hierarchy. 
	 */
	public static DecimalFormat getDefaultNumberFormatStatic() {
		return defaultNumberFormatStatic;
	}
	
	
	@Override
	public List<ComponentStateLog> getState() {
		List<ComponentStateLog> stats = new ArrayList<>();
		
		stats.add(new ComponentStateLog("General", "Iteration", currentIteration));
		
		Runtime runtime = Runtime.getRuntime();
		long memTotal = Math.round(runtime.totalMemory() / 1048576);
		long memFree = Math.round(runtime.freeMemory() / 1048576);
		long memUsed = memTotal - memFree;
		stats.add(new ComponentStateLog("General", "Memory", "Available", memTotal, "MB"));
		stats.add(new ComponentStateLog("General", "Memory", "Free", memFree, "MB"));
		stats.add(new ComponentStateLog("General", "Memory", "Used", memUsed, "MB"));
		
		stats.add(new ComponentStateLog("General", "Time", "Iteration duration", avgIterationTime, "seconds"));
		
		double eta = avgIterationTime * (maximumIterations - currentIteration);
		stats.add(new ComponentStateLog("General", "Time", "Estimated remaining", eta, "seconds"));
		
		return stats;
	}
	
	/**
	 * Run event types.
	 */
	public static enum Event {
		/**
		 * An event type indicating that an iteration of the main cycle (evaluation of individuals; replacement of some
		 * individuals generated from fittest) is starting.
		 */
		IterationBegin,
		
		/**
		 * An event type indicating that an iteration of the main cycle (evaluation of individuals; replacement of some
		 * individuals generated from fittest) is complete. This event is fired before incrementing the iteration 
		 * counter {@link Run#getCurrentIteration()}.
		 */
		IterationComplete,

		/**
		 * An event type indicating that this Run is set to stop when the current cycle is complete.
		 */
		Stopping
	}
}

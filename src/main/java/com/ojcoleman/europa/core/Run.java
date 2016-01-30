package com.ojcoleman.europa.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.json.*;
import com.ojcoleman.europa.Base;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.IDFactory;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.populations.SimplePopulation;
import com.ojcoleman.europa.rankers.DefaultRanker;
import com.ojcoleman.europa.speciators.NoSpeciation;

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

	// private static Run singleton;

	/**
	 * Run event types.
	 */
	public static enum Event {
		/**
		 * An event type indicating that an iteration of the main cycle (evaluation of individuals; replacement of some
		 * individuals generated from fittest) is complete.
		 */
		IterationComplete,

		/**
		 * An event type indicating that this Run is set to stop when the current cycle is complete.
		 */
		Stopping
	}

	@Parameter(description = "The name of the run. DefaultEvolver is the name of the configuration file appended with the current date and time.", optional = true)
	protected String name;

	@Parameter(description = "File output directory. DefaultEvolver is run name (with an integer appended to make it unique if necessary).", optional = true)
	protected String outputDirectory;

	@Parameter(description = "The random seed. DefaultEvolver value is the system time.", optional = true)
	protected long randomSeed;

	@Parameter(description = "The class to use to generate random numbers. It must extend Java.util.Random.", defaultValue = "java.util.Random")
	protected Class<? extends Random> randomClass;

	@Parameter(description = "The maximum number of iterations/generations to perform. Values <= 0 indicate no limit.", defaultValue = "0")
	protected int maximumIterations;

	@Component(description = "Component for transcribing a genotype to a 'phenotype' function to be evaluated (these may be one and the same).", defaultClass = DummyTranscriber.class)
	protected Transcriber<?, ?> transcriber;

	@Component(description = "Component(s) for the fitness evaluator(s). By default the first evaluator is considered the Primary evaluator, which may be used by the Transcriber to obtain information about how the genotype should be constructed.", defaultClass = DummyEvaluator.class)
	protected Evaluator[] evaluators;

	@Component(description = "Component for maintaining pertinent bits of evolution history.", defaultClass = History.class)
	protected History history;
	
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

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Run(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		// if (singleton != null) {
		// throw new Exception("There should only be a single instance of Run.");
		// }
		// singleton = this;

		if (name == null) {
			if ((parentComponent instanceof Base) && ((Base) parentComponent).getConfigFilePath() != null) {
				name = ((Base) parentComponent).getConfigFilePath().getFileName().toString();
			} else {
				name = "europa";
			}
			name += (new SimpleDateFormat("_yyyyMMdd_HHmmss")).format(new Date());
		}

		if (outputDirectory == null) {
			outputDirectory = Paths.get(".", name).toFile().getCanonicalPath().toString();

			int id = 0;
			DecimalFormat formatter = new DecimalFormat("000");
			while (Files.exists(Paths.get(outputDirectory + (id == 0 ? "" : "_" + formatter.format(id))))) {
				id++;
			}
			outputDirectory = outputDirectory + (id == 0 ? "" : "_" + formatter.format(id));
		}

		if (randomSeed == 0) {
			randomSeed = System.currentTimeMillis();
		}

		random = this.newGenericInstance(randomClass);

		currentIteration = 0;
	}

	/**
	 * Execute the run.
	 * 
	 * @throws Exception
	 */
	public final synchronized void run() throws Exception {
		mainLoop();
	}

	protected void mainLoop() {
		// Get a reference to the Population Component.
		Population<?, ?> population = transcriber.getPopulation();
		
		// If this is the first iteration.
		if (currentIteration == 0) {
			// Create initial population.
			population.generate();
		}

		// For each iteration/generation...
		while ((maximumIterations <= 0 || currentIteration < maximumIterations) && !stop) {
			// Evaluate the population (transcribing from genotype to phenotype as necessary).
			population.evaluate();
			
			// Produce a ranking over the population, if applicable.
			population.rank();
			
			// Speciate population if applicable.
			population.speciate();
			
			// Evolve population.
			population.evolve();

			this.fireEvent(Event.IterationComplete);
			
			currentIteration++;
		}
	}

	
	public Transcriber<?, ?> getTranscriber() {
		return transcriber;
	}

	
	public History getHistory() {
		return history;
	}

	
	public Evaluator[] getEvaluators() {
		return evaluators;
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
		return Paths.get(outputDirectory);
	}

	/**
	 * Returns the primary evaluator, which is the first evaluator in the list of evaluators by default.
	 */
	public Evaluator getPrimaryEvaluator() {
		return evaluators[0];
	}
}

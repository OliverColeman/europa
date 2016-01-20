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
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.IsComponent;
import com.ojcoleman.europa.evaluators.DummyEvaluator;
import com.ojcoleman.europa.populations.SimplePopulation;
import com.ojcoleman.europa.rankers.DefaultRanker;
import com.ojcoleman.europa.speciators.NoSpeciation;
import com.ojcoleman.europa.transcribers.DummyTranscriber;

/**
 * <p>
 * Initiates and manages an evolutionary run, implementing the basic core cycle of evaluating a population and then
 * producing a new population according to the results. A run can be started from a saved Run or a configuration file
 * (see {@link Main}). Run is designed as a singleton, there should only ever be one instance of a Run.
 * </p>
 * <p>
 * The stages of the cycle are implemented by various components whose implementation and parameters can be specified by
 * a JSON configuration file (see {@link com.ojcoleman.europa.configurable.Component and
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
public class Run extends Component {
	private final Logger logger = LoggerFactory.getLogger(Run.class);

	private static long nextID;

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

	@IsParameter(description = "The name of the run. DefaultEvolver is the name of the configuration file appended with the current date and time.", optional = true)
	protected String name;

	@IsParameter(description = "File output directory. DefaultEvolver is run name (with an integer appended to make it unique if necessary).", optional = true)
	protected String outputDirectory;

	@IsParameter(description = "The random seed. DefaultEvolver value is the system time.", optional = true)
	protected long randomSeed;

	@IsParameter(description = "The class to use to generate random numbers. It must extend Java.util.Random.", defaultValue = "java.util.Random")
	protected Class<? extends Random> randomClass;

	@IsParameter(description = "The maximum number of iterations/generations to perform. Values <= 0 indicate no limit.", defaultValue = "0")
	protected int maximumIterations;

	@IsParameter(description = "How many threads to use to transcribe and evaluate individuals simultaneously. If <= 0 given then this defaults to the number of CPU cores.", defaultValue = "0")
	protected int parallelThreads;

	@IsComponent(description = "Component for creating the initial population and new individuals from existing individuals via genetic operators.", defaultClass = DefaultEvolver.class)
	protected Evolver evolver;

	@IsComponent(description = "Component for the population of individuals.", defaultClass = SimplePopulation.class)
	protected Population population;

	@IsComponent(description = "Component for transcribing a genotype to a 'phenotype' function to be evaluated (these may be one and the same).", defaultClass = DummyTranscriber.class)
	protected Transcriber transcriber;

	@IsComponent(description = "Component(s) for the fitness evaluator(s). By default the first evaluator is considered the Primary evaluator, which may be used by the Transcriber to obtain information about how the genotype should be constructed.", defaultClass = DummyEvaluator.class)
	protected Evaluator[] evaluators;

		@IsComponent(description = "Component for determining the overall relative fitness of individuals in the population.", defaultClass = DefaultRanker.class)
	protected Ranker ranker;

	@IsComponent(description = "Component for dividing the population into species.", defaultClass = NoSpeciation.class)
	protected Speciator speciator;

	@IsComponent(description = "Component for maintaining pertinent bits of evolution history.", defaultClass = History.class)
	protected History history;
	
	
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
	 * Constructor for {@link Component}.
	 */
	public Run(Component parentComponent, JsonObject componentConfig) throws Exception {
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

		if (parallelThreads <= 0) {
			parallelThreads = Runtime.getRuntime().availableProcessors();
		}

		// history = new History();
		// evolver = new Evolver();
	}

	// public static final Run get() {
	// return singleton;
	// }

	/**
	 * Execute the run.
	 * 
	 * @throws Exception
	 */
	public final synchronized void run() throws Exception {
		// if (singleton != null && singleton != this) {
		// throw new Exception("There is another Run instance already running.");
		// }

		mainLoop();

		// singleton = null;
	}

	protected void mainLoop() {
		// If this is the first iteration.
		if (currentIteration == 0) {
			// Create initial population.
			evolver.createPopulation(population);
		}

		// A thread pool for transcription and evaluation.
		ExecutorService threadPool = Executors.newFixedThreadPool(parallelThreads);

		// A list of transcription and evaluation tasks.
		List<Future<?>> taskList = new ArrayList<Future<?>>();

		// Pool of functions to provide to transcriber in case it can re-use them.
		final ConcurrentLinkedDeque<Function<?, ?>> functionPool = new ConcurrentLinkedDeque<Function<?, ?>>();

		// Final reference to evaluators for use in anonymous runnable class.
		final Evaluator[] evaluators = this.evaluators;

		// For each iteration/generation...
		while ((maximumIterations <= 0 || currentIteration < maximumIterations) && !stop) {

			for (final Individual individual : population.getMembers()) {
				// Don't re-evaluate if already evaluated.
				if (individual.isEvaluated()) {
					continue;
				}

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						Function<?, ?> functionExisting = functionPool.pollLast();

						// Transcribe a function from the genotype. If there's an available function in the function
						// pool it will be provided (otherwise null is passed).
						Function<?, ?> function = transcriber.transcribe(individual.genotype, functionExisting);

						// If we couldn't get a function from the pool, we could probably use more functions in there,
						// so chuck it in.
						if (functionExisting == null) {
							functionPool.add(function);
						}

						individual.setFunction(function);

						for (Evaluator evaluator : evaluators) {
							// Allow for thread cancellation.
							if (Thread.currentThread().isInterrupted()) {
								return;
							}

							// Perform the evaluation(s) defined by this evaluator.
							evaluator.evaluate(individual);

							// Make sure evaluator set a result for each evaluation type it defines.
							for (EvaluationDescription evalDesc : evaluator.getEvaluationDescriptions()) {
								if (!individual.evaluationData.getResults().containsKey(evalDesc)) {
									throw new RuntimeException("The evaluator " + evaluator.getClass().getName() + " did not set a result for the evaluation type \"" + evalDesc.name + "\" that it defines.");
								}
							}
						}
					}
				};

				Future<?> task = threadPool.submit(runnable);

				taskList.add(task);
			}

			// Wait for them all to finish.
			for (Future<?> task : taskList) {
				try {
					task.get();
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
					logger.error("Error occurred in task to transcribe and evaluate individual.", e);
					e.printStackTrace();
					// This is almost certainly a fatal error, don't go any further.
					return;
				}
			}

			currentIteration++;
		}
	}

	public Transcriber getTranscriber() {
		return transcriber;
	}

	public Population getPopulation() {
		return population;
	}

	public Ranker getRanker() {
		return ranker;
	}

	public Speciator getSpeciator() {
		return speciator;
	}

	public Evolver getEvolver() {
		return evolver;
	}

	public History getHistory() {
		return history;
	}

	public Evaluator[] getEvaluators() {
		return evaluators;
	}

	/**
	 * Get the next Run-wide ID. This is useful when IDs must be unique across an entire Run.
	 */
	public long getNextID() {
		return ++nextID;
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

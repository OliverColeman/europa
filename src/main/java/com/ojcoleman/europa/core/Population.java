package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsComponent;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.IsPrototype;
import com.ojcoleman.europa.speciators.NoSpeciation;

/**
 * Represents the population of {@link Individual}s in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public abstract class Population <G extends Genotype<?>, F extends Function<?, ?>> extends Component {
	private final Logger logger = LoggerFactory.getLogger(Population.class);

	
	@IsParameter(description = "The desired population size.", defaultValue = "100", minimumValue = "1")
	protected int desiredSize;
	
	@IsParameter(description = "How many threads to use to transcribe and evaluate individuals simultaneously. If <= 0 given then this defaults to the number of CPU cores.", defaultValue = "0")
	protected int parallelThreads;

	
	@IsPrototype (description="The configuration for the prototype Indivudual.", defaultClass=Individual.class)
	protected Individual<G, F> individualPrototype;
	
	@IsComponent(description = "Component for creating the initial population and new individuals from existing individuals via genetic operators.", defaultClass = DefaultEvolver.class)
	protected Evolver<G> evolver;
	
	@IsComponent(description = "Optional component for speciating the population.", defaultClass = NoSpeciation.class)
	protected Speciator<G, F> speciator;
	
	
	protected ArrayListMultimap<Species<G, F>, Individual<G, F>> speciesMap;
	
	
	// A thread pool for transcription and evaluation.
	private ExecutorService threadPool;
	
	// Pool of functions to provide to transcriber in case it can re-use them.
	private final ConcurrentLinkedDeque<F> functionPool;
	
	
	/**
	 * Constructor for {@link Component}.
	 */
	public Population(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		if (parallelThreads <= 0) {
			parallelThreads = Runtime.getRuntime().availableProcessors();
		}
		
		threadPool = Executors.newFixedThreadPool(parallelThreads);
		
		functionPool = new ConcurrentLinkedDeque<>();
		
		speciesMap = ArrayListMultimap.create();
	}

	/**
	 * Returns the desired or preferred population size. This may differ at times from the size of the Set returned by
	 * {@link #getMembers()} as some individuals may have been removed because they could not be transcribed or
	 * evaluated successfully.
	 */
	public int getDesiredSize() {
		return desiredSize;
	}

	/**
	 * Should return all the members of this population.
	 */
	public abstract Collection<Individual<G, F>> getMembers();

	/**
	 * Add the given Individual to this population.
	 */
	public abstract void addIndividual(Individual<G, F> individual);

	/**
	 * Remove the given Individual from this population.
	 */
	public abstract void removeIndividual(Individual<G, F> individual);

	/**
	 * Returns the individual with the given {@link Genotype#id}.
	 */
	public abstract Individual<G, F> getIndividual(long genotypeID);

	/**
	 * Add the given genotype as an Individual to this population.
	 * 
	 * @param genotypeAndOptionalArgs The first argument should be the {@link Genotype} of the
	 *            {@link Individual} to add. If a custom class has been set for {@link #individualPrototype} then more
	 *            arguments can be supplied that match the constructor of that class.
	 */
	public void addGenotype(Object... genotypeAndOptionalArgs) {
		Individual<G, F> individual = individualPrototype.newInstance(genotypeAndOptionalArgs);
		this.addIndividual(individual);
	}
	
	
	/**
	 * Generates and adds {@link Individual}s to this Population. {@link Population#getDesiredSize()} individuals will be
	 * added. A seed Genotype is generated with {@link Transcriber#getTemplateGenotype()}, and the genetic material 
	 * for each individual is created by running {@link Evolver#mutateGenotype(Genotype, boolean)} on the seed Genotype.
	 */
	public void generate() {
		Run run = this.getParentComponent(Run.class);

		G seed = (G) getParentComponent(Transcriber.class).getTemplateGenotype();

		addGenotype(seed);

		for (int i = 0; i < getDesiredSize() - 1; i++) {
			G g = seed.newInstance(run.getNextID(), seed.alleles, seed);
			evolver.mutateGenotype(g, true);
			addGenotype(g);
		}
	}
	
	public void evaluate() {
		// A list of transcription and evaluation tasks.
		List<Future<?>> taskList = new ArrayList<Future<?>>();
	
		final Transcriber<G, F> transcriber = this.getParentComponent(Transcriber.class);
		
		// Final reference to evaluators for use in anonymous runnable class.
		final Evaluator[] evaluators = this.getParentComponent(Run.class).getEvaluators();

		for (final Individual<G, F> individual : getMembers()) {
			// Don't re-evaluate if already evaluated.
			if (individual.isEvaluated()) {
				continue;
			}

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					F functionExisting = functionPool.pollLast();

					// Transcribe a function from the genotype. If there's an available function in the function
					// pool it will be provided (otherwise null is passed).
					F function = transcriber.transcribe(individual.genotype, functionExisting);
					
					// If we couldn't get a function from the pool, we could probably use more functions in there,
					// so chuck it in.
					if (functionExisting == null) {
						functionPool.add(function);
					}

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

		// Wait for all transcription and evaluation tasks to finish.
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
	}
	
	
	public void evolve() {
		evolver.evolve(this);
	}

	
	/**
	 * Returns the Evolver that is used to create new genetic material for this population based on existing members.
	 */
	public Evolver<G> getEvoler() {
		return evolver;
	}

	/**
	 * Speciates this population with {@link Speciator#speciate(Population, ArrayListMultimap)}.
	 */
	public void speciate() {
		speciesMap = speciator.speciate(this, speciesMap);
	}
	
	/**
	 * Returns the Set of Species in this Population.
	 */
	public Set<Species<G, F>> getSpecies() {
		return speciesMap.keySet();
	}
	
	/**
	 * Get the Individuals associated with the specified Species.
	 */
	public List<Individual<G, F>> getSpeciesMembers(Species<G, F> species) {
		return speciesMap.get(species);
	}
}

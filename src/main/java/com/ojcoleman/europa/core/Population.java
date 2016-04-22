package com.ojcoleman.europa.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Observer;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.rankers.DefaultRanker;
import com.ojcoleman.europa.speciators.NoSpeciation;

/**
 * Represents the population of {@link Individual}s in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public abstract class Population<G extends Genotype<?>, F extends Function<?, ?>> extends ComponentBase implements Observer {
	private final Logger logger = LoggerFactory.getLogger(Population.class);

	@Parameter(description = "The desired population size.", defaultValue = "100", minimumValue = "1")
	protected int desiredSize;

	@Prototype(description = "The configuration for the prototype Individual.", defaultClass = Individual.class)
	protected Individual<G, F> individualPrototype;

	@Component(description = "Component for creating the initial population and new individuals from existing individuals via genetic operators.", defaultClass = DefaultEvolver.class)
	protected Evolver<G> evolver;

	@Component(description = "Optional component for speciating the population.", defaultClass = NoSpeciation.class)
	protected Speciator<G, Species<G>> speciator;

	@Component(description = "Component for determining the overall relative fitness of individuals in the population.", defaultClass = DefaultRanker.class)
	protected Ranker<G, F> ranker;

	
	/**
	 * List of Species associated with this population, in order of oldest to newest.
	 */
	protected List<Species<G>> species;
	
	/**
	 * A reference to the highest ranked individual. Cleared when {@link #evaluate() is called} and set when {@link #rank()} is called.
	 */
	protected Individual<G, F> fittest;

	/**
	 * A reference to the "best performing" individual. Cleared when {@link #evaluate() is called} and set when {@link #rank()} is called.
	 * The performance value used is drawn from the first performance metric defined by the first Evaluator in {@link Run#evaluators} which defines a performance metric. 
	 */
	protected Individual<G, F> bestPerforming;

	// Pool of functions to provide to transcriber in case it can re-use them.
	private final ConcurrentLinkedDeque<F> functionPool;

	// Final reference to Run and transcriber for use in anonymous runnable class.
	final Run run;
	final Transcriber<G, F> transcriber;
	
	// Used in getState()
	int speciesCount;
	int avgSpeciesSize;
	int maxSpeciesSize;
	int minSpeciesSize;
	
	
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Population(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		functionPool = new ConcurrentLinkedDeque<>();

		species = new LinkedList<>();

		run = this.getParentComponent(Run.class);
		transcriber = this.getParentComponent(Transcriber.class);
		
		this.getParentComponent(Run.class).monitor(this);
		
		// We listen for iteration complete events so we can update age of Species.
		run.addEventListener(this);
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
	 * Return the number of individuals in this Population.
	 */
	public abstract int size();

	
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
	 * Also removes the Individual from it's current {@link Species}, if set.
	 */
	public void removeIndividual(Individual<G, ?> ind) {
		if (ind.species != null) {
			// Remove the individual from its species (this also clears the Individual.species field).
			ind.species.removeMember(ind);
		}
		remove(ind);
	}
	
	/**
	 * Implementations must remove the given individual from the underlying collection.
	 */
	protected abstract void remove(Individual<G, ?> ind);
	
	/**
	 * Returns the individual with the given {@link Genotype#id}.
	 */
	public abstract Individual<G, F> getIndividual(long genotypeID);
	
	/**
	 * Returns a reference to the highest ranked individual. Cleared when {@link #evaluate() is called} and set when {@link #rank()} is called.
	 */
	public Individual<G, F> getFittest() {
		return fittest;
	}
	
	/**
	 * Returns a reference to the best performing individual. Cleared when {@link #evaluate() is called} and set when {@link #rank()} is called.
	 * The performance value used is drawn from the first performance metric defined by the first Evaluator in {@link Run#evaluators} which defines a performance metric. 
	 */
	public Individual<G, F> getBestPerforming() {
		return bestPerforming;
	}
	
	
	/**
	 * Add the given genotype as an Individual to this population.
	 * 
	 * @param genotypeAndOptionalArgs The first argument should be the {@link Genotype} of the {@link Individual} to
	 *            add. If a custom class has been set for {@link #individualPrototype} then more arguments can be
	 *            supplied that match the constructor of that class.
	 */
	public void addGenotype(Object... genotypeAndOptionalArgs) {
		Individual<G, F> individual = individualPrototype.newInstance(genotypeAndOptionalArgs);
		this.addIndividual(individual);
	}
	
	
	/**
	 * Generates and adds {@link Individual}s to this Population. {@link Population#getDesiredSize()} individuals will
	 * be added. A seed Genotype is generated with {@link Transcriber#getTemplateGenotype()}, and the genetic material
	 * for each individual is created by running {@link Evolver#mutateGenotype(Genotype, boolean)} on the seed Genotype.
	 */
	public void generate() {
		Run run = this.getParentComponent(Run.class);

		G seed = (G) getParentComponent(Transcriber.class).getTemplateGenotype();

		addGenotype(seed);

		for (int i = 1; i < getDesiredSize(); i++) {
			G g = seed.newInstance(seed.alleles, Lists.newArrayList(seed));
			evolver.mutateGenotype(g, true);
			addGenotype(g);
		}
		
		this.fireEvent(Event.PopulationGenerated, getMembers());
	}

	
	/**
	 * <p>
	 * Evaluate all the individuals in this Population with the Evaluators in {@link Run#evaluators}. Prior to
	 * evaluation an Individual will be {@link Transcriber#transcribe(Genotype, Function)}d.
	 * </p>
	 * <p>
	 * Individuals are transcribed and evaluated in parallel via {@link Run#parallel}.
	 * </p>
	 * @return true iff any of the Evaluators indicated that the evolutionary run should terminate, false otherwise.
	 */
	public boolean evaluate() {
		// Clear reference to fittest (highest ranked) member.
		
		fittest = null;
		
		// Evaluate each member.
		this.getParentComponent(Run.class).parallel.foreach(getMembers(), new Parallel.Operation<Individual<G, F>>() {
			public void perform(Individual<G, F> individual) {
				// Don't re-evaluate if already evaluated.
				if (individual.isEvaluated()) {
					return;
				}
				
				F functionExisting = functionPool.pollLast();
	
				// Transcribe a function from the genotype. If there's an available function in the function
				// pool it will be provided (otherwise null is passed).
				F function = transcriber.transcribe(individual.genotype, functionExisting);
				individual.setFunction(function);
	
				// If we couldn't get a function from the pool, we could probably use more functions in there,
				// so chuck it in.
				if (functionExisting == null) {
					functionPool.add(function);
				}
	
				for (Evaluator evaluator : run.getEvaluators()) {
					// Allow for thread cancellation.
					if (Thread.currentThread().isInterrupted()) {
						return;
					}
	
					// Perform the evaluation(s) defined by this evaluator.
					evaluator.evaluate(individual, Log.NO_LOG);
	
					// Make sure evaluator set a result for each evaluation type it defines.
					for (EvaluationDescription evalDesc : evaluator.getEvaluationDescriptions()) {
						if (!individual.evaluationData.getResults().containsKey(evalDesc)) {
							throw new RuntimeException("The evaluator " + evaluator.getClass().getName() + " did not set a result for the evaluation type \"" + evalDesc.name + "\" that it defines.");
						}
					}
				}
				
				// Remove the function reference from the individual as we might reuse and modify the function instance for another individual.
				individual.clearFunction();
			}
		});
		
		this.fireEvent(Event.PopulationEvaluated, getMembers());
		
		// Check if any evaluators think we should terminate.
		for (Evaluator evaluator : run.getEvaluators()) {
			if (evaluator.shouldTerminate()) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Returns the Evolver that is used to create new genetic material for this population based on existing members.
	 */
	public Evolver<G> getEvoler() {
		return evolver;
	}
	
	
	/**
	 * Produces a ranking over this population with {@link Ranker#rank(Population)}.
	 * @see Individual#getRank()
	 */
	public void rank() {
		ranker.rank(this);
		
		EvaluationDescription performanceEvDesc = null;
		for (Evaluator ev : run.getEvaluators()) {
			for (EvaluationDescription desc : ev.getEvaluationDescriptions()) {
				if (desc.isPerformanceIndicator) {
					performanceEvDesc = desc;
					break;
				}
			}
			if (performanceEvDesc != null) break;
		}
		
		fittest = null;
		bestPerforming = null;
		for (Individual<G, F> ind : getMembers()) {
			if (fittest == null || ind.rank > fittest.rank) {
				fittest = ind;
			}
			
			if (performanceEvDesc != null && (bestPerforming == null || ind.evaluationData.getResult(performanceEvDesc) > bestPerforming.evaluationData.getResult(performanceEvDesc))) {
				bestPerforming = ind;
			}
		}
		
		this.fireEvent(Event.PopulationRanked, getMembers());
	}
	
	
	/**
	 * Speciates this population with {@link Speciator#speciate(Population, List<Species<G, F>>}.
	 * {@link #getSpecies()} and {@link #getSpeciesMembers(Species)} may be called subsequently.
	 */
	public void speciate() {
		speciator.speciate(this, species);

		speciesCount = species.size();
		avgSpeciesSize = 0;
		maxSpeciesSize = 0;
		minSpeciesSize = Integer.MAX_VALUE;
		for (Species<?> s : species) {
			avgSpeciesSize += s.size();
			if (s.size() > maxSpeciesSize) maxSpeciesSize = s.size();
			if (s.size() < minSpeciesSize) minSpeciesSize = s.size();
		}
		avgSpeciesSize /= species.size();
		
		this.fireEvent(Event.PopulationSpeciated, getMembers());
	}
	
	
	/**
	 * Performs an evolutionary iteration/generation with {@link Evolver#evolve(Population)}.
	 * In a generational algorithm this will typically replace the "least fit" members of the population with
	 * new members based on genetic material from the "most fit". 
	 */
	public void evolve() {
		evolver.evolve(this);
		
		this.fireEvent(Event.PopulationEvolved, getMembers());
	}
	
	
	/**
	 * Returns the Species in this Population.
	 */
	public List<Species<G>> getSpecies() {
		return Collections.unmodifiableList(species);
	}
	
	
	@Override
	public List<ComponentStateLog> getState() {
		List<ComponentStateLog> stats = new ArrayList<>();
		
		stats.add(new ComponentStateLog("General", "Population", "Size", getMembers().size()));
		stats.add(new ComponentStateLog("Species", "Count", speciesCount));
		
		stats.add(new ComponentStateLog("Species", "Size", "Minimum", minSpeciesSize));
		stats.add(new ComponentStateLog("Species", "Size", "Average", avgSpeciesSize));
		stats.add(new ComponentStateLog("Species", "Size", "Maximum", maxSpeciesSize));
		
		int avgGenotypeSize = 0;
		int maxGenotypeSize = 0;
		int minGenotypeSize = Integer.MAX_VALUE;
		
		for (Individual<?, ?> ind : getMembers()) {
			Collection<?> g = ind.genotype.getAlleles();
			avgGenotypeSize += g.size();
			if (g.size() > maxGenotypeSize) maxGenotypeSize = g.size();
			if (g.size() < minGenotypeSize) minGenotypeSize = g.size();
		}
		avgGenotypeSize /= getMembers().size();
		
		stats.add(new ComponentStateLog("Genome", "Size", "Minumum", minGenotypeSize));
		stats.add(new ComponentStateLog("Genome", "Size", "Average", avgGenotypeSize));
		stats.add(new ComponentStateLog("Genome", "Size", "Maximum", maxGenotypeSize));
		
		for (Entry<EvaluationDescription, Double> e : fittest.evaluationData.getFitnessResults().entrySet()) {
			stats.add(new ComponentStateLog("Evaluation", "Fittest", e.getKey().name, e.getValue()));
		}
		for (Entry<EvaluationDescription, Double> e : fittest.evaluationData.getPerformanceResults().entrySet()) {
			stats.add(new ComponentStateLog("Evaluation", "Fittest", e.getKey().name, e.getValue()));
		}
		
		for (Entry<EvaluationDescription, Double> e : bestPerforming.evaluationData.getFitnessResults().entrySet()) {
			stats.add(new ComponentStateLog("Evaluation", "Best Performing", e.getKey().name, e.getValue()));
		}
		for (Entry<EvaluationDescription, Double> e : bestPerforming.evaluationData.getPerformanceResults().entrySet()) {
			stats.add(new ComponentStateLog("Evaluation", "Best Performing", e.getKey().name, e.getValue()));
		}
		
		return stats;
	}
	
	
	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (observed instanceof Run) {
			if (event == Run.Event.IterationComplete) {
				for (Species<G> species : getSpecies()) {
					species.incrementAge();
				}
			}
		}
	}
	
	
	public enum Event {
		PopulationGenerated,
		PopulationEvaluated,
		PopulationRanked,
		PopulationSpeciated,
		PopulationEvolved
	}
}

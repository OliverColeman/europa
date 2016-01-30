package com.ojcoleman.europa.core;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.configurable.Component;

/**
 * Base class for classes that generate the (@link Individual)s for an initial {@link Population} and then produce new
 * Individuals based on recombination and mutation of existing members of the population (once they've been evaluated
 * and ranked).
 * 
 * @author O. J. Coleman
 */
public abstract class Evolver<G extends Genotype<?>, F extends Function<?, ?>> extends ComponentBase {
	private final Logger logger = LoggerFactory.getLogger(Evolver.class);
	
	
	@Prototype (description="The configuration for the prototype Genotype.", defaultClass=DummyGenotype.class)
	protected G genotypePrototype;
	
	
	/**
	 * The set of mutators used to mutate genotypes.
	 */
	@Component(description = "Component(s) used to mutate genotypes.", defaultClass = DummyMutator.class)
	protected Mutator<G>[] mutators;

	/**
	 * The set of recombiners, if applicable, used to generate children genotypes. This is optional as some algorithms,
	 * for example, swarm or colony based do not use recombination.
	 */
	@Component(description = "Component(s) used to recombine genotypes to produce child genotypes.", optional = true)
	protected Recombiner<G>[] recombiners;

	/**
	 * The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if
	 * any)
	 */
	@Parameter(description = "The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if any).", defaultValue = "1")
	protected double relativeCloneProportion;
	
	
	/**
	 * The actual proportion of children to produce from each {@link Recombiner}, the last entry is the clone proportion.
	 * Each entry is the actual proportion plus the previous entry. This supports {@
	 */
	private final double[] actualRecombinerProportions;
	
	private final Random random;

	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Evolver(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		actualRecombinerProportions = new double[recombiners.length+1];
		for (int i = 0; i < recombiners.length; i++) {
			actualRecombinerProportions[i] = recombiners[i].relativeProportion;
		}
		actualRecombinerProportions[recombiners.length] = relativeCloneProportion;
		ArrayUtil.normaliseSum(actualRecombinerProportions);
		for (int i = 1; i < actualRecombinerProportions.length; i++) {
			actualRecombinerProportions[i] += actualRecombinerProportions[i-1];
		}
		System.out.println("actualRecombinerProportions: " + Arrays.toString(actualRecombinerProportions));
		
		random = this.getParentComponent(Run.class).random;
	}
	
	
	/**
	 * Add {@link Individual}s to the given Population based on the current members and optionally species groupings.
	 * <p>
	 * For non-swarm/colony based algorithms, implementations should do things like (taking {@link Species} into account if applicable):
	 * <ul>
	 * <li>Select some parents.</li>
	 * <li>Generate new {@link Genotype}s from parents via the {@link #recombiners} and/or cloning and then mutate them
	 * via the {@link #mutators} or by calling {@link #mutateGenotype(Genotype, boolean)} .</li>
	 * <li>Select some elites (most likely subset of parents).</li>
	 * <li>Remove individuals other than elites from population with {@link Population#removeIndividual(Individual)}.</li>
	 * <li>Add the new Genotypes to the population ({@link Population#addGenotype(Object...)}).</li>
	 * </ul>
	 * </p>
	 * <p>
	 * For swarm/colony based algorithms, implementations should do things like:
	 * <ul>
	 * <li>For each member of the population:
	 * <li>Generate an updated version of the genotype of the member via the {@link #mutators}.</li>
	 * <li>Remove the member with {@link Population#removeIndividual(Individual)} and replace it with the updated genotype via {@link Population#addGenotype(Object...)}
	 * </ul>
	 * </p>
	 */
	public abstract void evolve(Population<G, F> population);
	
	
	/**
	 * Selects a Recombiner from {@link #recombiners} at random (or the clone operation), taking into account {@link Recombiner#relativeProportion} and {@link #relativeCloneProportion}.
	 * @return A Recombiner or null to represent the clone operation.
	 */ 
	public Recombiner<G> selectRandomRecombiner() {
		int selection = Arrays.binarySearch(actualRecombinerProportions, random.nextDouble());
		if (selection == actualRecombinerProportions.length-1) {
			return null;
		}
		return recombiners[selection];
	}


	/**
	 * Mutate the given genotype. This default implementation calls {@link Mutator#mutate(Genotype)} for each of the
	 * {@link #mutators} on the given genotype.
	 * 
	 * @param genotype The Genotype to mutate.
	 * @param applyAllMutators If set then all mutators will be applied to the given Genotype. If not set then mutators
	 *            will be applied only if their implementation of {@link Mutator#shouldMutate(Genotype)} returns true
	 *            for the given Genotype.
	 */
	public void mutateGenotype(G genotype, boolean applyAllMutators) {
		for (Mutator<G> m : mutators) {
			if (applyAllMutators || m.shouldMutate(genotype)) {
				m.mutate(genotype);
			}
		}
	}
}

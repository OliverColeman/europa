package com.ojcoleman.europa.core;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.SubComponent;
import com.ojcoleman.europa.mutators.DummyMutator;
import com.ojcoleman.europa.recombiners.DummyRecombiner;

/**
 * Base class for classes that generate the (@link Individual)s for an initial {@link Population} and then produce new
 * Individuals based on recombination and mutation of existing members of the population (once they've been evaluated
 * and ranked).
 * 
 * @author O. J. Coleman
 */
public abstract class Evolver extends ConfigurableComponent {
	private final Logger logger = LoggerFactory.getLogger(Evolver.class);
	
	/**
	 * The set of mutators used to mutate genotypes.
	 */
	@SubComponent(description = "Sub-component(s) used to mutate genotypes.", defaultImplementation=DummyMutator.class)
	protected Mutator[] mutators;

	/**
	 * The set of recombiners, if applicable, used to generate children genotypes. This is optional as some algorithms,
	 * for example, swarm or colony based do not use recombination.
	 */
	@SubComponent(description = "Sub-component(s) used to recombine genotypes to produce child genotypes.", optional = true)
	protected Recombiner[] recombiners = new Recombiner[] {};

	/**
	 * The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if
	 * any)
	 */
	@Parameter(description = "The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if any).", defaultValue = "1")
	protected double relativeCloneProportion;

	/**
	 * The class to use for the {@link Genotype}.
	 */
	@Parameter(description = "The class to use for the Genotype.", defaultValue="com.ojcoleman.europa.genotypes.DummyGenotype")
	protected Class<? extends Genotype<?>> genotypeClass;
	
	
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public Evolver(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Add {@link Individual}s to the given Population based on the current members and optionally species groupings.
	 * <p>
	 * For non-swarm/colony based algorithms, implementations should do things like:
	 * <ul>
	 * <li>Select some parents.</li>
	 * <li>Select some elites (most likely subset of parents).</li>
	 * <li>Remove other individuals from the population.</li>
	 * <li>Generate new {@link Genotype}s from parents via the {@link #recombiners} and/or cloning and then mutate them
	 * via the {@link #mutators} or by calling {@link #mutateGenotype(Genotype, boolean)} .</li>
	 * <li>Add the new Genotypes to the population ({@link Population#addGenotype(Object...)}).</li>
	 * </ul>
	 * </p>
	 * <p>
	 * For swarm/colony based algorithms, implementations should do things like:
	 * <ul>
	 * <li>For each member of the population:
	 * <li>Generate an updated version of the genotype of the member via the {@link #mutators}.</li>
	 * <li>Remove the member and replace it with the updated genotype via {@link Population#addGenotype(Object...)}
	 * </ul>
	 * </p>
	 */
	public abstract void evolvePopulation(Population pop, List<List<Individual>> species);

	/**
	 * Add {@link Individual}s to the given empty Population. {@link Population#getDesiredSize()} individuals will be 
	 * added.
	 */
	public void createPopulation(Population pop) {
		Run run = this.getParentComponent(Run.class);
		
		Genotype seed = getParentComponent(Run.class).getTranscriber().getTemplateGenotype();
		
		pop.addGenotype(seed);

		for (int i = 0; i < pop.getDesiredSize() - 1; i++) {
			Genotype g = seed.createNew(run.getNextID(), seed.alleles, seed);
			mutateGenotype(g, true);
			pop.addGenotype(g);
		}
	}
	/*
	protected <T extends Genotype<?>> Constructor<T> getGenotypeCopyConstructor(Class<T> clazz) {
		try {
			return clazz.getConstructor(long.class, clazz, Array.newInstance(clazz, 0).getClass());
		} catch (NoSuchMethodException ex) {
			String name = this.getClass().getSimpleName();
			throw new RuntimeException("The Genotype class " + this.getClass().getName() + " must provide a constructor with signature \"public " + name + "(long id,  genotype, )\").");
		}
	}
	*/
	/**
	 * Mutate the given genotype. This default implementation calls {@link Mutator#mutate(Genotype)} for each of the
	 * {@link #mutators} on the given genotype.
	 * 
	 * @param genotype The Genotype to mutate.
	 * @param applyAllMutators If set then all mutators will be applied to the given Genotype. If not set then mutators
	 *            will be applied only if their implementation of {@link Mutator#shouldMutate(Genotype)} returns true
	 *            for the given Genotype.
	 */
	public void mutateGenotype(Genotype<?> genotype, boolean applyAllMutators) {
		for (Mutator m : mutators) {
			if (applyAllMutators || m.shouldMutate(genotype)) {
				m.mutate(genotype);
			}
		}
	}
}

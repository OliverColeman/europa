package com.ojcoleman.europa.core;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.IsPrototype;
import com.ojcoleman.europa.configurable.IsComponent;

/**
 * Base class for classes that generate the (@link Individual)s for an initial {@link Population} and then produce new
 * Individuals based on recombination and mutation of existing members of the population (once they've been evaluated
 * and ranked).
 * 
 * @author O. J. Coleman
 */
public abstract class Evolver<G extends Genotype<?>> extends Component {
	private final Logger logger = LoggerFactory.getLogger(Evolver.class);
	
	
	@IsPrototype (description="The configuration for the prototype Genotype.", defaultClass=DummyGenotype.class)
	protected G genotypePrototype;
	
	
	/**
	 * The set of mutators used to mutate genotypes.
	 */
	@IsComponent(description = "Component(s) used to mutate genotypes.", defaultClass = DummyMutator.class)
	protected Mutator<G>[] mutators;

	/**
	 * The set of recombiners, if applicable, used to generate children genotypes. This is optional as some algorithms,
	 * for example, swarm or colony based do not use recombination.
	 */
	@IsComponent(description = "Component(s) used to recombine genotypes to produce child genotypes.", optional = true)
	protected Recombiner<G>[] recombiners;

	/**
	 * The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if
	 * any)
	 */
	@IsParameter(description = "The relative proportion of children to produce by cloning (relative to the proportions set for recombiners, if any).", defaultValue = "1")
	protected double relativeCloneProportion;

	/**
	 * Constructor for {@link Component}.
	 */
	public Evolver(Component parentComponent, JsonObject componentConfig) throws Exception {
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
	public abstract void evolve(Population<G, ?> population);
	


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

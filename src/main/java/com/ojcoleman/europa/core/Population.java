package com.ojcoleman.europa.core;

import java.util.Collection;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;

/**
 * Represents the population of {@link Individual}s in the evolutionary algorithm.
 * 
 * @author O. J. Coleman
 */
public abstract class Population extends Component {
	@IsParameter(description = "The desired population size.", defaultValue = "100", minimumValue = "1")
	protected int desiredSize;

	@IsParameter(description = "The class to use for Individuals. Must extend com.ojcoleman.europa.core.Individual.", defaultValue = "com.ojcoleman.europa.core.Individual")
	protected Class<Individual<?>> individualClass;

	/**
	 * Constructor for {@link Component}.
	 */
	public Population(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
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
	public abstract Collection<Individual<?>> getMembers();

	/**
	 * Add the given Individual to this population.
	 */
	public abstract void addIndividual(Individual<?> individual);

	/**
	 * Remove the given Individual from this population.
	 */
	public abstract void removeIndividual(Individual<?> individual);

	/**
	 * Returns the individual with the given {@link Genotype#id}.
	 */
	public abstract Individual<?> getIndividual(long genotypeID);

	/**
	 * Add the given genotype as an Individual to this population.
	 * 
	 * @param genotypeAndOptionalArgs The first argument should generally be the {@link Genotype} of the
	 *            {@link Individual} to add. If a custom class has been set for {@link #individualClass} then more
	 *            arguments can be supplied that match the constructor of that class.
	 */
	public final void addGenotype(Object... genotypeAndOptionalArgs) {
		throw new RuntimeException();
		// this.addIndividual(newIndividual(genotypeAndOptionalArgs));
	}

	/**
	 * Instantiates and returns a new Individual. This shouldn't ordinarily be necessary as members can be added to the
	 * population via {@link #add(Genotype)}. This method should be used instead of instantiating an Individual directly
	 * in case a custom class (extending the Individual class) is used to represent individuals (see
	 * {@link #individualClass}. If a custom class is used care must be taken to ensure that it has a constructor
	 * accepting the arguments passed to this method. The default implementation accepts a single {@link Genotype}
	 * argument.
	 */
	// public Individual<?> newIndividual(Object... genotypeAndOptionalArgs) {
	// return newInstance(individualClass); //, genotypeAndOptionalArgs);
	// }
}

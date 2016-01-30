package com.ojcoleman.europa.algos.vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.InvalidConfigurationException;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Run;

/**
 * A component that encapsulates the configuration for a type of {@link Gene} and/or {Allele} that define a set of
 * evolvable parameters.
 * 
 * @author O. J. Coleman
 */
public class ParametrisedGeneType extends ConfigurableBase {
	@Configurable(description = "The available parameters, and associated properties, whose values should be fixed when a gene of this type is added.")
	protected VectorMetadata paramsGene;

	@Configurable(description = "The available parameters, and associated properties, whose value is an allele and so may change after this type of gene is added.")
	protected VectorMetadata paramsAllele;

	@Configurable(description = "Similar to paramsAllele, however the parameters specified here are intended to be referenced from other genes or alleles, instead of those parameters being set or evolved individually in each gene or allele. If this is set to a non-empty value then an integer parameter labeled \"typeReference\" must be added to one of paramsGene or paramsAllele; this is used as the reference to the type. The minimum value for the typeReference must be 0 and the maximum value must be one less than the number of types that should be allowed.")
	protected VectorMetadata paramsType;

	/**
	 * If {@link #paramsType} is non-empty, this is a reference to the one of {@link #paramsGene} or {#paramsAllele}
	 * which contains the "typeReference" parameter.
	 */
	public final VectorMetadata paramsTypeReference;

	/**
	 * If {@link #paramsType} is non-empty, this is the number of types that should be available.
	 */
	public final int paramsTypeCount;


	public ParametrisedGeneType(Configuration config) throws Exception {
		super(config);
		
		// If a dummy instance is being set-up then a default element will be added to paramsType. However this causes an exception below, so make paramsType empty.
		if (config.isDummy) {
			paramsType = VectorMetadata.EMPTY;
		}

		if (!paramsType.isEmpty()) {
			boolean refGene = paramsGene.hasLabel("typeReference");
			boolean refAllele = paramsAllele.hasLabel("typeReference");

			if (refGene && refAllele) {
				throw new InvalidConfigurationException("Only one of paramsGene and paramsAllele may contain the typeReference parameter.");
			}
			if (!refGene && !refAllele) {
				throw new InvalidConfigurationException("If paramsType is non-empty then one of paramsGene and paramsAllele must contain the (integer) \"typeReference\" parameter.");
			}
			paramsTypeReference = refGene ? paramsGene : paramsAllele;
			if (!paramsTypeReference.isInteger("typeReference") || paramsTypeReference.bound("typeReference").start.intValue() != 0) {
				throw new InvalidConfigurationException("The typeReference parameter must be of type integer and have a minimum value of 0.");
			}
			paramsTypeCount = paramsTypeReference.bound("typeReference").end.intValue();
		} else {
			paramsTypeReference = null;
			paramsTypeCount = 0;
		}

		// Make sure same params aren't specified by allele, gene or type vectors.
		Set<String> params = new HashSet<String>();
		params.addAll(paramsGene.getLabels());
		params.addAll(paramsAllele.getLabels());
		params.addAll(paramsType.getLabels());
		if (!config.isDummy && params.size() < paramsGene.size() + paramsAllele.size() + paramsType.size()) {
			throw new InvalidConfigurationException("One or more parameters in paramsGene, paramsAllele or paramsType have the same label.");
		}
	}

	/**
	 * The available parameters, and associated properties, whose values should be fixed when a gene of this type is
	 * added.
	 */
	public VectorMetadata getParamsGene() {
		return paramsGene;
	}

	/**
	 * The available parameters, and associated properties, whose value is an allele and so may change after this type
	 * of gene is added.
	 */
	public VectorMetadata getParamsAllele() {
		return paramsAllele;
	}

	/**
	 * Similar to {@link #getParamsGene()}, however the parameters specified here are intended to be referenced from
	 * other genes or alleles (see {@link #getParamsTypeReference()}), instead of those parameters being set or evolved
	 * individually in each gene or allele. {@link #getParamsTypeCount()} alleles should be created containing values
	 * for these parameters.
	 */
	public VectorMetadata getParamsType() {
		return paramsType;
	}

	/**
	 * Returns an immutable Vector with metadata set to {@link #getParamsGene()} and with random values distributed
	 * uniformly between their minimum and maximum values (inclusive).
	 */
	public Vector createGeneVector(Random random) {
		return new Vector(paramsGene, false, random);
	}

	/**
	 * Returns a mutable Vector with metadata set to {@link #getParamsAllele()} and with values initialised to zero.
	 */
	public Vector createAlleleVector() {
		return new Vector(paramsAllele);
	}

	/**
	 * Returns a mutable Vector with metadata set to {@link #getParamsType()} and with values initialised to zero.
	 */
	public Vector createTypeVector() {
		return new Vector(paramsType);
	}
}

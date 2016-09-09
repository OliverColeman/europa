package com.ojcoleman.europa.transcribers.nn;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.algos.vector.VectorMetadata;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.InvalidConfigurationException;
import com.ojcoleman.europa.transcribers.nn.integration.SimbrainNeuralNetwork;

/**
 * A component that encapsulates the configuration for a type of {@link com.ojcoleman.europa.core.Gene} and/or {@link com.ojcoleman.europa.core.Allele} that define a set of
 * evolvable parameters for a neuron. This extends {@link ParametrisedGeneType} to allow specifying the parameter properties for the input and output neurons separate
 * from the hidden neuron. {@link ParametrisedGeneType#paramsGene} is used for the parameters for hidden neurons.
 * 
 * @author O. J. Coleman
 *
 */
public class ParametrisedNeuronGeneType extends ParametrisedGeneType {
	private final Logger logger = LoggerFactory.getLogger(ParametrisedNeuronGeneType.class);
	
	
	@Configurable(description = "The available parameters, and associated properties, whose values should be fixed when a gene for an input neuron is added.")
	protected VectorMetadata paramsInputNeuronGene;

	@Configurable(description = "The available parameters, and associated properties, whose values should be fixed when a gene for an output neuron is added.")
	protected VectorMetadata paramsOutputNeuronGene;
	

	public ParametrisedNeuronGeneType(Configuration config) throws Exception {
		super(config);
		
		if (paramsInputNeuronGene.isEmpty()) {
			logger.info("Using paramsGene for paramsInputNeuronGene.");
			paramsInputNeuronGene = this.paramsGene;
		}
		if (paramsOutputNeuronGene.isEmpty()) {
			logger.info("Using paramsGene for paramsOutputNeuronGene.");
			paramsOutputNeuronGene = this.paramsGene;
		}
	
		// Make sure same params aren't specified by allele, input neuron gene or type vectors.
		Set<String> params = new HashSet<String>();
		params.addAll(paramsInputNeuronGene.getLabels());
		params.addAll(paramsAllele.getLabels());
		params.addAll(paramsType.getLabels());
		if (!config.isDummy && params.size() < paramsInputNeuronGene.size() + paramsAllele.size() + paramsType.size()) {
			throw new InvalidConfigurationException("One or more parameters in paramsInputNeuronGene, paramsAllele or paramsType have the same label.");
		}
		
		// Make sure same params aren't specified by allele, output neuron gene or type vectors.
		params.clear();
		params.addAll(paramsOutputNeuronGene.getLabels());
		params.addAll(paramsAllele.getLabels());
		params.addAll(paramsType.getLabels());
		if (!config.isDummy && params.size() < paramsOutputNeuronGene.size() + paramsAllele.size() + paramsType.size()) {
			throw new InvalidConfigurationException("One or more parameters in paramsOutputNeuronGene, paramsAllele or paramsType have the same label.");
		}
	}

	
	/**
	 * The available parameters, and associated properties, whose values should be fixed when an input neuron gene is
	 * added.
	 */
	public VectorMetadata getParamsInputNeuronGene() {
		return paramsInputNeuronGene;
	}

	/**
	 * The available parameters, and associated properties, whose values should be fixed when an output neuron gene is
	 * added.
	 */
	public VectorMetadata getParamsOutputNeuronGene() {
		return paramsOutputNeuronGene;
	}

	/**
	 * Returns an immutable Vector with metadata set to {@link #getParamsGeneInputNeuron()} and with random values distributed
	 * uniformly between their minimum and maximum values (inclusive).
	 */
	public Vector createInputNeuronGeneVector(Random random) {
		return new Vector(paramsInputNeuronGene, false, random);
	}

	/**
	 * Returns an immutable Vector with metadata set to {@link #getParamsGeneOutputNeuron()} and with random values distributed
	 * uniformly between their minimum and maximum values (inclusive).
	 */
	public Vector createOutputNeuronGeneVector(Random random) {
		return new Vector(paramsOutputNeuronGene, false, random);
	}


	@Override
	public void getStringableMap(Map<String, Object> map) {
		super.getStringableMap(map);
		map.put("paramsInputNeuronGene", paramsInputNeuronGene);
		map.put("paramsOutputNeuronGene", paramsOutputNeuronGene);
	}
}

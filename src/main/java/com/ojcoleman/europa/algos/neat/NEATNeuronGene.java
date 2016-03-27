package com.ojcoleman.europa.algos.neat;

import java.util.Random;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Represents a neuron in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATNeuronGene extends NEATGene {
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public NEATNeuronGene(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a new
	 * NEATNeuronGene with specified type and parameter Vector.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param type The type of the neuron. Generally one of {@link NNPart#NEURON_INPUT}, {@link NNPart#NEURON_HIDDEN},
	 *            {@link NNPart#NEURON_OUTPUT}.
	 * @param paramVector The parameter vector for this gene.
	 */
	public NEATNeuronGene(NEATNeuronGene prototype, NNPart type, Vector paramVector) {
		super(prototype, Gene.typeSet(NNPart.NEURON, type), paramVector);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a new
	 * NEATNeuronGene with same type as the prototype and the specified parameter Vector.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param paramVector The parameter vector for this gene.
	 */
	public NEATNeuronGene(NEATNeuronGene prototype, Vector paramVector) {
		super(prototype, paramVector);
	}
}

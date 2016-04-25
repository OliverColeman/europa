package com.ojcoleman.europa.algos.neat;

import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.algos.vector.Vector;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.core.Gene;
import com.ojcoleman.europa.transcribers.nn.NNConfig;
import com.ojcoleman.europa.transcribers.nn.NNPart;

/**
 * Represents a synapse or connection in a NEAT neural network.
 * 
 * @author O. J. Coleman
 */
public class NEATSynapseGene extends NEATGene {
	/**
	 * The NEAT ID of the source neuron gene.
	 */
	public final long sourceID;

	/**
	 * The NEAT ID of the destination neuron gene.
	 */
	public final long destinationID;

	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public NEATSynapseGene(Configuration config) {
		super(config);
		sourceID = -1;
		destinationID = -1;
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}. Create a
	 * new NEATSynapseGene based on a specified neural network configuration. The parameter values, if any, are
	 * initialised with random values.
	 * 
	 * @param prototype The prototype gene to copy.
	 * @param sourceID The NEAT ID of the source neuron gene.
	 * @param destinationID The NEAT ID of the destination neuron gene.
	 * @param paramVector The parameter vector for this synapse.
	 */
	public NEATSynapseGene(NEATSynapseGene prototype, long sourceID, long destinationID, Vector paramVector) {
		super(prototype, Gene.typeSet(NNPart.SYNAPSE), paramVector);
		this.sourceID = sourceID;
		this.destinationID = destinationID;
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		super.getStringableMap(map);
		map.put("sourceID", sourceID);
		map.put("destinationID", destinationID);
	}
}

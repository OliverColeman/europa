package com.ojcoleman.europa.transcribers.nn.integration;

import org.simbrain.network.core.SynapseUpdateRule;

import com.ojcoleman.europa.algos.vector.ParametrisedGeneType;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;

/**
 * Encapsulates the configuration for a type of {@link com.ojcoleman.europa.core.Gene} and/or {@link com.ojcoleman.europa.core.Allele} that defines a set of evolvable parameters
 * for the synapses in a Simbrain neural network.
 * 
 * @author O. J. Coleman
 */
public class SimbrainSynapseParametrisedGeneType extends ParametrisedGeneType {
	public SimbrainSynapseParametrisedGeneType(Configuration config) throws Exception {
		super(config);
	}

	@Parameter(description = "The class of the update rule to use for the synapses, if applicable.")
	protected Class<? extends SynapseUpdateRule> updateRule;

	/**
	 * Returns the class to use for the synapses, if applicable/specified.
	 */
	public Class<? extends SynapseUpdateRule> getUpdateRuleClass() {
		return updateRule;
	}
}

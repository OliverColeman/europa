package com.ojcoleman.europa.transcribers.nn.integration;

import org.simbrain.network.core.NeuronUpdateRule;

import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuronGeneType;

/**
 * Encapsulates the configuration for a type of {@link com.ojcoleman.europa.core.Gene} and/or {@link com.ojcoleman.europa.core.Allele} that defines a set of evolvable parameters
 * for the neurons in a Simbrain neural network.
 * 
 * @author O. J. Coleman
 */
public class SimbrainNeuronParametrisedGeneType extends ParametrisedNeuronGeneType {
	@Parameter(description = "The class of the update rule to use for the neurons, if applicable.")
	protected Class<? extends NeuronUpdateRule> updateRule;

	@Parameter(description = "The class of the update rule to use for the input neurons, if applicable. Defaults to updateRule", optional=true)
	protected Class<? extends NeuronUpdateRule> updateRuleInput;

	@Parameter(description = "The class of the update rule to use for the output neurons, if applicable. Defaults to updateRule.", optional=true)
	protected Class<? extends NeuronUpdateRule> updateRuleOutput;

	
	public SimbrainNeuronParametrisedGeneType(Configuration config) throws Exception {
		super(config);
		
		if (updateRuleInput == null) {
			updateRuleInput = updateRule;
		}
		if (updateRuleOutput == null) {
			updateRuleOutput = updateRule;
		}
	}
	
	/**
	 * Returns the class to use for the neurons, if applicable/specified.
	 */
	public Class<? extends NeuronUpdateRule> getUpdateRuleClass() {
		return updateRule;
	}
	
	/**
	 * Returns the class to use for the neurons, if applicable/specified.
	 */
	public Class<? extends NeuronUpdateRule> getUpdateRuleInputClass() {
		return updateRuleInput;
	}
	
	/**
	 * Returns the class to use for the neurons, if applicable/specified.
	 */
	public Class<? extends NeuronUpdateRule> getUpdateRuleOutputClass() {
		return updateRuleOutput;
	}
}

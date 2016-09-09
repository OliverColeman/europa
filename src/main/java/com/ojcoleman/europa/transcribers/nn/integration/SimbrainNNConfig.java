package com.ojcoleman.europa.transcribers.nn.integration;

import java.util.Map;

import com.amd.aparapi.Kernel;
import com.eclipsesource.json.JsonObject;
import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.transcribers.nn.NNConfig;

/**
 * Describes additional configuration parameters for Bain neural networks.
 * 
 * @author O. J. Coleman
 */
public class SimbrainNNConfig extends NNConfig<SimbrainNeuronParametrisedGeneType, SimbrainSynapseParametrisedGeneType> {
	@Parameter(description = "The length of each time step, in milliseconds. A typical value is between 0.1 and 1.", defaultValue = "0.1")
	double timeStep;

	public SimbrainNNConfig(Configuration config) throws Exception {
		super(config);
	}

	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("timeStep", timeStep);
	}
}

package com.ojcoleman.europa.transcribers.nn.integration;

import com.amd.aparapi.Kernel;
import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.transcribers.nn.NNConfig;

/**
 * Describes additional configuration parameters for Bain neural networks.
 * 
 * @author O. J. Coleman
 */
public class BainNNConfig extends NNConfig {
	@IsParameter(description = "The number of discrete simulation steps performed for each second of simulation time. A typical resolution is 1000, or 1ms duration for each step. This is only relevant for models with decay factors and similar.", defaultValue = "1000")
	int simulationResolution;

	@IsParameter(description = "The execution mode of the neuron and synapse model kernels.\nMay be SEQ (single CPU thread),\nJTP (Java Thread Pool), CPU (OpenCL on CPU) or GPU (OpenCL on GPU).", defaultValue = "SEQ")
	Kernel.EXECUTION_MODE aparapiExecutionMode;

	public BainNNConfig(JsonObject config) throws Exception {
		super(config);
	}
}

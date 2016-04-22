package com.ojcoleman.europa.transcribers.nn.integration;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.bain.NeuralNetwork;
import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.ConfigurableComponentCollection;
import com.ojcoleman.bain.base.NeuronCollection;
import com.ojcoleman.bain.base.SynapseCollection;
import com.ojcoleman.bain.neuron.rate.NeuronCollectionWithBias;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.functiontypes.VectorFunction;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork;
import com.ojcoleman.europa.transcribers.nn.Topology;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.util.Stringer;

import edu.princeton.cs.algorithms.BellmanFordSP;
import edu.princeton.cs.algorithms.DirectedEdge;
import edu.princeton.cs.algorithms.EdgeWeightedDigraph;

/**
 * A wrapper for Bain neural networks.
 */
public class BainNeuralNetwork extends ParametrisedNeuralNetwork {
	private final Logger logger = LoggerFactory.getLogger(BainNeuralNetwork.class);
	
	@Configurable (description="The general or base configuration for the neural networks.")
	protected BainNNConfig config;
	
	/**
	 * The Bain neural network.
	 */
	protected NeuralNetwork nn;
	
	// Record of how many neurons have been added via addNeuron()
	private int addedNeuronCount;
	// Record of how many synapses have been added via addSynapse()
	private int addedSynapseCount;
	
	// Number of input neurons.
	private int inputSize;
	// Index of the first output neuron.
	private int outputIndex;
	
	/**
	 * Mapping from neuronTypes index to Bain NeuronCollection configuration index.
	 * This exists because we can't guarantee that the neuronType configs will map
	 * onto Bain configurations until we've started adding neurons, at which point
	 * the configurations can be added in any order to the Bain NeuronCollection.
	 */
	protected Map<Integer, Integer> neuronTypeToBainConfig;
	/**
	 * See {@link #neuronTypeToBainConfig}.
	 */
	protected Map<Integer, Integer> synapseTypeToBainConfig;
	
	
	/**
	 * The number of simulation steps to perform for each request to apply input to the network and retrieve output,
	 * for example via {@link #apply(double[])}.
	 */
	protected int stepsPerStep;
	
	/**
	 * The topology class of the network.
	 */
	protected Topology topology;
	
	
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(JsonObject)}.
	 */
	public BainNeuralNetwork(Configuration config) {
		super(config);
	}

	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#Prototype(PrototypeBase)}.
	 * 
	 * Create a Bain neural network with the given base configuration and instance configuration.
	 * @param prototype The prototype neural network instance to copy.
	 * @param instConfig The instance configuration for the neural network, for example number of inputs and outputs.
	 */
	public BainNeuralNetwork(BainNeuralNetwork prototype, NNInstanceConfig instConfig) {
		super(prototype, instConfig);
		
		config = prototype.config;
		
		NeuronCollection<?> neurons = null;
		SynapseCollection<?> synapses = null;
		try {
			neurons = (NeuronCollection<?>) ComponentCollection.createCollection(config.neuron().getModelClass(), instConfig.neuronCount);
		} catch (Exception e) {
			throw new RuntimeException("Error creating neurons for Bain neural network.", e);
		}
		try {
			synapses = (SynapseCollection<?>) ComponentCollection.createCollection(config.synapse().getModelClass(), instConfig.synapseCount);
		} catch (Exception e) {
			throw new RuntimeException("Error creating synapses for Bain neural network.", e);
		}
		
		inputSize = instConfig.inputCount;
		outputIndex = instConfig.neuronCount - instConfig.outputCount;
		
		// stepsPerStep and topology may be changed by setStepsPerStepForNonLayeredFF()
		stepsPerStep = instConfig.stepsPerStep;
		topology = config.getTopology();
		
		nn = new NeuralNetwork(config.simulationResolution, neurons, synapses, config.aparapiExecutionMode);
		
		neuronTypeToBainConfig = new HashMap<>();
	}
	
	
	@Override
	public BainNNConfig getConfig() {
		return config;
	}
	
	
	/**
	 * Add a neuron to this neural network.
	 * 
	 * @param config Neuron model-specific configuration parameters, if applicable. This will typically contain a "bias"
	 *            value. If types are used then the config must contain a value for "typeReference" that will refer to
	 *            the value returned by {@link #addNeuronType(Map)}.
	 * @return The index of the new neuron in this neural network. This is used to reference a neuron, for example in
	 *         {@link #addSynapse(Map, int, int)}.
	 */
	@Override
	public int addNeuron(Map<String, Double> config) {
		NeuronCollection<?> neurons = nn.getNeurons();
		
		// addedNeuronCount is the index for the new neuron.
		
		// Set bias first if specified.
		if (config.containsKey("bias")) {
			if (nn.getNeurons() instanceof NeuronCollectionWithBias) {
				((NeuronCollectionWithBias<?>) neurons).setBias(addedNeuronCount, config.get("bias"));
			}
			else {
				logger.warn("bias parameter specified in neuron configuration but neuron class does not extend NeuronCollectionWithBias, bias parameter ignored.");
			}
			config.remove("bias");
		}
		
		setComponentConfig(neurons, addedNeuronCount, config, neuronTypes, neuronTypeToBainConfig);
		
		addedNeuronCount++;
		
		return addedNeuronCount-1;
	}
	
	/**
	 * Add a synapse to this neural network.
	 * 
	 * @param config Synapse model-specific configuration parameters, if applicable. This will typically contain at
	 *            least a "weight" value. If types are used then the config must contain a value for "typeReference"
	 *            that will refer to the value returned by {@link #addNeuronType(Map)}.
	 * @param source The index of the source neuron for the synapse, as returned by {@link #addNeuron(Map)}.
	 * @param dest The index of the destination neuron for the synapse, as returned by {@link #addNeuron(Map)}.
	 * @return The index of the new synapse in this neural network.
	 */
	@Override
	public void addSynapse(Map<String, Double> config, int source, int dest) {
		SynapseCollection<?> synapses = nn.getSynapses();
		
		// addedSynapseCount is the index for the new synapse.
		
		synapses.setPreAndPostNeurons(addedSynapseCount, source, dest);
		
		// Set weight/efficacy first if specified.
		if (config.containsKey("weight")) {
			synapses.setEfficacy(addedSynapseCount, config.get("weight"));
			config.remove("weight");
		}
		
		setComponentConfig(synapses, addedSynapseCount, config, synapseTypes, synapseTypeToBainConfig);
		
		addedSynapseCount++;
	}
	
	
	/**
	 * Sets the specified Bain "component" (eg a neuron or synapse) in the specified collection to have the specified parameters.
	 * 
	 * @param componentCollection The collection to add the component to.
	 * @param componentIndex The index of the component in the collection.
	 * @param config The configuration parameters.
	 * @param types The list of component types (eg {@link #neuronTypes}). 
	 * @param typeToBainConfig Mapping from component type indexes to Bain ConfigurableComponentCollection configuration index (eg {@link #neuronTypeToBainConfig}).
	 */
	protected static void setComponentConfig(ConfigurableComponentCollection<?> componentCollection, int componentIndex, Map<String, Double> config, List<Map<String, Double>> types, Map<Integer, Integer> typeToBainConfig) {
		//TODO not adding configs when it should?
		
		TreeMap<String, Object> dbg = new TreeMap<>();
		dbg.put("0compIndex", componentIndex);
		dbg.put("1config", config);
		
		// Params specific to each component.
		Map<String, Double> nonTypeConfig = new HashMap<>(config);
		
		// Params specific to a type of component.
		Map<String, Double> typeConfig = null;
		
		// Get the index of the type parameters and the parameter values, if available.
		int typeIndex = -1;
		if (config.containsKey("typeReference")) {
			typeIndex = (int) Math.round(config.get("typeReference"));
			if (typeIndex < 0 || typeIndex >= types.size()) {
				throw new IllegalArgumentException("The typeReference parameter for a configuration for " + componentCollection.getClass().getName() + " is out of range. You probably need to check the minimum and maximum bounds specified for the parameter.");
			}
			typeConfig = types.get(typeIndex);
		}
		// Remove "typeReference" parameter if present.
		nonTypeConfig.remove("typeReference");
		
		dbg.put("2typeConfig", typeConfig);
		dbg.put("3nonTypeConfig", nonTypeConfig);
		
		
		// If a type is specified and there are no more parameters specified in the nonTypeConfig, we can use a Bain 
		// ComponentConfiguration to encapsulate the type params (which is more efficient than creating a 
		// ComponentConfiguration for every single component).
		if (typeConfig != null && !typeConfig.isEmpty() && nonTypeConfig.isEmpty()) {
			int bainConfigIndex;
			
			if (!typeToBainConfig.containsKey(typeIndex)) {
				// Add a new configuration to the component collection.
				bainConfigIndex = addBainConfiguration(componentCollection, typeConfig);
				
				// Record the mapping from the component type index to the Bain configuration index.
				typeToBainConfig.put(typeIndex, bainConfigIndex);
			}
			else {
				bainConfigIndex = typeToBainConfig.get(typeIndex);
			}
			
			// Set the configuration for this component.
			componentCollection.setComponentConfiguration(componentIndex, bainConfigIndex);
		}
		else {
			// Collect all the params together.
			if (typeConfig != null) {
				nonTypeConfig.putAll(typeConfig);
			}
			
			dbg.put("4nonTypeConfig", nonTypeConfig);
			
			
			// If there are any params to set.
			if (!nonTypeConfig.isEmpty()) {
				// Add a new configuration to the component collection.
				int configIndex = addBainConfiguration(componentCollection, nonTypeConfig);
				// Set the added component to use it.
				componentCollection.setComponentConfiguration(componentIndex, configIndex);
			}
		}
		
		dbg.put("9typeToBainConfig", typeToBainConfig);
		
		//System.out.println(Stringer.toString(dbg));
	}
	
	/**
	 * Add a Bain ComponentConfiguration to the given Bain ConfigurableComponentCollection (eg collection of neurons or synapses),
	 * setting the parameter values for the configuration to those provided.
	 */
	protected static int addBainConfiguration(ConfigurableComponentCollection<?> componentCollection, Map<String, Double> params) {
		// Create a new Bain ComponentConfiguration.
		ComponentConfiguration config = componentCollection.getConfigSingleton().createConfiguration();
		// Set the parameter values for it from the provided params.
		for (Map.Entry<String, Double> param : params.entrySet()) {
			config.setParameterValue(param.getKey(), param.getValue(), true);
		}
		
		// Add the configuration to the collection.
		componentCollection.addConfiguration(config);
		
		// Return the index of the added configuration.
		return componentCollection.getConfigurationCount() - 1;
	}
	
	
	@Override
	public void finishedBuilding() {
		setStepsPerStepForNonLayeredFF();
		
		// Add default configs to the neuron and synapse collections if no other configs added and a default is available.
		if (nn.getNeurons().getConfigurationCount() == 0) {
			ComponentConfiguration config = nn.getNeurons().getConfigSingleton();
			if (config != null) {
				nn.getNeurons().addConfiguration(config.createConfiguration());
			}
		}
		if (nn.getSynapses().getConfigurationCount() == 0) {
			ComponentConfiguration config = nn.getSynapses().getConfigSingleton();
			if (config != null) {
				nn.getSynapses().addConfiguration(config.createConfiguration());
			}
		}
	}
	

	/**
	 * (Re)calculates the number of steps required to fully activate a non-layered feed-forward network. If this network
	 * is not of type {@link Topology#FEED_FORWARD} then this method does nothing and returns
	 * immediately.
	 */
	public void setStepsPerStepForNonLayeredFF() {
		if (topology != Topology.FEED_FORWARD)
			return;
		
		// To find the longest path from any output node to any input node
		// create a directed acyclic graph representing the network where
		// each edge has the opposite direction of the corresponding 
		// connection and has weight/value = -1. Then use the Bellman-Ford 
		// algorithm for finding the lowest value path starting from each 
		// output node/vertex to each input node/vertex. The lowest value path
		// corresponds to the longest path since the edges have value -1.
		EdgeWeightedDigraph graph = new EdgeWeightedDigraph(instanceConfig.neuronCount);
		SynapseCollection<?> synapses = nn.getSynapses();
		for (int c = 0; c < synapses.getSizePopulated(); c++) {
			graph.addEdge(new DirectedEdge(synapses.getPostNeuron(c), synapses.getPreNeuron(c), -1));
		}

		// Apply Bellman-Ford algo for each output neuron, query for lowest 
		// value path to each input.
		int maxDepth = 0;
		boolean cyclic = false;
		for (int o = outputIndex; o < instanceConfig.neuronCount && !cyclic; o++) {
			BellmanFordSP bfsp = new BellmanFordSP(graph, o);
			
			if (bfsp.hasNegativeCycle()) {
				cyclic = true;
				break;
			}
			
			for (int i = 0; i < instanceConfig.inputCount; i++) {
				double dist = bfsp.distTo(i);
				if (dist != Double.POSITIVE_INFINITY && -dist > maxDepth) {
					maxDepth = (int) Math.round(-dist);
				}
			}
		}
		
		if (!cyclic) {
			stepsPerStep = maxDepth - 1;
		} else {
			logger.warn("The Bain network marked as feed forward contains cycles. Switching to recurrent topology mode with " + stepsPerStep + " activation cycles per step.");
			topology = Topology.RECURRENT;
		}
	}
	
	
	
	@Override
	public double[] apply(double[] stimuli) {
		double[] outputs = new double[instanceConfig.outputCount];
		return apply(stimuli, outputs);
	}
	
	@Override
	public double[] apply(double[] stimuli, double[] output) {
		// Get a reference to (all) neuron outputs.
		double[] nnOutputs = nn.getNeurons().getOutputs();
		
		if (config.getTopology() == Topology.FEED_FORWARD) {
			// For non-layered FF networks we have to run the network stepsPerStep times to propagate the
			// signals all the way through, while making sure the input neurons have the input values
			// maintained each step.
			for (int s = 0; s < stepsPerStep; s++) {
				if (stimuli != null) {
					System.arraycopy(stimuli, 0, nnOutputs, 0, stimuli.length);
					nn.getNeurons().setOutputsModified(0, stimuli.length);
				}
				nn.step();
			}
			System.arraycopy(stimuli, 0, nnOutputs, 0, stimuli.length);
		} else {
			if (stimuli != null) {
				System.arraycopy(stimuli, 0, nnOutputs, 0, stimuli.length);
				nn.getNeurons().setOutputsModified(0, stimuli.length);
			}
			nn.run(stepsPerStep);
		}
		
		System.arraycopy(nnOutputs, outputIndex, output, 0, instanceConfig.outputCount);
		
		return output;
	}

	@Override
	public double[][] apply(double[][] input) {
		double[][] output = new double[input.length][instanceConfig.outputCount];
		apply(input, output);
		return output;
	}
	
	@Override
	public double[][] apply(double[][] input, double[][] output) {
		// Get a reference to (all) neuron outputs.
		double[] nnOutputs = nn.getNeurons().getOutputs();
				
		int stimuliCount = input.length;
		// Optimisation for layered FF networks.

		if (topology == Topology.FEED_FORWARD_LAYERED) {
			for (int stimuliIndex = 0, responseIndex = 1 - stepsPerStep; stimuliIndex < stimuliCount + stepsPerStep - 1; stimuliIndex++, responseIndex++) {
				if (stimuliIndex < stimuliCount) {
					// Set input values.
					System.arraycopy(input[stimuliIndex], 0, nnOutputs, 0, input[stimuliIndex].length);
					nn.getNeurons().setOutputsModified(0, input[stimuliIndex].length);
				}
				nn.step();
				if (responseIndex >= 0) {
					// Get output values.
					System.arraycopy(nnOutputs, outputIndex, output[responseIndex], 0, instanceConfig.outputCount);
				}
			}
		} else {
			for (int s = 0; s < stimuliCount; s++) {
				output[s] = apply(input[s]);
			}
		}
		
		return output;
	}

	@Override
	public double getMinimumOutputValue() {
		return nn.getNeurons().getMinimumPossibleOutputValue();
	}

	@Override
	public double getMaximumOutputValue() {
		return nn.getNeurons().getMaximumPossibleOutputValue();
	}
	
	
	public boolean isInput(int neuronIndex) {
		return neuronIndex < inputSize;
	}
	
	public boolean isOutput(int neuronIndex) {
		return neuronIndex >= outputIndex;
	}
	
	
	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("bainNNConfig", config);
		map.put("topology", topology);
		map.put("stepsPerStep", stepsPerStep);
		map.put("bainNN", this.toString());
	}
	
	
	/**
	 * Returns a string describing this network and its connectivity.
	 */
	@Override
	public String toString() {
		int neuronCount = nn.getNeurons().getSize();
		int synapseCount = nn.getSynapses().getSize();
		//int neuronDisabledCount = 0;
		
		//for (int i = 0; i < neuronCount; i++) {
		//	if (!neuronDisabled[i]) neuronDisabledCount++;
		//}
		DecimalFormat floatf = new DecimalFormat(" 0.00;-0.00");
		DecimalFormat intf = new DecimalFormat("000000000");
		DecimalFormat nfInt = new DecimalFormat("0000");
		StringBuilder out = new StringBuilder(125 + synapseCount * 30);
		out.append("Neuron class: " + nn.getNeurons().getClass());
		out.append("\nSynapse class: " + nn.getSynapses().getClass());
		//out.append("\nNeuron count: " + neuronCount + "  Populated/enabled size: " + neuronDisabledCount);
		out.append("\nSynapse count: " + synapseCount + "  Populated/enabled size: " + nn.getSynapses().getSizePopulated());
		out.append("\nTopology type: " + topology);
		out.append("\nCycles per step: " + stepsPerStep);

		//out.append("\nNeurons:\n\tEnabled\t");
		out.append("\nNeurons:\n");
		NeuronCollectionWithBias biasNeurons = (nn.getNeurons() instanceof NeuronCollectionWithBias) ? (NeuronCollectionWithBias) nn.getNeurons() : null;
		//if (coordsEnabled()) {
		//	out.append("Coordinates\t\t\t\t");
		//}
		if (biasNeurons != null) {
			out.append("bias\t");
		}
		String[] paramNames = nn.getNeurons().getConfigSingleton() != null ? nn.getNeurons().getConfigSingleton().getParameterNames() : null;
		if (paramNames != null) {
			out.append("config\t");
			for (int p = 0; p < paramNames.length; p++) {
				out.append(paramNames[p].substring(0, Math.min(6, paramNames[p].length())) + "\t");
			}
		}
		out.append("\n");
		for (int i = 0; i < neuronCount; i++) {
			//out.append("\n\t" + (neuronDisabled[i] ? "0" : "1"));
			//if (coordsEnabled()) {
			//	out.append("\t(" + floatf.format(getXCoord(i)) + ", " + floatf.format(getYCoord(i)) + ", " + floatf.format(getZCoord(i)) + ")");
			//}
			if (biasNeurons != null) {
				out.append(floatf.format(biasNeurons.getBias(i)));
			}
			if (paramNames != null) {
				if (nn.getNeurons().getComponentConfiguration(i) != null) {
					out.append("\t" + nn.getNeurons().getComponentConfigurationIndex(i) + "\t" + ArrayUtil.toString(nn.getNeurons().getComponentConfiguration(i).getParameterValues(), "\t", floatf));
				}
				else {
					out.append("<no config>");
				}
			}
			out.append("\n");
		}
		
		if (nn.getNeurons().getConfigurationCount() > 0) {
			out.append("\n\nNeuron configurations:\n");
			for (int p = 0; p < paramNames.length; p++) {
				out.append("\t" + paramNames[p].substring(0, Math.min(6, paramNames[p].length())));
			}
			out.append("\n");
			for (int i = 0; i < nn.getNeurons().getConfigurationCount(); i++) {
				out.append("\t" + ArrayUtil.toString(nn.getNeurons().getConfiguration(i).getParameterValues(), ",\t", floatf) + "\n");
			}
		}
		
		out.append("\nSynapses:");
		out.append("\n\tpre > post\tEnabled\tweight");
		paramNames = nn.getSynapses().getConfigSingleton() != null ? nn.getSynapses().getConfigSingleton().getParameterNames() : null;
		if (paramNames != null) {
			out.append("\tConf\t");
			for (int p = 0; p < paramNames.length; p++) {
				out.append(paramNames[p].substring(0, Math.min(6, paramNames[p].length())) + "\t");
			}
		}
		TreeMap<String, String> sortedSynapses = new TreeMap<String, String>();
		for (int i = 0; i < synapseCount; i++) {
			int pre = nn.getSynapses().getPreNeuron(i);
			int post = nn.getSynapses().getPostNeuron(i);
			String preType = isInput(pre) ? "i" : isOutput(pre) ? "o" : "h";
			String postType = isInput(post) ? "i" : isOutput(post) ? "o" : "h";
			String enabled = nn.getSynapses().isNotUsed(i) ? "0" : "1";
			//if (!nn.getSynapses().isNotUsed(i)) {
			//	System.err.println(i + "\t" + nn.getSynapses().getEfficacy(i) + "\t " + nn.getSynapses().getInitialEfficacy(i) + "\t " + nn.getSynapses().getComponentConfiguration(i).getParameterValue("n"));
			//}
			String efficacy = floatf.format(nn.getSynapses().getInitialEfficacy(i));
			String zero = nn.getSynapses().getInitialEfficacy(i) == 0 ? "z" : " ";
			
			String key = intf.format(pre) + ":" + intf.format(post) + ":" + intf.format(i);
			String value = preType + ":" + pre + " > " + postType + ":" + post + "\t" + enabled + "\t" + efficacy + zero;
			if (paramNames != null && nn.getSynapses().getComponentConfiguration(i) != null) {
				value += "\t" + nn.getSynapses().getComponentConfigurationIndex(i) + "\t" + ArrayUtil.toString(nn.getSynapses().getComponentConfiguration(i).getParameterValues(), "\t", floatf);
			}
			//sortedSynapses.put(key, value);			
			sortedSynapses.put(nfInt.format(i), value);
		}
		for (String cs : sortedSynapses.values()) {
			out.append("\n\t" + cs);
		}
		
		if (nn.getSynapses().getConfigurationCount() > 0) {
			out.append("\n\nSynapse configurations:\n");
			for (int p = 0; p < paramNames.length; p++) {
				out.append("\t" + paramNames[p].substring(0, Math.min(6, paramNames[p].length())));
			}
			out.append("\n");
			for (int i = 0; i < nn.getSynapses().getConfigurationCount(); i++) {
				out.append("\t" + ArrayUtil.toString(nn.getSynapses().getConfiguration(i).getParameterValues(), ",\t", floatf) + "\n");
			}
		}
		
		out.append("\n");
		return out.toString();
	}
}

package com.ojcoleman.europa.transcribers.nn.integration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.transcribers.nn.NNInstanceConfig;
import com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork;
import com.ojcoleman.europa.transcribers.nn.Topology;

import edu.princeton.cs.algorithms.BellmanFordSP;
import edu.princeton.cs.algorithms.DirectedEdge;
import edu.princeton.cs.algorithms.EdgeWeightedDigraph;

/**
 * A wrapper for Simbrain neural networks.
 *
 * @author O. J. Coleman
 */
public class SimbrainNeuralNetwork extends ParametrisedNeuralNetwork {
	private final Logger logger = LoggerFactory.getLogger(SimbrainNeuralNetwork.class);
	
	@Configurable(description = "The general or base configuration for the neural networks.")
	protected SimbrainNNConfig config;
	
	protected Network network;
	
	/**
	 * The number of simulation steps to perform for each request to apply input to the network and retrieve output, for
	 * example via {@link #apply(double[])}.
	 */
	protected int stepsPerStep;
	
	/**
	 * The topology class of the network.
	 */
	protected Topology topology;

	// Index of the first output neuron.
	private int outputIndex;
	
	private Table<Class<?>, String, Setter> setterMap;
	private Table<Class<?>, String, Object> setterFailMap;
	
	
	/**
	 * PrototypeBase constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(Configuration)}.
	 */
	public SimbrainNeuralNetwork(Configuration config) {
		super(config);
	}
	
	/**
	 * Copy constructor. See {@link com.ojcoleman.europa.configurable.PrototypeBase#PrototypeBase(PrototypeBase)}.
	 * 
	 * Create a Bain neural network with the given base configuration and instance configuration.
	 * 
	 * @param prototype The prototype neural network instance to copy.
	 * @param instConfig The instance configuration for the neural network, for example number of inputs and outputs.
	 */
	public SimbrainNeuralNetwork(SimbrainNeuralNetwork prototype, NNInstanceConfig instConfig) {
		super(prototype, instConfig);
		config = prototype.config;
		
		// Simbrain expects a properties file at 
		String fs = System.getProperty("file.separator");
        File simbrainPropertiesFile = new File("." + fs + "etc" + fs + "config.properties");
        if (!simbrainPropertiesFile.exists()) {
        	try {
        		simbrainPropertiesFile.getParentFile().mkdirs();
				simbrainPropertiesFile.createNewFile();
			} catch (IOException e) {
				logger.warn("Unable to create Simbrain properties file. Simbrain initialisation might fail. Error was " + e.getMessage()); 
			}
        }
        
		network = new Network();

		outputIndex = instConfig.neuronCount - instConfig.outputCount;

		// stepsPerStep and topology may be changed by setStepsPerStepForNonLayeredFF()
		stepsPerStep = config.getSimulationStepsPerStep();
		topology = config.getTopology();
		
		// Find all the setter methods on the Simbrain neuron and synapse classes and specified update rule classes.
		// Generate mapping from class name/setter method name to setter method (wrapped in Setter utility class).
		// The labelled values in VectorGene/Alleles can then be used to set parameters on the neuron/synapse/update rule using the matching setter method.
		// TODO: if there is a setter method with the same name on the neuron/synapse class and corresponding update rule then we 
		// currently apply it only to the neuron/synapse object. Should probably provide a way to specify which if ambiguous.
		setterMap = HashBasedTable.create();
		Class<?>[] classes = new Class[]{Neuron.class, Synapse.class, config.neuron().updateRule, config.synapse().updateRule};
		for (Class<?> clazz : classes) {
			for (Method method : clazz.getMethods()) {
				if (method.getName().startsWith("set")) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1) {
						setterMap.put(clazz, method.getName().substring(3, method.getName().length()), new Setter(method, paramTypes[0]));
					}
				}
			}
		}
		setterFailMap = HashBasedTable.create();
	}
	
	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.functiontypes.VectorFunction#getMinimumOutputValue()
	 */
	@Override
	public double getMinimumOutputValue() {
		return network.getNeuron(0).getLowerBound();
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.functiontypes.VectorFunction#getMaximumOutputValue()
	 */
	@Override
	public double getMaximumOutputValue() {
		return network.getNeuron(0).getUpperBound();
	}
	
	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.functiontypes.VectorFunction#apply(double[])
	 */
	@Override
	public double[] apply(double[] input) {
		double[] outputs = new double[instanceConfig.outputCount];
		return apply(input, outputs);
	}
	
	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.core.Function#apply(java.lang.Object, java.lang.Object)
	 */
	@Override
	public double[] apply(double[] input, double[] output) {
		for (int i = 0; i < instanceConfig.inputCount; i++) {
			network.getNeuron(i).setClamped(true);
			network.getNeuron(i).forceSetActivation(input[i]);
		}
		
		for (int s = 0; s < stepsPerStep; s++) {
			network.update();
		}
		
		for (int i = 0, n = outputIndex; n < instanceConfig.neuronCount; i++, n++) {
			output[i] = network.getNeuron(n).getActivation();
		}
		
		return output;
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.functiontypes.VectorFunction#apply(double[][])
	 */
	@Override
	public double[][] apply(double[][] input) {
		double[][] output = new double[input.length][instanceConfig.outputCount];
		apply(input, output);
		return output;
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.core.Function#apply(java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public double[][] apply(double[][] input, double[][] output) {
		int stimuliCount = input.length;
		
		// Optimisation for layered FF networks.
		if (topology == Topology.FEED_FORWARD_LAYERED) {
			for (int stimuliIndex = 0, responseIndex = 1 - stepsPerStep; stimuliIndex < stimuliCount + stepsPerStep - 1; stimuliIndex++, responseIndex++) {
				for (int i = 0; i < instanceConfig.inputCount; i++) {
					network.getNeuron(i).setClamped(true);
					network.getNeuron(i).forceSetActivation(input[stimuliIndex][i]);
				}
				network.update();
				if (responseIndex >= 0) {
					for (int i = 0, n = outputIndex; n < instanceConfig.neuronCount; i++, n++) {
						output[responseIndex][i] = network.getNeuron(n).getActivation();
					}
				}
			}
		} else {
			for (int s = 0; s < stimuliCount; s++) {
				output[s] = apply(input[s]);
			}
		}
		
		return output;
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.core.Function#reset()
	 */
	@Override
	public void reset() {
		network.clearInputs();
		network.clearActivations();
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork#getConfig()
	 */
	@Override
	public SimbrainNNConfig getConfig() {
		return config;
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork#addNeuron(java.util.Map)
	 */
	@Override
	public int addNeuron(Map<String, Double> params) {
		NeuronUpdateRule rule;
		try {
			rule = config.neuron().updateRule.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Could not create instance of neuron class " + config.neuron().updateRule.getCanonicalName() + ". Does it have a constructor taking no arguments?", e);
		}
		
		Neuron neuron = new Neuron(network, rule);
	    
		Setter setter;
		for (Map.Entry<String, Double> entry : params.entrySet()) {
			setter = null;
			if ((setter = setterMap.get(Neuron.class, entry.getKey())) != null) {
				setter.set(neuron, entry.getValue());
			}
			else if ((setter = setterMap.get(config.neuron().updateRule, entry.getKey())) != null) {
				setter.set(rule, entry.getValue());
			}
			else if (!setterFailMap.contains(Neuron.class, entry.getKey())) {
				setterFailMap.put(Neuron.class, entry.getKey(), null);
				logger.error("Could not set neuron/update rule parameter for " + entry.getKey());
			}
		}
		
    	network.addNeuron(neuron);
		
	    return network.getNeuronCount()-1;
	}

	/* (non-Javadoc)
	 * @see com.ojcoleman.europa.transcribers.nn.ParametrisedNeuralNetwork#addSynapse(java.util.Map, int, int)
	 */
	@Override
	public void addSynapse(Map<String, Double> params, int source, int dest) {
		SynapseUpdateRule rule;
		try {
			rule = config.synapse().updateRule.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Could not create instance of synapse class " + config.synapse().updateRule.getCanonicalName() + ". Does it have a constructor taking no arguments?", e);
		}
		
		Synapse synapse = new Synapse(network, network.getNeuron(source), network.getNeuron(dest), rule);
	    
		Setter setter;
		for (Map.Entry<String, Double> entry : params.entrySet()) {
			if ((setter = setterMap.get(Synapse.class, entry.getKey())) != null) {
				setter.set(synapse, entry.getValue());
			}
			else if ((setter = setterMap.get(config.synapse().updateRule, entry.getKey())) != null) {
				setter.set(rule, entry.getValue());
			}
			else if (!setterFailMap.contains(Synapse.class, entry.getKey())) {
				setterFailMap.put(Synapse.class, entry.getKey(), null);
				logger.error("Could not set synapse/update rule parameter for " + entry.getKey());
			}
		}
		
    	network.addSynapse(synapse);
	}
	
	
	@Override
	public void finishedBuilding() {
		setStepsPerStepForNonLayeredFF();
	}

	/**
	 * (Re)calculates the number of steps required to fully activate a non-layered feed-forward network. If this network
	 * is not of type {@link Topology#FEED_FORWARD} then this method does nothing and returns immediately.
	 */
	public void setStepsPerStepForNonLayeredFF() {
		if (topology != Topology.FEED_FORWARD)
			return;
		
		Map<Neuron, Integer> neuronIndexMap = new HashMap<>();
		for (int i = 0; i < network.getNeuronCount(); i++) {
			neuronIndexMap.put(network.getNeuron(i), i);
		}
		
		// To find the longest path from any output node to any input node
		// create a directed acyclic graph representing the network where
		// each edge has the opposite direction of the corresponding
		// connection and has weight/value = -1. Then use the Bellman-Ford
		// algorithm for finding the lowest value path starting from each
		// output node/vertex to each input node/vertex. The lowest value path
		// corresponds to the longest path since the edges have value -1.
		EdgeWeightedDigraph graph = new EdgeWeightedDigraph(instanceConfig.neuronCount);
		for (Synapse synapse : network.getSynapseList()) {
			graph.addEdge(new DirectedEdge(neuronIndexMap.get(synapse.getTarget()), neuronIndexMap.get(synapse.getSource()), -1));
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
			stepsPerStep = maxDepth;
		} else {
			throw new RuntimeException("The Simbrain network marked as feed forward contains cycles.");
		}
	}
	

	@Override
	public void getStringableMap(Map<String, Object> map) {
		map.put("config", config);
		map.put("topology", topology);
		map.put("stepsPerStep", stepsPerStep);
		map.put("network", this.toString());
	}

	@Override
	public String toString() {
		return network.toString();
	}
	
	private static class Setter {
		/**
		 * The setter method.
		 */
		public Method method;
		/**
		 * The type of the first (and only) argument for the method.
		 */
		public Class<?> argType;
		public Setter(Method m, Class<?> t) {
			method = m;
			argType = t;
		}
		/**
		 * Call the setter method for the neuron/synapse or update rule as appropriate, with the given value as argument.
		 */
		public void set(Object o,  Double v) {
			try {
				if (argType == double.class) {
					method.invoke(o, v);
				}
				else if (argType == float.class) {
					method.invoke(o, v.longValue());
				}
				else if (argType == long.class) {
					method.invoke(o, v.longValue());
				}
				else if (argType == int.class) {
					method.invoke(o, v.floatValue());
				}
				else if (argType == boolean.class) {
					method.invoke(o, v >= 0.5);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}

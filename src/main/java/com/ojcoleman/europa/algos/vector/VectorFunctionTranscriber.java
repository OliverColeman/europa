package com.ojcoleman.europa.algos.vector;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.core.Transcriber;
import com.ojcoleman.europa.functiontypes.VectorFunction;

/**
 * Base class for transcribers that produce {@link VectorFunction}s that take a double vector as input and output a double vector.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorFunctionTranscriber<G extends Genotype<?>> extends Transcriber<G, VectorFunction> {
	/**
	 * The number of inputs for the function to transcribe. By default this is determined from the primary evaluator (see {@link com.ojcoleman.europa.Run#}), 
	 * which should usually extend {@link VectorFunctionEvaluator}.
	 */
	protected int functionInputSize;
	
	/**
	 * The number of outputs for the function to transcribe. By default this is determined from the primary evaluator (see {@link com.ojcoleman.europa.Run#}), 
	 * which should usually extend {@link VectorFunctionEvaluator}.
	 */
	protected int functionOutputSize;
	
	public VectorFunctionTranscriber(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		Evaluator primaryEvaluator = getParentComponent(Run.class).getPrimaryEvaluator();
		if (primaryEvaluator instanceof VectorFunctionEvaluator) {
			functionInputSize = ((VectorFunctionEvaluator) primaryEvaluator).getVectorFunctionInputSize();
			functionOutputSize = ((VectorFunctionEvaluator) primaryEvaluator).getVectorFunctionOutputSize();
		}
	}
	
	/**
	 * Returns the size of the input vector.
	 */
	public int getFunctionInputSize() {
		return functionInputSize;
	}
	
	/**
	 * Returns the size of the output vector.
	 */
	public int getFunctionOutputSize() {
		return functionOutputSize;
	}
}

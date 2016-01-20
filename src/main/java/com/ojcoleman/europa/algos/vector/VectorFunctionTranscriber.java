package com.ojcoleman.europa.algos.vector;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.core.Transcriber;
import com.ojcoleman.europa.functiontypes.VectorFunction;

/**
 * Base class for transcribers that produce {@link VectorFunction}s that take a double vector as input and output a
 * double vector.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorFunctionTranscriber<G extends Genotype<?>> extends Transcriber<G, VectorFunction> {
	/**
	 * The number of inputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	private int functionInputSize = -1;

	/**
	 * The number of outputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	private int functionOutputSize = -1;

	
	public VectorFunctionTranscriber(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * The number of inputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	public int getFunctionInputSize() {
		if (functionInputSize == -1) {
			Evaluator primaryEvaluator = getParentComponent(Run.class).getPrimaryEvaluator();
			if (primaryEvaluator instanceof VectorFunctionEvaluator) {
				functionInputSize = ((VectorFunctionEvaluator) primaryEvaluator).getVectorFunctionInputSize();
			}
		}
		return functionInputSize;
	}

	/**
	 *  The number of outputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	public int getFunctionOutputSize() {
		if (functionOutputSize == -1) {
			Evaluator primaryEvaluator = getParentComponent(Run.class).getPrimaryEvaluator();
			if (primaryEvaluator instanceof VectorFunctionEvaluator) {
				functionOutputSize = ((VectorFunctionEvaluator) primaryEvaluator).getVectorFunctionOutputSize();
			}
		}
		return functionOutputSize;
	}
}

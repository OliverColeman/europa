package com.ojcoleman.europa.algos.vector;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
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
	private int functionInputSize;

	/**
	 * The number of outputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	private int functionOutputSize;

	
	public VectorFunctionTranscriber(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		// Ordinarily we'd used Run.getPrimaryEvaluator() however we should use getSubComponent in Component constructors.
		Evaluator[] evaluators = (Evaluator[]) getParentComponent(Run.class).getSubComponent("evaluators", this);
		Evaluator primaryEvaluator = evaluators[0];
		if (primaryEvaluator instanceof VectorFunctionEvaluator) {
			functionInputSize = ((VectorFunctionEvaluator) primaryEvaluator).getVectorFunctionInputSize();
			functionOutputSize = ((VectorFunctionEvaluator) primaryEvaluator).getVectorFunctionOutputSize();
		}
	}

	/**
	 * The number of inputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	public int getFunctionInputSize() {
		return functionInputSize;
	}

	/**
	 *  The number of outputs for the function to transcribe. By default this is determined from the primary evaluator
	 * (see {@link com.ojcoleman.europa.Run#}), which should usually extend {@link VectorFunctionEvaluator}.
	 */
	public int getFunctionOutputSize() {
		return functionOutputSize;
	}
}

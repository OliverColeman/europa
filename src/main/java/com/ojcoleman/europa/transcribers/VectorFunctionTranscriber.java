package com.ojcoleman.europa.transcribers;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Transcriber;
import com.ojcoleman.europa.functiontypes.VectorFunction;

/**
 * Base class for transcribers that produce {@link VectorFunction}s that take a double vector as input and output a double vector.
 * 
 * @author O. J. Coleman
 */
public abstract class VectorFunctionTranscriber extends Transcriber<VectorFunction> {
	public VectorFunctionTranscriber(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}
	
	/**
	 * Returns the size of the input vector.
	 */
	public abstract int getInputSize();
	
	/**
	 * Returns the size of the output vector.
	 */
	public abstract int getOutputSize();
}

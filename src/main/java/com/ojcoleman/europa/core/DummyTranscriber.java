package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;

/**
 * Dummy evaluator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyTranscriber extends Transcriber<DummyGenotype, Function<?, ?>> {
	public DummyTranscriber(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public Function<?, ?> transcribe(DummyGenotype genotype, Function<?, ?> function, Log log) {
		return null;
	}

	@Override
	public DummyGenotype getTemplateGenotype() {
		return null;
	}

}

package com.ojcoleman.europa.transcribers;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Transcriber;

/**
 * Dummy evaluator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyTranscriber extends Transcriber<Genotype<?>, Function<?, ?>> {

	public DummyTranscriber(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public Function<?, ?> transcribe(Genotype<?> genotype, Function<?, ?> function) {
		return null;
	}

	@Override
	public Genotype<?> getTemplateGenotype() {
		return null;
	}

}

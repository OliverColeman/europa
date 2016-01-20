package com.ojcoleman.europa.mutators;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Mutator;

/**
 * Dummy mutator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyMutator extends Mutator {

	public DummyMutator(Component parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void mutate(Genotype genotype) {
	}

}

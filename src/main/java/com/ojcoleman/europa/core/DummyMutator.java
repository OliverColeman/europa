package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;

/**
 * Dummy mutator used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyMutator extends Mutator {

	public DummyMutator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public void mutate(Genotype genotype) {
	}

}

package com.ojcoleman.europa.recombiners;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;
import com.ojcoleman.europa.core.Genotype;
import com.ojcoleman.europa.core.Recombiner;

/**
 * Dummy recombiner used for printing configuration options.
 * 
 * @author O. J. Coleman
 */
public class DummyRecombiner extends Recombiner {

	public DummyRecombiner(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	@Override
	public Genotype recombine(Genotype... parents) {
		return null;
	}

}

package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.ojcoleman.europa.configurable.ConfigurableComponent;

/**
 * A Transcriber transcribes a {@link Genotype} to a "phenotype" {@link Function}. It must also determine the class of
 * the Genotype and the Function, and be able to produce a template genotype. Typically a Transcriber will query the
 * primary {@link Evaluator} to determine some aspects of the Genotype and phenotype Function.
 */
public abstract class Transcriber<G extends Genotype<?>, F extends Function<?, ?>> extends ConfigurableComponent {
	/**
	 * Constructor for {@link ConfigurableComponent}.
	 */
	public Transcriber(ConfigurableComponent parentComponent, JsonObject componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Transcribe the given genotype to a phenotype function. If the genotype and function are the same object then
	 * implementations should simply return the genotype.
	 * 
	 * @param genotype The genotype to transcribe.
	 * @param function Optionally a reference to a previously generated function. This allows the transcriber the option
	 *            to re-use the function objects it generates in the case that they are large and deallocating and
	 *            recreating them from scratch is inefficient. Note that this will be null at times (for example in the
	 *            first calls to this method).
	 */
	public abstract F transcribe(G genotype, F function);
	
	/**
	 * Generate and return a template genotype that this Transcriber knows how to transcribe. The template is used to
	 * seed an evolutionary run.
	 */
	public abstract G getTemplateGenotype();
}

package com.ojcoleman.europa.core;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Table;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Component;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.populations.SimplePopulation;

/**
 * A Transcriber transcribes a {@link Genotype} to a "phenotype" {@link Function}. It must also determine the class of
 * the Genotype and the Function, and be able to produce a template genotype. Typically a Transcriber will query the
 * primary {@link Evaluator} to determine some aspects of the Genotype and phenotype Function.
 */
public abstract class Transcriber<G extends Genotype<?>, F extends Function<?, ?>> extends ComponentBase {
	@Prototype(description = "The prototype configuration for the genotype.")
	protected G genotype;
	
	@Component(description = "Component for the population of individuals.", defaultClass = SimplePopulation.class)
	protected Population<G, F> population;
	
	
	/**
	 * Constructor for {@link ComponentBase}.
	 */
	public Transcriber(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
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
	 * Transcribe the given genotype to a phenotype function, attempting to cast the given genotype and function, if provided, to the appropriate type.
	 * This is useful when needing to perform a transcription from outside the generics-typed hierarchy of components. 
	 */
	public Function transcribeGeneric(Genotype g, Function f) {
		return this.transcribe((G) g, null);
	}
	
	/**
	 * Generate and return a template genotype that this Transcriber knows how to transcribe. The template is used to
	 * seed an evolutionary run.
	 */
	public abstract G getTemplateGenotype();

	/**
	 * Returns a reference to the Population.
	 */
	public Population<G, F> getPopulation() {
		return population;
	}
}

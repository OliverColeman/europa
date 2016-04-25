package com.ojcoleman.europa.monitor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale.Category;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.algos.neat.NEATSynapseAllele;
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Allele;
import com.ojcoleman.europa.core.EvaluationDescription;
import com.ojcoleman.europa.core.Evaluator;
import com.ojcoleman.europa.core.Function;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Log;
import com.ojcoleman.europa.core.Monitor;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.util.Stringer;

/**
 * 
 * Implementation of {@link Monitor} that records information about the fittest individual.
 * 
 * @author O. J. Coleman
 */
public class FittestMonitor extends Monitor {
	@Parameter(description = "How many iterations between logging the fittest.", defaultValue = "1")
	protected int period;

	@Parameter(description = "Whether to log the fittest (highest ranked) individual.", defaultValue = "true")
	protected boolean logFittest;

	@Parameter(description = "Whether to log the best performing individual (defined by first Evaluator to define a performance metric).", defaultValue = "true")
	protected boolean logBestPerforming;

	private Run run;

	public FittestMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		run = this.getParentComponent(Run.class);
	}

	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (event == Run.Event.IterationComplete && (run.getCurrentIteration() % period == 0 || run.isStopping())) {
			if (logFittest) {
				log(run.getPopulation().getFittest(), "fittest");
			}
			if (logBestPerforming) {
				log(run.getPopulation().getBestPerforming(), "best_performing");
			}
		}
	}

	protected void log(Individual<?, ?> ind, String label) {
		try {
			FileWriter outputFile = new FileWriter(run.getOutputDirectory().resolve(label + "-" + run.getCurrentIteration() + "-" + ind.id + "-genotype.log").toFile());
			outputFile.append(Stringer.toString(ind.genotype));
			outputFile.close();

			// Transcribe a function from the genotype.
			Function<?, ?> function = run.getTranscriber().transcribeGeneric(ind.genotype, null);
			ind.setFunction(function);

			outputFile = new FileWriter(run.getOutputDirectory().resolve(label + "-" + run.getCurrentIteration() + "-" + ind.id + "-function.log").toFile());
			outputFile.append(Stringer.toString(function));
			outputFile.close();

			ind.evaluationData.clear();

			int evalIndex = 0;
			for (Evaluator evaluator : run.getEvaluators()) {
				// Allow for thread cancellation.
				if (Thread.currentThread().isInterrupted()) {
					return;
				}

				// Perform the evaluation(s) defined by this evaluator.
				Log log = new Log("string");
				evaluator.evaluate(ind, log);

				if (log.hasItem("string")) {
					outputFile = new FileWriter(run.getOutputDirectory().resolve(label + "-" + run.getCurrentIteration() + "-" + ind.id + "-evaluation-" + evalIndex + "-" + evaluator.getName() + ".log").toFile());
					outputFile.append(Stringer.toString(log.getLog("string")));
					outputFile.close();
				}
				evalIndex++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

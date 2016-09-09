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
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.core.Species;
import com.ojcoleman.europa.transcribers.nn.NNPart;
import com.ojcoleman.europa.util.ArrayUtil;
import com.ojcoleman.europa.util.Stringer;

/**
 * 
 * Implementation of {@link Monitor} that records the sizes of species every iteration/generation 
 * in a CSV file that can be used to create a visualisation of the comings and goings of species.
 * 
 * @author O. J. Coleman
 */
public class SpeciesMonitor extends Monitor {
	private Run run;
	private Map<Long, Integer> speciesSizes = new TreeMap<>();

	public SpeciesMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);

		run = this.getParentComponent(Run.class);
	}

	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (event == Population.Event.PopulationSpeciated) {
			try {
				StringBuilder out = new StringBuilder();
				
				// Reset all sizes.
				for (Map.Entry<Long, Integer> ss : speciesSizes.entrySet()) {
					ss.setValue(0);
				}
				
				// Update/add sizes for current species.
				for (Species<?> s : run.getPopulation().getSpecies()) {
					speciesSizes.put(s.id, s.size());
				}
				
				// Print sizes.
				boolean first = true;
				for (Integer ss : speciesSizes.values()) {
					out.append((first ? "" : ", ") + ss);
					first = false;
				}
				
				out.append("\n");
				
				FileWriter outputFile = new FileWriter(run.getOutputDirectory().resolve("species-size.log").toFile(), true);
				outputFile.append(out);
				outputFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

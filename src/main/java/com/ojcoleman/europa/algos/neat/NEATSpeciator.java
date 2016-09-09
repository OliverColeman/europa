package com.ojcoleman.europa.algos.neat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Individual;
import com.ojcoleman.europa.core.Parallel;
import com.ojcoleman.europa.core.Population;
import com.ojcoleman.europa.core.Run;
import com.ojcoleman.europa.core.Speciator;
import com.ojcoleman.europa.core.Species;
import com.ojcoleman.europa.util.Stringer;

/**
 * Base class for Speciators for NEATGenotypes.
 * 
 * @author O. J. Coleman
 */
public abstract class NEATSpeciator<G extends NEATGenotype, S extends Species<G>> extends Speciator<G, S> {
	@Parameter(description = "The importance of the number of excess genes (C1).", defaultValue = "1", minimumValue = "0")
	protected double excessGenesFactor;

	@Parameter(description = "The importance of the number of disjoint genes (C2).", defaultValue = "1", minimumValue = "0")
	protected double disjointGenesFactor;

	@Parameter(description = "The importance of the difference in weights or other parameter values (C3).", defaultValue = "0.4", minimumValue = "0")
	protected double paramValueDifferenceFactor;

	@Parameter(description = "Whether to normalise the difference between parameter values.", defaultValue = "true")
	protected boolean normaliseParameterValues;

	@Parameter(description = "For mismatched genes (disjoint or excess), whether the first allele parameter value (typically the weight) should be used instead of a constant value of 1.", defaultValue = "false")
	protected boolean geneMismatchUseValues;

	public NEATSpeciator(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
	}

	/**
	 * Determine the distance between the two given genotypes, according to the parameters set for this speciator.
	 */
	public double getDistance(NEATGenotype g1, NEATGenotype g2) {
		double disjointCountOrValueSum = 0, excessCountOrValueSum = 0, commonCount = 0;
		double paramDifference = 0;
		List<NEATAllele<?>> g1Alleles = new LinkedList<>(g1.getAlleles());
		List<NEATAllele<?>> g2Alleles = new LinkedList<>(g2.getAlleles());
		
		double maxSize = Math.max(g1Alleles.size(), g2Alleles.size());
		
		boolean dbg = false; //Math.random() < 0.00001;

		if (g1Alleles.isEmpty() || g2Alleles.isEmpty()) {
			Collection<NEATAllele<?>> m = g1Alleles.size() == 0 ? g2Alleles : g1Alleles;
			for (NEATAllele<?> a : m) {
				excessCountOrValueSum += getMismatchValue(a);
			}
		} else {
			Iterator<NEATAllele<?>> g1Iter = g1Alleles.iterator(), g2Iter = g2Alleles.iterator();
			NEATAllele<?> g1Current = g1Iter.next(), g2Current = g2Iter.next();
			long g1MaxInnoID = g1Alleles.get(g1Alleles.size() - 1).gene.id, g2MaxInnoID = g2Alleles.get(g2Alleles.size() - 1).gene.id;
			
			if (dbg) System.out.println("g1MaxInnoID: " + g1MaxInnoID);
			if (dbg) System.out.println("g2MaxInnoID: " + g2MaxInnoID);
			
			// Iterate through g1 and g2 alleles counting up common and disjoint genes as we go.
			do {
				if (dbg) System.out.println("g1 id: " + g1Current.gene.id);
				if (dbg) System.out.println("g2 id: " + g2Current.gene.id);
				
				if (g1Current.gene.id == g2Current.gene.id) {
					if (dbg) System.out.println("common");
					
					commonCount++;
					paramDifference += g1Current.difference(g2Current, normaliseParameterValues);
					
					g1Current = g1Iter.hasNext() ? g1Iter.next() : null;
					g2Current = g2Iter.hasNext() ? g2Iter.next() : null;
				} else {
					if (dbg) System.out.println("dis");
					
					NEATAllele<?> a = g1Current.gene.id < g2Current.gene.id ? g1Current : g2Current;
					disjointCountOrValueSum += getMismatchValue(a);

					if (g1Current.gene.id < g2Current.gene.id) {
						g1Current = g1Iter.hasNext() ? g1Iter.next() : null;
					} else {
						g2Current = g2Iter.hasNext() ? g2Iter.next() : null;
					}
				}
			} while (g1Current != null && g2Current != null);
			
			

			// If the last gene pulled from g1 is out of the range of innovation IDs of g2, add it to excess.
			if (g1Current != null && g1Current.gene.id > g2MaxInnoID) {
				if (dbg) System.out.println("g1 ec:" + g1Current.gene.id);
				
				excessCountOrValueSum += getMismatchValue(g1Current);
			}
			// If the last gene pulled from g2 is out of the range of innovation IDs of g1, add it to excess.
			if (g2Current != null && g2Current.gene.id > g1MaxInnoID) {
				if (dbg) System.out.println("g2 ec:" + g2Current.gene.id);
				
				excessCountOrValueSum += getMismatchValue(g2Current);
			}

			// Iterate over and count up any remaining excess genes.
			while (g1Iter.hasNext()) {
				NEATAllele<?> allele = g1Iter.next();
				
				if (dbg) System.out.println("g1 e:" + allele.gene.id);
				
				excessCountOrValueSum += getMismatchValue(allele);
			}
			while (g2Iter.hasNext()) {
				NEATAllele<?> allele = g2Iter.next();
				
				if (dbg) System.out.println("g2 e:" + allele.gene.id);
				
				excessCountOrValueSum += getMismatchValue(allele);
			}
		}
		
		disjointCountOrValueSum /= maxSize;
		excessCountOrValueSum /= maxSize;

		if (dbg) {
			System.out.println("g1: " + Stringer.toString(g1, 4));
			System.out.println("g2: " + Stringer.toString(g2, 4));
			
			System.out.println(disjointCountOrValueSum + " : " + excessCountOrValueSum + " : " + paramDifference + " : "
					+ (excessGenesFactor * excessCountOrValueSum + disjointGenesFactor * disjointCountOrValueSum +
							paramValueDifferenceFactor * paramDifference));
			
		}
		
		return disjointGenesFactor * disjointCountOrValueSum + excessGenesFactor * excessCountOrValueSum + paramValueDifferenceFactor * paramDifference;
	}
	
	
	// Distance value of a mismatched allele.
	private double getMismatchValue(NEATAllele<?> allele) {
		if (geneMismatchUseValues) {
			double value = allele.vector.getValues().get(0);
			if (normaliseParameterValues) {
				return allele.vector.metadata.bound(0).translateToUnit(value);
			}
			return value;
		}
		return 1;
	}
}

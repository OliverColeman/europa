package com.ojcoleman.europa.algos.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.google.common.collect.BiMap;
import com.ojcoleman.europa.configurable.ConfigurableBase;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.DefaultIDFactory;
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.configurable.PrototypeBase;
import com.ojcoleman.europa.util.Interval;
import com.ojcoleman.europa.util.IntervalDouble;
import com.ojcoleman.europa.util.IntervalLong;

/**
 * Class for containing meta-data about each value in a numeric vector.
 * 
 * @author O. J. Coleman
 */
public class VectorMetadata extends ConfigurableBase {
	/**
	 * Metadata for an empty (zero length/size) vector.
	 */
	public final static VectorMetadata EMPTY = new VectorMetadata(new ArrayList<String>(0), new double[0], new double[0], new boolean[0]);

	@Configurable(description = "The default meta-data for the elements of the Vector.")
	protected ElementDefaults elementDefaults;

	@Configurable(description = "The meta-data for each element of the Vector.")
	protected Element[] elements;

	private Map<String, Element> labelMap;
	private List<String> labels;
	

	/**
	 * Creates new VectorMetadata from the given Configuration.
	 */
	public VectorMetadata(Configuration config) {
		super(config);
		
		// Set default bounds if necessary.
		elementDefaults.updateBounds(0, 1);
		
		// Set bounds for elements whose bounds were not set. This also populates Element.bounds. 
		for (Element el : elements) {
			el.updateBounds(elementDefaults.min, elementDefaults.max);
		}
		
		labels = new ArrayList<>(elements.length);
		labelMap = new HashMap<String, Element>();
		for (int i = 0; i < elements.length; i++) {
			labels.add(elements[i].label);
			labelMap.put(elements[i].label, elements[i]);
		}
	}
	
	
	/**
	 * Creates new VectorMetadata from the given specifications.
	 */
	public VectorMetadata(List<String> labels, double[] minValues, double[] maxValues, boolean[] isInteger) {
		elements = new Element[labels.size()];
		
		for (int e = 0; e < elements.length; e++) {
			elements[e] = new Element(labels.get(e), minValues[e], maxValues[e], isInteger[e]);
		}

		this.labels = new ArrayList<>(labels);
		labelMap = new HashMap<String, Element>();
		for (int i = 0; i < elements.length; i++) {
			this.labels.add(elements[i].label);
			labelMap.put(elements[i].label, elements[i]);
		}
	}
	

	/**
	 * Returns the size of the vector(s) this VectorMetadata is intended to contain information about.
	 */
	public int size() {
		return elements.length;
	}

	public boolean isEmpty() {
		return elements.length == 0;
	}

	public IntervalDouble bound(int index) {
		return elements[index].bounds;
	}

	public boolean isInteger(int index) {
		return elements[index].isInteger;
	}

	public List<String> getLabels() {
		return Collections.unmodifiableList(labels);
	}

	public String label(int index) {
		return elements[index].label;
	}

	public boolean hasLabel(String label) {
		return labelMap.containsKey(label);
	}

	public IntervalDouble bound(String label) {
		return labelMap.get(label).bounds;
	}

	public boolean isInteger(String label) {
		return labelMap.get(label).isInteger;
	}

	/**
	 * Two VectorMetadata objects are considered equal if they have the same labels, bounds and isInteger specifications
	 * in the same order.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof VectorMetadata) {
			VectorMetadata other = (VectorMetadata) o;
			return Arrays.equals(elements, other.elements);
		}
		return false;
	}

	/**
	 * Stores meta-data for the default settings for the Elements of a Vector.
	 */
	public static class ElementDefaults extends ConfigurableBase {
		@Parameter(description = "The lower bound for this Vector Element, inclusive.", optional = true)
		protected double min = Double.NaN;

		@Parameter(description = "The upper bound for this Vector Element, inclusive.", optional = true)
		protected double max = Double.NaN;

		@Parameter(description = "Indicates if this Vector Element stores an integer value.", defaultValue="false")
		protected boolean isInteger;
		
		/**
		 * The minimum and maximum bounds for this Vector Element.
		 */
		protected IntervalDouble bounds;

		public ElementDefaults(Configuration config) {
			super(config);
		}
		
		public ElementDefaults(double min, double max, boolean isInteger) {
			this.isInteger = isInteger;
			bounds = new IntervalDouble(min, max);
		}
		
		protected void updateBounds(double minDefault, double maxDefault) {
			if (Double.isNaN(min)) {
				min = minDefault;
			}
			if (Double.isNaN(max)) {
				max = maxDefault;
			}
			bounds = new IntervalDouble(min, max);
		}
		
		/**
		 * Get the lower and upper bounds for this Vector Element, inclusive.
		 */
		public IntervalDouble getBounds() {
			return bounds;
		}

		/**
		 * Returns true iff this Vector Element stores an integer value.
		 */
		public boolean isInteger() {
			return isInteger;
		}
	}

	/**
	 * Stores meta-data for a Vector element.
	 */
	public static class Element extends ElementDefaults {
		@Parameter(description = "The label for this Vector Element.", optional = true)
		protected String label;
		
		public Element(Configuration config) {
			super(config);
		}

		public Element(String label, double min, double max, boolean isInteger) {
			super(min, max, isInteger);
			this.label = label;
		}

		/**
		 * Get the label for this Vector Element.
		 */
		public String getLabel() {
			return label;
		}

		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o instanceof Element) {
				Element other = (Element) o;
				return label.equals(other.label) && isInteger == other.isInteger && bounds.equals(other.bounds);
			}
			return false;
		}
	}
}

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
import com.ojcoleman.europa.configurable.Configurable;
import com.ojcoleman.europa.configurable.IsConfigurable;
import com.ojcoleman.europa.configurable.IsParameter;
import com.ojcoleman.europa.configurable.Prototype;
import com.ojcoleman.europa.util.Interval;
import com.ojcoleman.europa.util.IntervalDouble;
import com.ojcoleman.europa.util.IntervalLong;

/**
 * Class for containing meta-data about each value in a numeric vector.
 * 
 * @author O. J. Coleman
 */
public class VectorMetadata extends Configurable {
	/**
	 * Metadata for an empty (zero length/size) vector.
	 */
	public final static VectorMetadata EMPTY = new VectorMetadata(Json.parse("{\"elements\":[]}").asObject());

	@IsConfigurable(description = "The default meta-data for the elements of the Vector.")
	protected ElementDefaults elementDefaults;

	@IsConfigurable(description = "The meta-data for each element of the Vector.")
	protected Element[] elements;

	private Map<String, Element> labelMap;
	private List<String> labels;
	

	/**
	 * Create a VectorMetadata from the given JsonObject. An example JSON object:
	 * 
	 * <pre>
	 * {
	 *   "_defaults" : {"min": -1, "max": 2, "int": true}
	 *   "myParam" : {"min": -1}
	 *   "myOtherParam" : {"min": -2, "max": 3, "int": false}
	 * }
	 * </pre>
	 * 
	 * The "_defaults" specify default values to use if values are not specified for a specific parameter. This is
	 * optional, as is any of the keys within the defaults. If, for a given parameter, no value is specified for the
	 * "min, "max" or "int" keys either explicitly or in the defaults then 0, 1 or false are used respectively.
	 */
	public VectorMetadata(JsonObject config) {
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
	 * Returns the size of the vector(s) this VectorMetadata is intended to contain information about.
	 */
	public int size() {
		return elements.length;
	}

	public boolean isEmpty() {
		return elements.length == 0;
	}

	public Interval<?> bound(int index) {
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

	public Interval<?> bound(String label) {
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
	public static class ElementDefaults extends Configurable {
		@IsParameter(description = "The lower bound for this Vector Element, inclusive.", optional = true)
		protected double min = Double.NaN;

		@IsParameter(description = "The upper bound for this Vector Element, inclusive.", optional = true)
		protected double max = Double.NaN;

		@IsParameter(description = "Indicates if this Vector Element stores an integer value.", optional = true)
		protected boolean isInteger;

		/**
		 * The minimum and maximum bounds for this Vector Element.
		 */
		protected Interval<?> bounds;

		public ElementDefaults(JsonObject config) {
			super(config);
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
		public Interval<?> getBounds() {
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
		@IsParameter(description = "The label for this Vector Element.", optional = true)
		protected String label;

		public Element(JsonObject config) {
			super(config);
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

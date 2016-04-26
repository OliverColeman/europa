package com.ojcoleman.europa.configurable;

import java.lang.reflect.Array;

/**
 * Stores data about the state of a {@link ComponentBase}.
 * 
 * @see ComponentBase#getAllStateData()
 * @see ComponentBase#getState()
 * 
 * @author O. J. Coleman
 */
public class ComponentStateLog implements Comparable<ComponentStateLog> {
	/**
	 * Human-readable category label.
	 */
	public final String category;

	/**
	 * Human-readable sub-category label.
	 */
	public final String subCategory;

	/**
	 * Human-readable state label.
	 */
	public final String label;

	/**
	 * State data, which should generally be convertible to a sensible String with the toString() method (however
	 * numbers, arrays and Collections should be handled sensibly by the caller so that these do not need to be
	 * wrapped).
	 */
	public final Object state;

	/**
	 * If applicable, the unit label (eg "seconds", "MB"). If not applicable this will be set to null.
	 */
	public final String unit;

	/**
	 * Create a ComponentStateLog with no unit label.
	 */
	public ComponentStateLog(String category, String label, Object state) {
		this.category = category;
		this.subCategory = "";
		this.label = label;
		this.state = state;
		this.unit = null;
	}

	/**
	 * Create a ComponentStateLog with a unit label.
	 */
	public ComponentStateLog(String category, String label, Object state, String unit) {
		this.category = category;
		this.subCategory = "";
		this.label = label;
		this.state = state;
		this.unit = unit;
	}

	/**
	 * Create a ComponentStateLog with a sub-category and no unit label.
	 */
	public ComponentStateLog(String category, String subCategory, String label, Object state) {
		this.category = category;
		this.subCategory = subCategory;
		this.label = label;
		this.state = state;
		this.unit = null;
	}

	/**
	 * Create a ComponentStateLog with a sub-category and a unit label.
	 */
	public ComponentStateLog(String category, String subCategory, String label, Object state, String unit) {
		this.category = category;
		this.subCategory = subCategory;
		this.label = label;
		this.state = state;
		this.unit = unit;
	}

	/**
	 * Returns a simple string representation of this state log. If the state is an array or an instance of Iterable
	 * then the individual elements are converted to strings with toString() and separated by commas.
	 */
	public String toString() {
		StringBuilder out = new StringBuilder(category + " - " + (subCategory.equals("") ? "" : subCategory + " - ") + label + ": ");
		if (state.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(state); i++) {
				if (i > 0) {
					out.append(", ");
				}
				out.append(Array.get(state, i).toString());
			}
		} else if (state instanceof Iterable) {
			boolean first = true;
			for (Object o : (Iterable<?>) state) {
				if (!first) {
					out.append(", ");
				} else {
					first = false;
				}
				out.append(o.toString());
			}
		} else {
			out.append(state);
			if (unit != null) {
				out.append(" " + unit);
			}
		}

		return out.toString();
	}

	/**
	 * Compare this ComponentStateLog with the given ComponentStateLog, for sorting purposes. The comparison is
	 * performed by comparing the {@link #category}s first, {@link #subCategory}s second, and finally {@link #label}s.
	 */
	@Override
	public int compareTo(ComponentStateLog other) {
		int catCompare = this.category.compareTo(other.category);
		if (catCompare != 0) {
			return catCompare;
		}
		int subCatCompare = this.subCategory.compareTo(other.subCategory);
		if (subCatCompare != 0) {
			return subCatCompare;
		}
		return this.label.compareTo(other.label);
	}

	/**
	 * Returns true iff the {@link #category}s and {@link #label}s are identical.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof ComponentStateLog) {
			return this.compareTo((ComponentStateLog) other) == 0;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return category.hashCode() ^ subCategory.hashCode() ^ label.hashCode();
	}
}

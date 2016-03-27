package com.ojcoleman.europa.util;

import java.text.DecimalFormat;

import com.ojcoleman.europa.configurable.Parameter;

/**
 * Wrapper for DecimalFormat so it can be used as a {@link Parameter}.
 * @author O. J. Coleman
 *
 */
public class DecimalFormatConfigurable extends DecimalFormat {
	public DecimalFormatConfigurable(String pattern) {
		super(pattern);
	}
	
	public String toString() {
		return "\"" + toPattern() + "\"";  
	}
}

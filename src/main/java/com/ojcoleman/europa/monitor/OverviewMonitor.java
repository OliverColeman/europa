package com.ojcoleman.europa.monitor;

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
import com.ojcoleman.europa.configurable.ComponentBase;
import com.ojcoleman.europa.configurable.ComponentStateLog;
import com.ojcoleman.europa.configurable.Configuration;
import com.ojcoleman.europa.configurable.Observable;
import com.ojcoleman.europa.configurable.Parameter;
import com.ojcoleman.europa.core.Monitor;
import com.ojcoleman.europa.core.Run;

/**
 * 
 * Implementation of {@link Monitor} that prints out a line of statistics every
 * <em>N</em> generations to the standard output (console).
 * 
 * @author O. J. Coleman
 */
public class OverviewMonitor extends FileOrCLIMonitor {
	@Parameter (description = "How many iterations between printing a line of information.", defaultValue = "1")
	protected int period;
	
	@Parameter (description = "How many lines to print before re-printing the header lines, or -1 = never, 0 = only once.", defaultValue = "20")
	protected int headerPerLines;
	
	@Parameter (description = "The precision of printed floating point values.", defaultValue = "6")
	protected int floatPrecision;
	
	@Parameter (description = "The minumum column width.", defaultValue = "6")
	protected int minColumnWidth;
	
	@Parameter (description = "The number of spaces between columns.", defaultValue = "2")
	protected int columnMargin;
	
	
	private Run run;
	
	private Map<String, Category> categories = new TreeMap<>();
	private int linesSinceLastHeader;
	private String categoryHeader;
	private String subCategoryHeader;
	private String labelHeader;
	private String stateValuesFormat;
	private String floatFormat;
	
	public OverviewMonitor(ComponentBase parentComponent, Configuration componentConfig) throws Exception {
		super(parentComponent, componentConfig);
		
		floatFormat = "%0$ " + floatPrecision + "." + floatPrecision + "G";
		
		run = this.getParentComponent(Run.class);
	}
	
	
	@Override
	public void eventOccurred(Observable observed, Object event, Object state) {
		if (event == Run.Event.IterationComplete && run.getCurrentIteration() % period == 0) {
			Multimap<String, ComponentStateLog> stateData = this.getParentComponent(Run.class).getAllStateData();
			
			// Collate all the logs, organised by category, sub-category and label.
			// Detect if any new logs have been added or old logs removed or changed widths.
			boolean changed = false;
			for (ComponentStateLog log : stateData.values()) {
				if (categories.containsKey(log.category)) {
					changed |= categories.get(log.category).addLog(log);
				}
				else {
					categories.put(log.category, new Category(log));
					changed = true;
				}
			}
			
			// Remove any log entries that weren't present in the current batch (unlikely, but possible).
			Iterator<Map.Entry<String, Category>> itr = categories.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<String, Category> entry = itr.next();
				changed |= entry.getValue().removeOld();
				if (entry.getValue().subCategories.isEmpty()) {
					itr.remove();
				}
			}
			
			linesSinceLastHeader++;
			if (headerPerLines != -1 && (headerPerLines > 0 && linesSinceLastHeader == headerPerLines || changed)) {
				if (changed) {
					StringBuilder catSB = new StringBuilder("| ");
					Formatter catF = new Formatter(catSB);
					StringBuilder subCatSB = new StringBuilder("| ");
					Formatter subCatF = new Formatter(subCatSB);
					StringBuilder labelSB = new StringBuilder("| ");
					Formatter labelF = new Formatter(labelSB);
					
					stateValuesFormat = "| ";
					
					int valueIdx = 0;
					boolean firstCat = true;
					for (Map.Entry<String, Category> cat : categories.entrySet()) {
						String name = makeNameFit(cat.getKey(), cat.getValue().getWidth()-3);
						
						catF.format("%0$-" + (cat.getValue().getWidth()-1) + "." + (cat.getValue().getWidth()-3) + "s| ", name);
						
						int subCatRemain = cat.getValue().subCategories.size();
						
						for (Map.Entry<String, SubCategory> subCat : cat.getValue().subCategories.entrySet()) {
							subCatRemain--;
							int width = subCat.getValue().getWidth();
							name = makeNameFit(subCat.getKey(), width-2);
							subCatF.format("%0$-" + width + "." + (width-2) + "s" + (subCatRemain == 0 ? "| " : ""), name);
							
							int labelRemain = subCat.getValue().labels.size();
							for (String label : subCat.getValue().labelsOrdered) {
								labelRemain--;
								Label log = subCat.getValue().labels.get(label);
								width = log.getWidth();
								name = makeNameFit(label, width-2);
								labelF.format("%0$-" + width + "." + (width-2) + "s" + (subCatRemain == 0 && labelRemain == 0 ? "| " : ""), name);
								
								stateValuesFormat += "%" + valueIdx + "$-" + log.getWidth() + "s" + (subCatRemain == 0 && labelRemain == 0 ? "| " : "");
							}
						}
						
						firstCat = false;
					}
					
					categoryHeader = catSB.toString();
					subCategoryHeader = subCatSB.toString();
					labelHeader = labelSB.toString();
				}
				
				write(observed, event, state, categoryHeader);
				write(observed, event, state, subCategoryHeader);
				write(observed, event, state, labelHeader);
				linesSinceLastHeader = 0;
			}
			
			List<String> values = new ArrayList<>(stateData.size());
			for (Map.Entry<String, Category> cat : categories.entrySet()) {
				for (Map.Entry<String, SubCategory> subCat : cat.getValue().subCategories.entrySet()) {
					for (String label : subCat.getValue().labelsOrdered) {
						Label log = subCat.getValue().labels.get(label);
						values.add(log.stringValue);
					}
				}
			}
			StringBuilder valueSB = new StringBuilder();
			Formatter valueF = new Formatter(valueSB);
			valueF.format(stateValuesFormat, values.toArray());
			write(observed, event, state, valueSB);
		}
	}
	
	private String makeNameFit(String name, int width) {
		if (name.length() > width) {
			// Remove repeated letters and white space.
			name = name.replaceAll("([a-zA-Z\\s])\\1{1,}", "$1");
			// If still too long.
			if (name.length() > width) {
				// Remove vowels and similar (except first and last) to shorten but still be readable.
				// Try leaving some in at the beginning for improved legibility.
				String newName = name;
				for (int i = Math.min(10, name.length()); i > 0 && newName.length() > width; i--) {
					// regex from http://stackoverflow.com/a/10916103/1133481
					newName = name.substring(0, i) + name.substring(i).replaceAll("(?<!^)([aeiouAEIOU]|[yY](?![aeiouyAEIOUY]))(?!$)","");
				}
				name = newName;
				// If still too long, chop it.
				if (name.length() > width) {
					name = name.substring(0, width);
				}
			}
		}
		return name;
	}
	
	
	public class Category {
		protected Map<String, SubCategory> subCategories = new TreeMap<>();
		private int width;
		
		public Category(ComponentStateLog log) {
			addLog(log);
		}
		
		protected boolean addLog(ComponentStateLog log) {
			boolean changed = true;
			if (subCategories.containsKey(log.subCategory)) {
				changed = subCategories.get(log.subCategory).addLog(log);
			}
			else {
				subCategories.put(log.subCategory, new SubCategory(log));
			}
			if (changed) {
				calcWidth();
			}
			return changed;
		}
		
		protected int getWidth() {
			return width;
		}
		
		private void calcWidth() {
			width = 1; // 1 to allow for | separator.
			for (SubCategory subCat : subCategories.values()) {
				width += subCat.getWidth();
			}
		}
		
		private boolean removeOld() {
			Iterator<Map.Entry<String, SubCategory>> itr = subCategories.entrySet().iterator();
			boolean changed = false;
			while (itr.hasNext()) {
				Map.Entry<String, SubCategory> entry = itr.next();
				changed |= entry.getValue().removeOld();
				if (entry.getValue().labels.isEmpty()) {
					itr.remove();
				}
			}
			return changed;
		}
	}
	
	public class SubCategory {
		private List<String> labelsOrdered = new ArrayList<>();
		private Map<String, Label> labels = new HashMap<>();
		private int width;
		private int lastLabelIndex;
		
		public SubCategory(ComponentStateLog log) {
			addLog(log);
		}
		
		protected boolean addLog(ComponentStateLog log) {
			boolean changed = true;
			if (labels.containsKey(log.label)) {
				changed = labels.get(log.label).setLog(log);
			}
			else {
				labels.put(log.label, new Label(log));
				labelsOrdered.add(lastLabelIndex, log.label);
			}
			lastLabelIndex++;
			if (changed) {
				calcWidth();
			}
			return changed;
		}
		
		protected int getWidth() {
			return width;
		}
		
		private void calcWidth() {
			width = 0;
			for (Label label : labels.values()) {
				width += label.getWidth();
			}
		}

		protected boolean removeOld() {
			Iterator<Map.Entry<String, Label>> itr = labels.entrySet().iterator();
			boolean changed = false;
			lastLabelIndex = 0;
			while (itr.hasNext()) {
				Map.Entry<String, Label> entry = itr.next();
				if (!entry.getValue().current) {
					changed = true;
					itr.remove();
				}
				else {
					entry.getValue().current = false;
				}
			}
			return changed;
		}
	}
	
	public class Label {
		private ComponentStateLog log;
		protected LogType type;
		protected int maxWidth = minColumnWidth;
		protected String stringValue;
		// Indicates if this log is present in the current batch of logs.
		protected boolean current;
		
		public Label(ComponentStateLog log) {
			setLog(log);
		}
		/** 
		 * Returns true iff the given log has a different type to the previous type or requires a larger width.
		 */ 
		protected boolean setLog(ComponentStateLog log) {
			current = true;
			LogType newType = getType(log);
			this.log = log;
			stringValue = convertToString(newType);
			boolean changed = type != newType || stringValue.length() > maxWidth;
			if (stringValue.length() > maxWidth) {
				maxWidth = stringValue.length();
			}
			this.type = newType;
			return changed;
		}
		
		protected int getWidth() {
			return Math.max(maxWidth, minColumnWidth) + columnMargin;
		}
		
		private String convertToString(LogType type) {
			if (type == LogType.BOOL) {
				return (boolean) log.state ? "True" : "False";
			}
			if (type == LogType.FLOAT) {
				StringBuilder sb = new StringBuilder();
			    Formatter f = new Formatter(sb);
			    f.format(floatFormat, log.state);
			    String str = sb.toString();
			    // Trim numbers < 1 that have zero valued digits immediately after the decimal point 
			    // (the formatter makes them longer to show floatPrecision non-zero digits).
			    if (str.length() > floatPrecision && str.indexOf("E") == -1 && str.indexOf(".") != -1 && str.length() >= floatPrecision+2) {
			    	str = str.substring(0, floatPrecision+2);
			    }
			    str += getUnitString();
			    return str;
			}
			return log.state.toString() + getUnitString();
		}
		
		private String getUnitString() {
			if (log.unit == null) {
				return "";
			}
			String[] parts = log.unit.split(" ");
			String unit = "";
			for (String p : parts) {
				for (int i = 0; i < p.length(); i++) {
					if (i == 0 || !Character.isLowerCase(p.charAt(i))) {
						unit += p.charAt(i);
					}
				}
			}
			return unit;
		}
	}
	
	protected LogType getType(ComponentStateLog log) {
		if (log.state instanceof Boolean) {
			return LogType.BOOL;
		}
		else if (log.state instanceof Integer || log.state instanceof Long) {
			return LogType.INT;
		}
		else if (log.state instanceof Float || log.state instanceof Double) {
			return LogType.FLOAT;
		}
		else if (log.state instanceof CharSequence) {
			return LogType.STRING;
		}
		return LogType.OTHER;
	}
	
	public static enum LogType {
		BOOL, INT, FLOAT, STRING, OTHER
	}
}

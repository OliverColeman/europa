package com.ojcoleman.europa.util;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.TreeMultimap;
import com.ojcoleman.europa.core.Stringable;

/**
 * Utility class that attempts to convert generic objects into human-readable strings.
 * 
 * @author O. J. Coleman
 */
public class Stringer {
	/**
	 * Convert an object to a string, with automatic recursive handling of arrays, Iterable,
	 * Map and {@link Stringable} objects. Recursion will only go 5 levels deep.
	 * 
	 * @param obj The object to convert to a String.
	 */
	public static String toString(Object obj) {
		StringBuilder sb = new StringBuilder();
		toString(obj, sb, 0, 5, new HashSet<>());
		return sb.toString();
	}
	
	
	/**
	 * Convert an object to a string, with automatic recursive handling of arrays, Iterable,
	 * Map and {@link Stringable} objects.
	 * 
	 * @param obj The object to convert to a String.
	 * @param maxLevel The maximum recursion level. -1 for no limit.
	 */
	public static String toString(Object obj, int maxLevel) {
		StringBuilder sb = new StringBuilder();
		toString(obj, sb, 0, maxLevel, new HashSet<>());
		return sb.toString();
	}
	
	
	/**
	 * Convert an object to a string, with automatic recursive handling of arrays, Iterable,
	 * Map and {@link Stringable} objects.
	 * 
	 * @param obj The object to convert to a String.
	 * @param maxLevel The maximum recursion level. -1 for no limit.
	 * @param initialLevel The initial indentation level.
	 */
	public static String toString(Object obj, int maxLevel, int initialLevel) {
		StringBuilder sb = new StringBuilder();
		toString(obj, sb, initialLevel, maxLevel == -1 ? -1 : maxLevel+initialLevel, new HashSet<>());
		return sb.toString();
	}
	
	
	private static void toString(Object obj, StringBuilder sb, int level, int maxLevel, HashSet<Object> covered) {
		sb.append(StringUtils.repeat("    ", level));
		
		if (obj == null) {
			sb.append("<NULL>");
			return;
		}

		if (covered.contains(obj)) {
			sb.append("<RECURSION>");
			return;
		}
		
		if (maxLevel != -1) {
			if (level > maxLevel || 
					(level >= maxLevel && (obj instanceof Map || obj instanceof Stringable || obj instanceof Iterable || obj.getClass().isArray()))) {
				sb.append("<MAXIMUM DEPTH>");
				return;
			}
		}

		covered.add(obj);
		
		if (obj instanceof Map || obj instanceof Stringable) {
			Map<?, ?> map;
			if (obj instanceof Stringable) {
				map = new TreeMap<String, Object>();
				((Map<String, Object>) map).put("class", obj.getClass().getSimpleName());
				((Stringable) obj).getStringableMap((Map<String, Object>) map);
				
			}
			else {
				map = (Map<?, ?>) obj;
			}
			
			sb.append("{");
			String[][] lines = new String[map.size()][];
			String[] keys = new String[map.size()];
			TreeMultimap<Integer, Integer> lineCountToIndex = TreeMultimap.create();
			int entryIdx = 0;
			for (Map.Entry<?, ?> e : map.entrySet()) {
				keys[entryIdx] = e.getKey().toString();
				StringBuilder subSB = new StringBuilder();
				toString(e.getValue(), subSB, 0, maxLevel == -1 ? -1 : maxLevel-(level+1), covered);
				lines[entryIdx] = subSB.toString().split("\n");
				lineCountToIndex.put(lines[entryIdx].length, entryIdx);
				entryIdx++;
			}
			if (lines.length == 1 && lines[0].length == 1) {
				sb.append(" ").append(keys[0]).append(": ").append(lines[0][0]).append(" }");
			}
			else {
				for (Map.Entry<Integer, Integer> lci: lineCountToIndex.entries()) {
					int l = lci.getValue();
					sb.append("\n").append(StringUtils.repeat("    ", level+1)).append(keys[l]).append(":");
					if (lines[l].length == 1) {
						sb.append(" ").append(lines[l][0]);
					}
					else {
						for (String subLine : lines[l]) {
							sb.append("\n").append(StringUtils.repeat("    ", level+2)).append(subLine);
						}
					}
				}
				sb.append("\n").append(StringUtils.repeat("    ", level)).append("}");
			}
		}
		else if (obj instanceof Iterable) {
			sb.append("[");
			for (Object o : (Iterable<?>) obj) {
				sb.append("\n");
				toString(o, sb, level+1, maxLevel, covered);
			}
			sb.append("\n").append(StringUtils.repeat("    ", level)).append("]");
		} else if (obj.getClass().isArray()) {
			sb.append("[");
			for (int i = 0; i < Array.getLength(obj); i++) {
				sb.append("\n");
				toString(Array.get(obj, i), sb, level + 1, maxLevel, covered);
			}
			sb.append("\n").append(StringUtils.repeat("    ", level)).append("]");
		}
		else {
			sb.append(obj);
		}
		
		covered.remove(obj);
	}
}

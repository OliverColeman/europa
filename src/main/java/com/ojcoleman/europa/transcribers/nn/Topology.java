package com.ojcoleman.europa.transcribers.nn;

/**
 * Describes the basic topology of a network.
 */
public enum Topology {
	/**
	 * The network contains recurrent connections or cycles.
	 */
	RECURRENT,
	/**
	 * The network is strictly feed-forward (no recurrent connections or cycles), but the longest and shortest paths
	 * between input and output neurons may not be equal.
	 */
	FEED_FORWARD,
	/**
	 * The network is strictly feed-forward (no recurrent connections or cycles), and is arranged in layers.
	 */
	FEED_FORWARD_LAYERED
}
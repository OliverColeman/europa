package com.ojcoleman.europa.transcribers.nn;

/**
 * Describes the basic topology class of a network.
 */
public enum Topology {
	/**
	 * The network contains recurrent connections or cycles.
	 */
	RECURRENT,
	/**
	 * The network is strictly feed-forward (no recurrent connections or cycles), but the longest and shortest paths
	 * between input and output neurons may not be equal (that is, the neurons may not be arranged in layers that only have connections from one layer to the next).
	 */
	FEED_FORWARD,
	/**
	 * The network is strictly feed-forward (no recurrent connections or cycles), and is arranged in layers.
	 */
	FEED_FORWARD_LAYERED
}
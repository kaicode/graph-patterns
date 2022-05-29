package io.kaicode.graphpattern.domain;

import java.util.*;

public class Pattern {

	private final Set<Node> nodes;
	private final Set<Node> optionalNodes;
	private Set<Node> allNodes;
	private final int hash;
	private int count;
	private float coverage;
	private float accuracy;

	public Pattern(Set<Node> nodes) {
		this.nodes = nodes;
		this.allNodes = nodes;
		this.optionalNodes = new HashSet<>();
		hash = Objects.hash(nodes);// Precompute hashcode for faster comparison
		count = 0;
	}

	public void incrementCount() {
		count++;
	}

	public float getCoverageTimesAccuracy() {
		return coverage * accuracy;
	}

	public void makeNodesOptional(Collection<Node> optional) {
		for (Node optionalNode : optional) {
			if (!nodes.remove(optionalNode)) {
				System.err.println("Failed to remove node, not part of set.");
			}
			optionalNodes.add(optionalNode);
		}
		allNodes = new HashSet<>(nodes);
		allNodes.addAll(optionalNodes);
	}

	public void increaseCoverage(float coverage) {
		System.out.println("Coverage was " + this.coverage);
		this.coverage += coverage;
		System.out.println("Coverage now " + this.coverage);
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public Set<Node> getOptionalNodes() {
		return optionalNodes;
	}

	public Set<Node> getAllNodes() {
		return allNodes;
	}

	public int getCount() {
		return count;
	}

	public void setCoverage(float coverage) {
		this.coverage = coverage;
	}

	public float getCoverage() {
		return coverage;
	}

	public void setAccuracy(float accuracy) {
		int precision = 1000;
		this.accuracy = Math.round(accuracy * precision) / (float) precision;
	}

	public float getAccuracy() {
		return accuracy;
	}

	@Override
	public String toString() {
		return "Pattern{" +
				"hash=" + hash +
				", coverage=" + coverage +
				", accuracy=" + accuracy +
				", nodes=" + nodes +
				", optionalNodes=" + optionalNodes +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pattern pattern = (Pattern) o;
		return hash == pattern.hash;
	}

	@Override
	public int hashCode() {
		return hash;
	}
}

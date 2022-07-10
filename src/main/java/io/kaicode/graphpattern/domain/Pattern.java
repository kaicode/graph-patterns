package io.kaicode.graphpattern.domain;

import java.util.*;

public class Pattern {

	private final Set<Node> nodes;
	private final Set<Node> optionalNodes;
	private Set<Node> allNodes;
	private final int hash;
	private Set<Integer> subsumed;
	private int count;
	private float coverage;
	private float accuracy;

	public Pattern(Set<Node> nodes) {
		this.nodes = nodes;
		this.allNodes = nodes;
		this.optionalNodes = new HashSet<>();
		subsumed = new HashSet<>();
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

	public void subsume(Pattern otherPattern) {
		this.coverage += otherPattern.getCoverage();
		this.count += otherPattern.getCount();
		this.subsumed.add(otherPattern.hash);
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

	public Set<Integer> getSubsumed() {
		return subsumed;
	}

	@Override
	public String toString() {
		return "Pattern{" +
				"hash=" + hash +
				", subsumed=" + subsumed +
				", count=" + count +
				", coverage=" + coverage +
				", accuracy=" + accuracy +
				", nodes=" + collectionToString(10, nodes) +
				", optionalNodes=" + collectionToString(10, optionalNodes) +
				'}';
	}

	private String collectionToString(int maxLength, Set<Node> collection) {
		String collectionString;
		if (collection.size() > maxLength) {
			collectionString = new ArrayList<>(collection).subList(0, maxLength) + " +" + (collection.size() - maxLength);
		} else {
			collectionString = collection.toString();
		}
		return collectionString;
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

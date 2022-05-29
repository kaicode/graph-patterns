package io.kaicode.graphpattern.domain;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class Pattern {

	private final Set<Node> nodes;
	private final int hash;
	private int count;
	private float coverage;
	private float accuracy;

	public Pattern(Set<Node> nodes) {
		this.nodes = Collections.unmodifiableSet(nodes);
		hash = Objects.hash(nodes);// Precompute hashcode for faster comparison
		count = 0;
	}

	public void incrementCount() {
		count++;
	}

	public float getCoverageTimesAccuracy() {
		return coverage * accuracy;
	}

	public Set<Node> getNodes() {
		return nodes;
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
				"coverage=" + coverage +
				", accuracy=" + accuracy +
				", nodes=" + nodes +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pattern pattern = (Pattern) o;
		return nodes.equals(pattern.nodes);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}

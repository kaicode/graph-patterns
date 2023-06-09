package io.kaicode.graphpattern.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Node {

	private final String code;
	private final Set<Node> parents;
	private final Set<Node> children;
	private Set<Node> links;
	private int groupACount = 0;
	private int groupBCount = 0;

	public Node(String code) {
		this.code = code;
		parents = new HashSet<>();
		children = new HashSet<>();
	}

	public Node addChild(Node childNode) {
		children.add(childNode);
		childNode.getParents().add(this);
		return childNode;
	}

	public Node getOrAddChild(String id) {
		for (Node child : getChildren()) {
			if (child.code.equals(id)) {
				return child;
			}
		}
		return addChild(new Node(id));
	}

	public void link(Node linkedNode) {
		if (links == null) {
			links = new HashSet<>();
		}
		links.add(linkedNode);
	}

	public String getCode() {
		return code;
	}

	public Set<Node> getLinks() {
		return links != null ? links : Collections.emptySet();
	}

	public Set<Node> getChildren() {
		return children != null ? children : Collections.emptySet();
	}

	public Set<Node> getParents() {
		return parents;
	}

	public Set<Node> getAncestors() {
		return getAncestors(new HashSet<>());
	}

	private Set<Node> getAncestors(Set<Node> ancestors) {
		ancestors.addAll(parents);
		for (Node parent : parents) {
			parent.getAncestors(ancestors);
		}
		return ancestors;
	}

	public void setGroupACount(int groupACount) {
		this.groupACount = groupACount;
	}

	public void setGroupBCount(int groupBCount) {
		this.groupBCount = groupBCount;
	}

	public float getScaledAggregatedDifference(int groupASize, int groupBSize) {
		int conceptAndDescendantsIncidenceCountInGroupA = getAggregateGroupACount();
		int conceptAndDescendantsIncidenceCountInGroupB = getAggregateGroupBCount();

		float aStrength = conceptAndDescendantsIncidenceCountInGroupA / (float) groupASize;
		float bStrength = conceptAndDescendantsIncidenceCountInGroupB / (float) groupBSize;
		float diff = bStrength - aStrength;
		return diff;
	}

	private int getAggregateGroupACount() {
		int a = groupACount;
		for (Node child : children) {
			a += child.getAggregateGroupACount();
		}
		return a;
	}

	private int getAggregateGroupBCount() {
		int b = groupBCount;
		for (Node child : children) {
			b += child.getAggregateGroupBCount();
		}
		return b;
	}

	@Override
	public String toString() {
		return code;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return code.equals(node.code);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}
}

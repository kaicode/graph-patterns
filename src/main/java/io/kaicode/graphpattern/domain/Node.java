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
	private Set<String> groupAInstanceIds;
	private Set<String> groupBInstanceIds;

	public Node(String code) {
		this.code = code;
		parents = new HashSet<>();
		children = new HashSet<>();
		groupAInstanceIds = new HashSet<>();
		groupBInstanceIds = new HashSet<>();
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

	public void addGroupAInstance(String id) {
		groupAInstanceIds.add(id);
	}

	public void addGroupBInstance(String id) {
		groupBInstanceIds.add(id);
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

	public float getScaledAggregatedDifference(int groupASize, int groupBSize) {
		int conceptAndDescendantsInstanceCountInGroupA = getAggregateGroupACount();
		int conceptAndDescendantsInstanceCountInGroupB = getAggregateGroupBCount();

		float aStrength = conceptAndDescendantsInstanceCountInGroupA / (float) groupASize;
		float bStrength = conceptAndDescendantsInstanceCountInGroupB / (float) groupBSize;
		float diff = bStrength - aStrength;
		return diff;
	}

	private int getAggregateGroupACount() {
		Set<String> instanceIds = new HashSet<>(groupAInstanceIds);
		for (Node child : children) {
			child.collectGroupAInstances(instanceIds);
		}
		return instanceIds.size();
	}

	private void collectGroupAInstances(Set<String> instanceIds) {
		instanceIds.addAll(groupAInstanceIds);
		for (Node child : children) {
			child.collectGroupAInstances(instanceIds);
		}
	}

	private int getAggregateGroupBCount() {
		Set<String> instanceIds = new HashSet<>(groupBInstanceIds);
		for (Node child : children) {
			child.collectGroupBInstances(instanceIds);
		}
		return instanceIds.size();
	}

	private void collectGroupBInstances(Set<String> instanceIds) {
		instanceIds.addAll(groupBInstanceIds);
		for (Node child : children) {
			child.collectGroupAInstances(instanceIds);
		}
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

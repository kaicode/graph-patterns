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
	private final Set<String> groupAInstanceIds;
	private final Set<String> groupBInstanceIds;
	private float groupDifferenceWithSubtypes = -2;
	private float groupDifferenceWithSubtypesBackup = 0;
	private int depth;

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
		return children;
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

	public Set<String> getCodeAndDescendantCodes(Set<String> codes) {
		codes.add(code);
		for (Node child : children) {
			child.getCodeAndDescendantCodes(codes);
		}
		return codes;
	}

	public void calculateGroupDifferenceWithSubtypes(int groupASize, int groupBSize, boolean forceZero) {
		if (forceZero) {
			groupDifferenceWithSubtypes = 0;
		} else if (groupDifferenceWithSubtypes == -2) {
			// Count unique patients for this concept and all descendants, in each group
			int conceptAndDescendantsInstanceCountInGroupA = getAggregateGroupACount();
			int conceptAndDescendantsInstanceCountInGroupB = getAggregateGroupBCount();

			float aStrength = conceptAndDescendantsInstanceCountInGroupA / (float) groupASize;
			float bStrength = conceptAndDescendantsInstanceCountInGroupB / (float) groupBSize;
			this.groupDifferenceWithSubtypes = bStrength - aStrength;
			if (this.groupDifferenceWithSubtypesBackup == 0) {
				this.groupDifferenceWithSubtypesBackup = groupDifferenceWithSubtypes;
			}
		}
	}

	public Float getGroupDifferenceWithSubtypes() {
		return groupDifferenceWithSubtypes;
	}

	public float getGroupDifferenceWithSubtypesBackup() {
		return groupDifferenceWithSubtypesBackup;
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
			child.collectGroupBInstances(instanceIds);
		}
	}

	/**
	 * Set the depth at which the concept first appears within the hierarchy
	 * @param depth depth so far
	 */
	public void recordDepth(int depth) {
		if (this.depth < depth) {
			this.depth = depth;
		}
		for (Node child : getChildren()) {
			child.recordDepth(depth + 1);
		}
	}

	public Integer getDepth() {
		return depth;
	}

	public Set<Node> getDescendantsAndSelf() {
		Set<Node> nodes = new HashSet<>();
		nodes.add(this);
		getDescendants(nodes);
		return nodes;
	}

	private void getDescendants(Set<Node> nodes) {
		for (Node child : children) {
			nodes.add(child);
			child.getDescendants(nodes);
		}
	}

	@Override
	public String toString() {
		return code + " diff:" + groupDifferenceWithSubtypesBackup;
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

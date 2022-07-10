package io.kaicode.graphpattern.domain;

import java.util.*;

public class Node {

	private String id;
	private Set<Node> children;
	private Set<Node> links;

	Node() {

	}

	Node(String id) {
		this.id = id;
	}

	public Node addChild(String id) {
		if (children == null) {
			children = new HashSet<>();
		}
		Node child = new Node(id);
		children.add(child);
		return child;
	}

	public Node getOrAddChild(String id) {
		for (Node child : getChildren()) {
			if (child.id.equals(id)) {
				return child;
			}
		}
		return addChild(id);
	}

	public void link(Node linkedNode) {
		if (links == null) {
			links = new HashSet<>();
		}
		links.add(linkedNode);
	}

	public String getId() {
		return id;
	}

	public Set<Node> getLinks() {
		return links != null ? links : Collections.emptySet();
	}

	public Set<Node> getChildren() {
		return children != null ? children : Collections.emptySet();
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return id.equals(node.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}

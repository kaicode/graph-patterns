package io.kaicode.graphpattern.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Node {

	private final String id;
	private final Set<Node> parents;
	private final Set<Node> children;
	private Set<Node> links;

	public Node(String id) {
		this.id = id;
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
			if (child.id.equals(id)) {
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

	public String getId() {
		return id;
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

package io.kaicode.graphpattern.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphBuilder {

	private final Map<String, Node> allNodes = new HashMap<>();
	private Node rootNode = null;

	public void addChildParentLink(String child, String parent) {
		Node parentNode = allNodes.computeIfAbsent(parent, id -> new Node(parent));
		Node childNode = allNodes.computeIfAbsent(child, id -> new Node(child));
		parentNode.addChild(childNode);

		if (parent.equals("138875005")) {// Keep root for debug browsing
			rootNode = parentNode;
		}
	}

	public Node getNode(String code) {
		return allNodes.get(code);
	}

	public Set<String> getAncestors(String code, int upwardLevelLimit) {
		Set<String> ancestors = new HashSet<>();
		Node node = allNodes.get(code);
		if (node != null) {
			collectAncestors(node, ancestors, upwardLevelLimit);
		}
		return ancestors;
	}

	public Node getRootNode() {
		return rootNode;
	}

	private void collectAncestors(Node node, Set<String> ancestors, int upwardLevelLimit) {
		if (upwardLevelLimit > 0) {
			Set<Node> parents = node.getParents();
			for (Node parent : parents) {
				ancestors.add(parent.getCode());
				collectAncestors(parent, ancestors, upwardLevelLimit - 1);
			}
		}
	}
}

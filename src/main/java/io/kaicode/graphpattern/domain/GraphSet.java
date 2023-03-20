package io.kaicode.graphpattern.domain;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class GraphSet {

	private Graph knowledgeGraph;
	private Set<Graph> instances = new HashSet<>();

	public Graph createKnowledgeGraph() {
		knowledgeGraph = new Graph("K");
		return knowledgeGraph;
	}

	public Graph createInstanceGraph(String id) {
		Graph instance = new Graph(id);
		instances.add(instance);
		return instance;
	}

	public Node getMostLinkedNode() {
		Map<Node, AtomicInteger> nodeLinks = new HashMap<>();
		for (Node instance : instances) {
			for (Node link : instance.getLinks()) {
				nodeLinks.computeIfAbsent(link, i -> new AtomicInteger()).incrementAndGet();
			}
		}
		System.out.println(nodeLinks);

		Map.Entry<Node, AtomicInteger> mostLinked = null;
		for (Map.Entry<Node, AtomicInteger> entry : nodeLinks.entrySet()) {
			if (entry.getValue().get() == 0) {
				continue;
			}
			if (mostLinked == null || mostLinked.getValue().get() < entry.getValue().get()) {
				mostLinked = entry;
			}
		}
		return mostLinked != null ? mostLinked.getKey() : null;
	}

	public List<Graph> generateInstanceGraphs(int quantity, Consumer<Graph> graphCreator) {
		List<Graph> groupRootNodes = new ArrayList<>();
		for (int i = 0; i < quantity; i++) {
			Graph instanceGraph = new Graph("i" + i);
			graphCreator.accept(instanceGraph);
			groupRootNodes.add(instanceGraph);
		}
		return groupRootNodes;
	}

	public Graph getKnowledgeGraph() {
		return knowledgeGraph;
	}
}

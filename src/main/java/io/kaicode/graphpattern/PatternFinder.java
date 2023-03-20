package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PatternFinder {

	public static final Comparator<Pattern> PATTERN_COMPARATOR = Comparator.comparing(Pattern::getCoverageTimesAccuracy).reversed();
	private final Graph knowledgeGraph;
	private boolean debug = false;

	public PatternFinder(Graph knowledgeGraph) {
		this.knowledgeGraph = knowledgeGraph;
	}

	private Map<Integer, Pattern> collectRawPatternsWithCounts(List<Graph> groupGraphs) {
		Map<Integer, Pattern> rawPatterns = new HashMap<>();
		for (Graph groupGraph : groupGraphs) {
			Pattern pattern = new Pattern(groupGraph.getLinks());
			rawPatterns.computeIfAbsent(pattern.hashCode(), (i) -> pattern).incrementCount();
		}
		return rawPatterns;
	}

	public PatternSets differentiateGroupB(List<Graph> groupAGraphs, List<Graph> groupBGraphs) {
		System.out.println("Differentiating groups...");
		Map<Integer, Pattern> groupBPatterns = collectRawPatternsWithCounts(groupBGraphs);
		Map<Integer, Pattern> groupAPatterns = collectRawPatternsWithCounts(groupAGraphs);

		for (Pattern groupBPattern : groupBPatterns.values()) {
			Pattern matchingGroupAPattern = groupAPatterns.get(groupBPattern.hashCode());
			int aCount = matchingGroupAPattern != null ? matchingGroupAPattern.getCount() : 0;
			groupBPattern.setCoverage(groupBPattern.getCount() / (float) groupBGraphs.size());
			groupBPattern.setAccuracy(groupBPattern.getCount() / (float) (aCount + groupBPattern.getCount()));
		}

		List<Pattern> sortedPatterns = new ArrayList<>(groupBPatterns.values());
		sortedPatterns.sort(PATTERN_COMPARATOR);
		return new PatternSets(groupAPatterns.values(), sortedPatterns);
	}

	public List<Pattern> mergeGroups(List<Pattern> groupBPatterns, Collection<Pattern> groupAPatterns) {

		// Make group B patterns more general by finding commonality between them and making some nodes optional.
		// TODO: Before accepting a more general pattern it must be tested against groupA to check that the accuracy has not decreased.

		System.out.println();
		System.out.println("Merging groups");
		Set<Pattern> mergedPatterns = new HashSet<>();
		for (Pattern pattern : groupBPatterns.stream().sorted(Comparator.comparing(p -> p.getNodes().size() * -1)).collect(Collectors.toList())) {
			if (pattern.getAccuracy() == 1f && !mergedPatterns.contains(pattern)) {
				for (Pattern otherPattern : groupBPatterns) {

					if (!otherPattern.equals(pattern) && otherPattern.getAccuracy() == 1f && !mergedPatterns.contains(otherPattern)
							&& pattern.getAllNodes().containsAll(otherPattern.getAllNodes())) {

						if (debug) System.out.println(pattern.hashCode() + " takes " + otherPattern.hashCode());

						Set<Node> optional = new HashSet<>();
						for (Node node : pattern.getNodes()) {
							if (!otherPattern.getNodes().contains(node)) {
								optional.add(node);
							}
						}
						if (!optional.isEmpty()) {
							if (debug) System.out.println("Pattern " + pattern + " make node optional: " + optional);
							pattern.makeNodesOptional(optional);
							if (debug) System.out.println(pattern);
						}
						if (debug) System.out.println("Coverage was " + pattern.getCoverage());
						pattern.subsume(otherPattern);
						if (debug) System.out.println("Coverage now " + pattern.getCoverage());
						if (debug) System.out.println();
						mergedPatterns.add(otherPattern);
					}
				}
			}
		}
		groupBPatterns.removeAll(mergedPatterns);
		groupBPatterns.sort(PATTERN_COMPARATOR);
		return groupBPatterns;
	}

	public List<Pair<Float, Node>> findMostDifferentiatingNodes(List<Graph> groupAGraphs, List<Graph> groupBGraphs, int topNNodes) {
		Map<Node, AtomicInteger> groupANodeCounts = getNodeCounts(groupAGraphs);
		Map<Node, AtomicInteger> groupBNodeCounts = getNodeCounts(groupBGraphs);
		AtomicInteger defaultValue = new AtomicInteger(0);
		List<Pair<Float, Node>> scoredNodes = groupBNodeCounts.entrySet().stream()
				.map(entry -> {
					Node node = entry.getKey();
					float groupBCount = entry.getValue().get();
					float groupACount = groupANodeCounts.getOrDefault(node, defaultValue).get();
					float v = groupBCount / (groupACount + groupBCount);
					return new Pair<>(v, node);
				})
				.sorted(Comparator.comparing(Pair::getFirst, Comparator.reverseOrder()))
				.collect(Collectors.toList());
		return scoredNodes.subList(0, Math.min(scoredNodes.size(), topNNodes));
	}

	private Map<Node, AtomicInteger> getNodeCounts(List<Graph> graphs) {
		Map<Node, AtomicInteger> nodeCounts = new HashMap<>();
		for (Graph graph : graphs) {
			for (Node link : graph.getLinks()) {
				nodeCounts.computeIfAbsent(link, key -> new AtomicInteger()).incrementAndGet();
			}
		}
		return nodeCounts;
	}

}

package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.Graph;
import io.kaicode.graphpattern.domain.Node;
import io.kaicode.graphpattern.domain.Pattern;

import java.util.*;
import java.util.stream.Collectors;

public class PatternFinder {

	public static final Comparator<Pattern> PATTERN_COMPARATOR = Comparator.comparing(Pattern::getCoverageTimesAccuracy).reversed();
	private final Graph knowledgeGraph;

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

	public List<Pattern> differentiateGroupB(List<Graph> groupAGraphs, List<Graph> groupBGraphs) {
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
		return sortedPatterns;
	}

	public Collection<Pattern> mergeGroups(List<Pattern> patterns) {
		System.out.println();
		Set<Pattern> mergedPatterns = new HashSet<>();
		for (Pattern pattern : patterns.stream().sorted(Comparator.comparing(p -> p.getNodes().size() * -1)).collect(Collectors.toList())) {
			if (pattern.getAccuracy() == 1f && !mergedPatterns.contains(pattern)) {
				for (Pattern otherPattern : patterns) {

					if (!otherPattern.equals(pattern) && otherPattern.getAccuracy() == 1f && !mergedPatterns.contains(otherPattern)
							&& pattern.getAllNodes().containsAll(otherPattern.getAllNodes())) {

						System.out.println(pattern.hashCode() + " takes " + otherPattern.hashCode());

						Set<Node> optional = new HashSet<>();
						for (Node node : pattern.getNodes()) {
							if (!otherPattern.getNodes().contains(node)) {
								optional.add(node);
							}
						}
						if (!optional.isEmpty()) {
							System.out.println("Pattern " + pattern + " make node optional: " + optional);
							pattern.makeNodesOptional(optional);
							System.out.println(pattern);
						}
						pattern.increaseCoverage(otherPattern.getCoverage());
						System.out.println();
						mergedPatterns.add(otherPattern);
					}
				}
			}
		}
		patterns.removeAll(mergedPatterns);
		patterns.sort(PATTERN_COMPARATOR);
		return patterns;
	}
}

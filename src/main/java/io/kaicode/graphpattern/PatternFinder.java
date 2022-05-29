package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.Graph;
import io.kaicode.graphpattern.domain.Pattern;

import java.util.*;

public class PatternFinder {

	private final Graph knowledgeGraph;

	public PatternFinder(Graph knowledgeGraph) {
		this.knowledgeGraph = knowledgeGraph;
	}

	public Collection<Pattern> differentiateGroups(List<Graph> groupAGraphs, List<Graph> groupBGraphs) {
		Map<Integer, Pattern> groupBPatterns = collectRawPatternsWithCounts(groupBGraphs);
		Map<Integer, Pattern> groupAPatterns = collectRawPatternsWithCounts(groupAGraphs);

		for (Pattern groupBPattern : groupBPatterns.values()) {
			Pattern matchingGroupAPattern = groupAPatterns.get(groupBPattern.hashCode());
			int aCount = matchingGroupAPattern != null ? matchingGroupAPattern.getCount() : 0;
			groupBPattern.setCoverage(groupBPattern.getCount() / (float) groupBGraphs.size());
			groupBPattern.setAccuracy(groupBPattern.getCount() / (float) (aCount + groupBPattern.getCount()));
		}

		SortedSet<Pattern> sortedPatterns = new TreeSet<>(Comparator.comparing(Pattern::getCoverageTimesAccuracy).reversed());
		sortedPatterns.addAll(groupBPatterns.values());
		return sortedPatterns;
	}

	private Map<Integer, Pattern> collectRawPatternsWithCounts(List<Graph> groupGraphs) {
		Map<Integer, Pattern> rawPatterns = new HashMap<>();
		for (Graph groupGraph : groupGraphs) {
			Pattern pattern = new Pattern(groupGraph.getLinks());
			rawPatterns.computeIfAbsent(pattern.hashCode(), (i) -> pattern).incrementCount();
		}
		return rawPatterns;
	}
}

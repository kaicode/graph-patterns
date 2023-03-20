package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.GraphBuilder;
import io.kaicode.graphpattern.domain.Pair;
import io.kaicode.graphpattern.util.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExperimentalRecursiveSplitting {
	public static final String DIABETIC_FOOT = "371087003";

	// Load knowledge graph
	// Load instance graphs
	// Enrich instance graphs
	// Identify top differentiating node
	// Split groups by top node
	// Identify next top differentiating node
	// Repeat

	// knowledge-graph-child-parent.txt instance-data.txt
	public static void main(String[] args) {
		new ExperimentalRecursiveSplitting().run("knowledge-graph-child-parent.txt", "instance-data.txt", DIABETIC_FOOT);
	}

	private void run(String knowledgeGraphHierarchy, String instanceData, String groupBIndicator) {
		System.out.println("< Graph Pattern Finder >");
		System.out.println();

		GraphBuilder knowledgeGraph = loadKnowledgeGraph(knowledgeGraphHierarchy);

		Map<String, Set<String>> allInstanceGraphs = loadInstanceGraphs(instanceData);

		// Split instance groups
		Map<String, Set<String>> groupAInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> !entry.getValue().contains(groupBIndicator))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<String, Set<String>> groupBInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> entry.getValue().contains(groupBIndicator))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// Collect group B ids for enrichment step
		Set<String> groupBNodeIds = groupBInstanceGraphs.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
		Map<String, Set<String>> groupBAncestorMap = new HashMap<>();
		for (String toEnrich : groupBNodeIds) {
			Set<String> ancestors = knowledgeGraph.getAncestors(toEnrich, 5);
			if (!ancestors.isEmpty()) {
				groupBAncestorMap.put(toEnrich, ancestors);
			}
		}
		groupBAncestorMap.remove(DIABETIC_FOOT);

		// Prepare index for enrichment step
		// - Create inverted index of nodes and instance graphs, add group A and B
		Map<String, List<Set<String>>> nodeInstanceSets = new HashMap<>();
		for (Set<String> instanceNodes : groupAInstanceGraphs.values()) {
			for (String instanceNode : instanceNodes) {
				nodeInstanceSets.computeIfAbsent(instanceNode, v -> new ArrayList<>()).add(instanceNodes);
			}
		}
		for (Set<String> instanceNodes : groupBInstanceGraphs.values()) {
			for (String instanceNode : instanceNodes) {
				nodeInstanceSets.computeIfAbsent(instanceNode, v -> new ArrayList<>()).add(instanceNodes);
			}
		}

		// Apply enrichment
		// - For each node in group B, add the node's ancestors to instance graphs in group A and group B
		int a = 0;
		for (Map.Entry<String, Set<String>> nodeEnrichmentNode : groupBAncestorMap.entrySet()) {
			for (Set<String> instanceNodes : nodeInstanceSets.get(nodeEnrichmentNode.getKey())) {
//				if (a++ < 10) {
//					System.out.printf("Adding ancestors of %s : %s\n", nodeEnrichmentNode.getKey(), nodeEnrichmentNode.getValue());
//				}
				instanceNodes.addAll(nodeEnrichmentNode.getValue());
			}
		}

		Map<String, Set<String>> subGroupA = groupAInstanceGraphs;
		Map<String, Set<String>> subGroupB = groupBInstanceGraphs;
		Set<String> alreadyChosen = new HashSet<>();
		System.out.printf("Group spitting, starting A:%s/B:%s\n", subGroupA.size(), subGroupB.size());
		for (int i = 0; i < 10; i++) {
			List<Pair<String, Float>> mostDifferentiatingNodes = getMostDifferentiatingNodes(subGroupA, subGroupB);
			Optional<Pair<String, Float>> first = mostDifferentiatingNodes.stream()
					.filter(pair -> !DIABETIC_FOOT.equals(pair.getFirst()) && !alreadyChosen.contains(pair.getFirst())).findFirst();
			if (first.isPresent()) {
				String mostDifferentiatingNode = first.get().getFirst();
				int subGroupASizeBefore = subGroupA.size();
				int subGroupBSizeBefore = subGroupB.size();
				subGroupA = filterGroup(subGroupA, entry -> !entry.getValue().contains(mostDifferentiatingNode));
				subGroupB = filterGroup(subGroupB, entry -> !entry.getValue().contains(mostDifferentiatingNode));
//				subGroupA = filterGroup(subGroupA, entry -> entry.getValue().contains(mostDifferentiatingNode));
//				subGroupB = filterGroup(subGroupB, entry -> entry.getValue().contains(mostDifferentiatingNode));
				alreadyChosen.add(mostDifferentiatingNode);
				System.out.printf("Groups spit using %s, A:%s/B:%s reduction of %s%%/%s%%\n",
						mostDifferentiatingNode, subGroupA.size(), subGroupB.size(),
						Math.round((subGroupA.size() / (float) subGroupASizeBefore) * 100), Math.round((subGroupB.size() / (float) subGroupBSizeBefore) * 100));
			} else {
				break;
			}
		}


	}

	private List<Pair<String, Float>> getMostDifferentiatingNodes(Map<String, Set<String>> groupAInstanceGraphs, Map<String, Set<String>> groupBInstanceGraphs) {
		Map<String, Integer> groupAKNodeCounts = getTopKNodeCounts(groupAInstanceGraphs);
		Map<String, Integer> groupBKNodeCounts = getTopKNodeCounts(groupBInstanceGraphs);
		int groupASize = groupAInstanceGraphs.size();
		int groupBSize = groupBInstanceGraphs.size();
		return getMostDifferentiatingNodes(groupAKNodeCounts, groupBKNodeCounts, groupASize, groupBSize);
	}

	private Map<String, Set<String>> filterGroup(Map<String, Set<String>> group, Predicate<Map.Entry<String, Set<String>>> predicate) {
		return group.entrySet().stream().filter(predicate).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private float getNodeSetStrength(Set<String> nodeSet, Map<String, Set<String>> groupAInstanceGraphs, int groupASize,
			Map<String, Set<String>> groupBInstanceGraphs, int groupBSize) {

		long aCount = groupAInstanceGraphs.values().stream().filter(values -> values.containsAll(nodeSet)).count();
		long bCount = groupBInstanceGraphs.values().stream().filter(values -> values.containsAll(nodeSet)).count();
		if (bCount > 0) {
			if (aCount > 0) {
				float aCoverage = aCount / (float) groupASize;
				float bCoverage = bCount / (float) groupBSize;
				return bCoverage - aCoverage;
			} else {
				return 1;
			}
		}
		return 0;
	}

	private List<Pair<String, Float>> getMostDifferentiatingNodes(Map<String, Integer> groupAKNodeCounts, Map<String, Integer> groupBKNodeCounts,
			int groupASize, int groupBSize) {

		Map<String, Float> nodeDiffStrength = new HashMap<>();

		Set<String> allKeys = new HashSet<>(groupBKNodeCounts.keySet());
		allKeys.addAll(groupAKNodeCounts.keySet());
		for (String code : allKeys) {
			int groupACount = groupAKNodeCounts.getOrDefault(code, 0);
			float groupACoverage = groupACount / (float) groupASize;

			int groupBCount = groupBKNodeCounts.getOrDefault(code, 0);
			float groupBCoverage = groupBCount / (float) groupBSize;

			float diffStrength = groupBCoverage - groupACoverage;
			nodeDiffStrength.put(code, diffStrength);
		}

		return nodeDiffStrength.entrySet().stream()
//				.sorted(Map.Entry.comparingByValue())
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	private Map<String, Integer> getTopKNodeCounts(Map<String, Set<String>> groupAInstanceGraphs) {
		Map<String, AtomicInteger> nodeCounts = new HashMap<>();
		for (Set<String> kNodes : groupAInstanceGraphs.values()) {
			for (String kNode : kNodes) {
				nodeCounts.computeIfAbsent(kNode, k -> new AtomicInteger()).incrementAndGet();
			}
		}
		return nodeCounts.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
	}

	private GraphBuilder loadKnowledgeGraph(String knowledgeGraphHierarchy) {
		GraphBuilder graphBuilder = new GraphBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(knowledgeGraphHierarchy))) {
			FileUtils.readAndAssertHeader(reader, "child\tparent");

			String line;
			while ((line = reader.readLine()) != null) {
				String[] pair = line.split("\t");
				graphBuilder.addChildParentLink(pair[0], pair[1]);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return graphBuilder;
	}

	private Map<String, Set<String>> loadInstanceGraphs(String instanceData) {
		Map<String, Set<String>> instanceGraphs = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(instanceData))) {
			FileUtils.readAndAssertHeader(reader, "instance\tyear\tsnomedId");

			String line;
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\t");
				// instance	year	snomedId
				// 0		1		2
				String instanceId = cols[0];
//				String year = cols[1]; Not used yet
				if (cols.length == 3) {// Some data may not be mapped
					String snomedId = cols[2];
					instanceGraphs.computeIfAbsent(instanceId, id -> new HashSet<>()).add(snomedId);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return instanceGraphs;

	}

}

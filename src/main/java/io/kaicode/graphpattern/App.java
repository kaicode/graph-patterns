package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.GraphBuilder;
import io.kaicode.graphpattern.domain.Pair;
import io.kaicode.graphpattern.util.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class App {

	// Load knowledge graph
	// Load instance graphs
	// Load instance cohorts
	// Enrich instance graphs with KG ancestors
	// Identify top x differentiating nodes
	// Identify any pair for each that increases the score?

	// knowledge-graph-child-parent.txt instance-data.txt
	public static void main(String[] args) {
		// Block clustering at grouper nodes
		// Add params:
		// number of highest diff concepts
		// output - list of highest, strength and type
		// output - map of highest code to final code (may be original or enriched)

//		args = new String[]{"data/knowledge-graph-child-parent.txt", "data/instance-data.txt", "371087003", "3"};

		if (args.length != 5) {
			System.out.println("Expecting 5 arguments: path-to-knowledge-graph path-to-instance-data path-to-cohorts groupB-indicator max-upward-level");
			System.exit(1);
		}
		String knowledgeGraphHierarchy = args[0];
		String instanceData = args[1];
		String instanceCohorts = args[2];
		String groupBIndicator = args[3];
		int upwardLevelLimit = Integer.parseInt(args[4]);
		new App().run(knowledgeGraphHierarchy, instanceData, instanceCohorts, groupBIndicator, upwardLevelLimit);
	}

	private void run(String knowledgeGraphHierarchy, String instanceData, String instanceCohorts, String groupBIndicator, int upwardLevelLimit) {
		System.out.println("< Graph Pattern Analysis >");
		System.out.println();

		GraphBuilder knowledgeGraph = loadKnowledgeGraph(knowledgeGraphHierarchy);

		Map<String, Set<String>> allInstanceGraphs = loadInstanceGraphs(instanceData);
		Map<String, Set<String>> cohortInstanceMap = loadCohorts(instanceCohorts);
		Set<String> groupBCohort = cohortInstanceMap.get(groupBIndicator);
		if (groupBCohort == null) {
			throw new RuntimeException("GroupB indicator is not present in the cohorts file");
		}
		if (groupBCohort.isEmpty()) {
			throw new RuntimeException("GroupB cohort is empty");
		}

		// Split instance groups
		Map<String, Set<String>> groupAInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> !groupBCohort.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<String, Set<String>> groupBInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> groupBCohort.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Set<String> nodesToEnrich = groupBInstanceGraphs.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
		Map<String, Set<String>> nodeEnrichmentMap = new HashMap<>();
		for (String toEnrich : nodesToEnrich) {
			Set<String> ancestors = knowledgeGraph.getAncestors(toEnrich, upwardLevelLimit);
			if (!ancestors.isEmpty()) {
				nodeEnrichmentMap.put(toEnrich, ancestors);
			}
		}

		// Create index of all nodes used within instance graphs and the sets of instance nodes
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
		int a = 0;
		for (Map.Entry<String, Set<String>> nodeEnrichmentNode : nodeEnrichmentMap.entrySet()) {
			for (Set<String> instanceNodes : nodeInstanceSets.get(nodeEnrichmentNode.getKey())) {
//				if (a++ < 10) {
//					System.out.printf("Adding ancestors of %s : %s\n", nodeEnrichmentNode.getKey(), nodeEnrichmentNode.getValue());
//				}
				instanceNodes.addAll(nodeEnrichmentNode.getValue());
			}
		}

		Map<String, Integer> groupAKNodeCounts = getTopKNodeCounts(groupAInstanceGraphs);
		Map<String, Integer> groupBKNodeCounts = getTopKNodeCounts(groupBInstanceGraphs);

//		Set<String> nodesToAddParents = groupBKNodeCounts.entrySet().stream()
//				.filter(entry -> entry.getValue() < 100)
//				.map(Map.Entry::getKey)
//				.collect(Collectors.toSet());
//		for (String nodesToAddParent : nodesToAddParents) {
//			Node node = knowledgeGraph.getNode(nodesToAddParent);
//			if (node != null) {
//				Set<Node> parents = node.getParents();
//				for (Node parent : parents) {
//					Set<Node> children = parent.getChildren();
//				}
//			}
//		}

		int groupASize = groupAInstanceGraphs.size();
		int groupBSize = groupBInstanceGraphs.size();
		List<Pair<String, Float>> mostDifferentiatingNodes = getMostDifferentiatingNodes(groupAKNodeCounts, groupBKNodeCounts, groupASize, groupBSize);

		Set<String> nMostDifferentiatingNodes = new HashSet<>();
		Set<String> nodePathsCovered = new HashSet<>();
		nodePathsCovered.add(groupBIndicator);

		File outputDir = new File("output");
		if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
			System.err.println("Failed to create output directory");
		}
		String codeMapFilename = "output/instance-code-cluster-map.txt";
		try (BufferedWriter clusterMap = new BufferedWriter(new FileWriter(codeMapFilename))) {
			clusterMap.write("sourceCode\ttargetCode");
			clusterMap.newLine();
			for (int i = 0; nMostDifferentiatingNodes.size() < Math.min(50, mostDifferentiatingNodes.size() - 2); i++) {
				Pair<String, Float> codeDiffStrength = mostDifferentiatingNodes.get(i);
				String code = codeDiffStrength.getFirst();

				if (!nodePathsCovered.contains(code)) {
					boolean enriched = !nodesToEnrich.contains(code);
					int sourceCodesCount = 0;
					if (enriched) {
						List<String> originalCodes = nodeEnrichmentMap.entrySet().stream()
								.filter(entry -> entry.getValue().contains(code))
								.map(Map.Entry::getKey)
								.collect(Collectors.toList());
						sourceCodesCount = originalCodes.size();
						for (String originalCode : originalCodes) {
							clusterMap.write(originalCode);
							clusterMap.write("\t");
							clusterMap.write(code);
							clusterMap.newLine();
//							System.out.printf("Map %s -> %s%n", originalCode, code);
						}
					}
					String enrichmentMessage = enriched ? format("clustered at ancestor level (includes %s codes)", sourceCodesCount) : "";
					Float diffStrength = codeDiffStrength.getSecond();
					Float diffStrengthDisplay = Math.round(diffStrength * 1000) / 1000f;
					System.out.printf("code %s diff strength %s %s%n", code, diffStrengthDisplay, enrichmentMessage);

					if (!code.equals(groupBIndicator)) {
						nMostDifferentiatingNodes.add(code);
					}
				}
				nodePathsCovered.add(code);
				nodePathsCovered.addAll(knowledgeGraph.getAncestors(code, upwardLevelLimit));
			}
			System.out.println();
			System.out.printf("Map written to %s%n", codeMapFilename);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write cluster map output file.", e);
		}

//		findPairs(groupAInstanceGraphs, groupBInstanceGraphs, groupASize, groupBSize, mostDifferentiatingNodes, nMostDifferentiatingNodes);

		System.out.println();
		System.out.println("Process Complete");
	}

	private Map<String, Set<String>> loadCohorts(String instanceCohorts) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(instanceCohorts));
			String header = reader.readLine();
			int columnCount = header.split("\t").length;
			if (columnCount != 2) {
				System.err.printf("Failed to read cohorts input file. Expected tab separated file with two columns. Got %s columns.", columnCount);
				System.exit(1);
			}

			Map<String, Set<String>> cohortInstanceMap = new HashMap<>();
			String row;
			while ((row = reader.readLine()) != null) {
				String[] columns = row.split("\t");
				String cohort = columns[0];
				String instanceId = columns[1];
				cohortInstanceMap.computeIfAbsent(cohort, i -> new HashSet<>()).add(instanceId);
			}
			return cohortInstanceMap;
		} catch (IOException e) {
			throw new RuntimeException("Failed to read cohort file.", e);
		}
	}

	private void findPairs(Map<String, Set<String>> groupAInstanceGraphs, Map<String, Set<String>> groupBInstanceGraphs, int groupASize, int groupBSize, List<Pair<String, Float>> mostDifferentiatingNodes, Set<String> nMostDifferentiatingNodes) {
		System.out.println("Finding pairs...");
		// Find pairs which are stronger
		List<Pair<Float, Set<String>>> setStrengths = new ArrayList<>();
		Set<Set<String>> sets = new HashSet<>();
		Map<String, Float> mostDifferentiatingNodeStrengths = mostDifferentiatingNodes.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
		for (String node : nMostDifferentiatingNodes) {
			Float originalStrength = mostDifferentiatingNodeStrengths.get(node);
			for (String otherNode : nMostDifferentiatingNodes) {
				if (!node.equals(otherNode)) {
					// Is this pair stronger?
					Set<String> nodeSet = Set.of(node, otherNode);
					float setStrength = getNodeSetStrength(nodeSet, groupAInstanceGraphs, groupASize, groupBInstanceGraphs, groupBSize);
					if (setStrength > originalStrength) {
						if (!sets.contains(nodeSet)) {
							setStrengths.add(new Pair<>(setStrength, nodeSet));
							sets.add(nodeSet);
						}
					}
				}
			}
		}
		System.out.println("----");

		setStrengths.sort(Comparator.comparing(Pair::getFirst, Comparator.reverseOrder()));
		for (int i = 0; i < 10 && i < setStrengths.size(); i++) {
			Pair<Float, Set<String>> floatSetPair = setStrengths.get(i);
			System.out.printf("set %s diff strength %s%n", floatSetPair.getFirst(), floatSetPair.getSecond());
		}
		System.out.println("----");

		// Check group coverage
		Map<String, Set<String>> groupARemaining = new HashMap<>(groupAInstanceGraphs);
		Map<String, Set<String>> groupBRemaining = new HashMap<>(groupBInstanceGraphs);
		for (int i = 0; i < 10 && i < setStrengths.size(); i++) {
			Pair<Float, Set<String>> floatSetPair = setStrengths.get(i);
			int aBeforeSize = groupARemaining.size();
			groupARemaining.entrySet().removeIf(entry -> entry.getValue().containsAll(floatSetPair.getSecond()));
			int aAfterSize = groupARemaining.size();

			int bBeforeSize = groupBRemaining.size();
			groupBRemaining.entrySet().removeIf(entry -> entry.getValue().containsAll(floatSetPair.getSecond()));
			int bAfterSize = groupBRemaining.size();

			System.out.printf("set %s reduced group A by %s%% to %s and B by %s%% to %s%n", floatSetPair.getSecond(),
					((aBeforeSize - aAfterSize) / (float) aBeforeSize) * 100, aAfterSize,
					((bBeforeSize - bAfterSize) / (float) bBeforeSize) * 100, bAfterSize);
		}
		System.out.println("----");
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

			// TODO: Report accuracy as a separate number?

			float diffStrength = groupBCoverage - groupACoverage;
			nodeDiffStrength.put(code, diffStrength);
		}

		return nodeDiffStrength.entrySet().stream()
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
			FileUtils.readAndAssertHeader(reader, "instance\tyear\tcode");

			String line;
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\t");
				// instance	year	code
				// 0		1		2
				String instanceId = cols[0];
//				String year = cols[1]; Not used yet
				if (cols.length == 3) {// Some data may not be mapped
					String code = cols[2];
					instanceGraphs.computeIfAbsent(instanceId, id -> new HashSet<>()).add(code);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return instanceGraphs;

	}

}

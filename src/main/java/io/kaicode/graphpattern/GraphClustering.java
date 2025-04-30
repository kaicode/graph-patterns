package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.GraphBuilder;
import io.kaicode.graphpattern.domain.Node;
import io.kaicode.graphpattern.util.FileUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class GraphClustering {

	public static final Comparator<Node> MAX_DIFF_MAX_DEPTH_COMPARATOR = Comparator
			.comparing(Node::getDepthBoostedAggregateGroupDifference)
			.thenComparing(Node::getDepth)
			.reversed();

	public static final Comparator<Node> MAX_DIFF_MAX_DEPTH_COMPARATOR_USING_DIFF_BACKUP = Comparator
			.comparing(Node::getDepthBoostedAggregateGroupDifferenceBackup)
			.thenComparing(Node::getDepth)
			.reversed();

	public static float depthMultiplier;
	private final float minDiff;
	private final int maxClusters;

	// Load knowledge graph
	// Load instance graphs
	// Load instance cohorts
	// Enrich instance graphs with KG ancestors
	// Identify top x differentiating nodes

	// knowledge-graph-child-parent.txt instance-data.txt
	public static void main(String[] args) {
		// Block clustering at grouper nodes
		// Add params:
		// number of highest diff concepts
		// output - list of highest, strength and type
		// output - map of highest code to final code (may be original or enriched)

		/*
Example params:
knowledge-graph/child-parent.txt
knowledge-graph/code-labels.txt
synthetic-data/patients.txt
synthetic-data/cohorts.txt
"B"
0.1
0.05
10
		 */

		if (args.length != 8) {
			System.out.println("Expecting 8 arguments: " +
					"path-to-knowledge-graph " +
					"path-to-knowledge-graph-labels " +
					"path-to-instance-data " +
					"path-to-cohorts " +
					"groupB-indicator " +
					"depth-boost " +
					"min-difference " +
					"max-clusters");
			System.exit(1);
		}
		String knowledgeGraphHierarchy = args[0];
		String knowledgeGraphLabels = args[1];
		String instanceData = args[2];
		String instanceCohorts = args[3];
		String groupBIndicator = args[4];

		float depthMultiplier = Float.parseFloat(args[5]);
		float minDiff = Float.parseFloat(args[6]);
		int maxClusters = Integer.parseInt(args[7]);

		new GraphClustering(depthMultiplier, minDiff, maxClusters)
				.run(knowledgeGraphHierarchy, knowledgeGraphLabels, instanceData, instanceCohorts, groupBIndicator);
	}

	public GraphClustering(float depthMultiplier, float minDiff, int maxClusters) {
		GraphClustering.depthMultiplier = depthMultiplier;
		this.minDiff = minDiff;
		this.maxClusters = maxClusters;
	}

	public void run(String knowledgeGraphHierarchyPath, String knowledgeGraphLabelsPath, String instanceDataPath, String instanceCohortsPath, String groupBIndicator) {

		System.out.println("< Graph Pattern Analysis >");
		System.out.printf("Depth-multiplier: %s, Minimum-difference: %s, Maximum-clusters: %s%n", depthMultiplier, minDiff, maxClusters);
		System.out.println();
		System.out.println(">> Loading required data");

		GraphBuilder knowledgeGraph = loadKnowledgeGraph(knowledgeGraphHierarchyPath);
		System.out.printf(" %,d nodes loaded%n", knowledgeGraph.getNodeCount());
		Map<String, String> knowledgeGraphLabels = loadKnowledgeGraphLabels(knowledgeGraphLabelsPath);
		System.out.printf(" %,d labels loaded%n", knowledgeGraphLabels.size());
		for (Map.Entry<String, String> labelEntry : knowledgeGraphLabels.entrySet()) {
			String code = labelEntry.getKey();
			Node node = knowledgeGraph.getNode(code);
			if (node != null) {
				node.setLabel(labelEntry.getValue());
			}
		}

		Map<String, Set<String>> allInstanceGraphs = loadInstanceGraphs(instanceDataPath);
		System.out.printf(" %,d patient records loaded%n", allInstanceGraphs.size());
		Map<String, Set<String>> cohortInstanceMap = loadCohorts(instanceCohortsPath);
		Set<String> groupBCohort = cohortInstanceMap.get(groupBIndicator);
		if (groupBCohort == null) {
			throw new RuntimeException("GroupB indicator is not present in the cohorts file");
		}
		if (groupBCohort.isEmpty()) {
			throw new RuntimeException("GroupB cohort is empty");
		}
		System.out.printf(" %,d patient records in cohort B%n", groupBCohort.size());

		List<Node> nodesRankedByDifference = run(knowledgeGraph, allInstanceGraphs, groupBCohort, Collections.emptySet());
		Set<String> allCodesUsed = knowledgeGraph.getAllCodesUsed();
		writeOutputFile(groupBIndicator, knowledgeGraph, knowledgeGraphLabels, nodesRankedByDifference, allCodesUsed);
	}

	public List<Node> run(GraphBuilder knowledgeGraph, Map<String, Set<String>> allInstanceGraphs, Set<String> groupBCohort, Set<String> codeBanList) {
		// Split instance groups
		// Group A is all patients not in Group B
		System.out.println(">> Preprocessing...");
		Map<String, Set<String>> groupAInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> !groupBCohort.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<String, Set<String>> groupBInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> groupBCohort.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// Add instance ids into the graph
		Set<String> notFoundCodes = new HashSet<>();
		for (Map.Entry<String, Set<String>> instanceIdAndConcepts : groupAInstanceGraphs.entrySet()) {
			String id = instanceIdAndConcepts.getKey();
			Set<String> concepts = instanceIdAndConcepts.getValue();
			for (String code : concepts) {
				if (!codeBanList.contains(code)) {
					Node node = knowledgeGraph.getNode(code);
					if (node == null) {
						if (notFoundCodes.add(code)) {// Report only once per code
							System.err.printf("Code %s not found in knowledge graph%n", code);
						}
					} else {
						node.addGroupAInstance(id);
					}
				}
			}
		}
		for (Map.Entry<String, Set<String>> patientAndConcepts : groupBInstanceGraphs.entrySet()) {
			String id = patientAndConcepts.getKey();
			Set<String> concepts = patientAndConcepts.getValue();
			for (String code : concepts) {
				if (!codeBanList.contains(code)) {
					Node node = knowledgeGraph.getNode(code);
					if (node == null) {
						if (notFoundCodes.add(code)) {// Report only once per code
							System.err.printf("Code %s not found in knowledge graph%n", code);
						}
					} else {
						node.addGroupBInstance(id);
					}
				}
			}
		}

		int groupASize = groupAInstanceGraphs.size();
		int groupBSize = groupBInstanceGraphs.size();
		System.out.println("groupASize " + groupASize);
		System.out.println("groupBSize " + groupBSize);

		if (groupASize == 0) {
			throw new RuntimeException("GroupA size is zero");
		}
		if (groupBSize == 0) {
			throw new RuntimeException("GroupB size is zero");
		}

		//		Set<String> codeBanList = new HashSet<>(knowledgeGraph.getRootNode().getChildren().stream().map(Node::getCode).collect(Collectors.toSet()));
		Node rootNode = knowledgeGraph.getRootNode();
		rootNode.recordDepth(0);
		return getNodesRankedByDifferenceAndGain(knowledgeGraph, groupASize, groupBSize, codeBanList);
	}

	private void writeOutputFile(String groupBIndicator, GraphBuilder knowledgeGraph, Map<String, String> knowledgeGraphLabels,
			List<Node> nodesRankedByDifference, Set<String> allCodesUsed) {

		Set<String> chosenNodes = new HashSet<>();
		Map<String, Float> chosenNodeStrengths = new HashMap<>();

		File outputDir = new File("output");
		if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
			System.err.println("Failed to create output directory");
		}
		String clusterFilename = "output/clusters.txt";
		String clusterConceptLabelsFilename = "output/clusters-with-labels.txt";
		String codeMapFilename = "output/instance-code-cluster-map.txt";
		try (BufferedWriter clustersWriter = new BufferedWriter(new FileWriter(clusterFilename));
			 BufferedWriter clustersWithLabelsWriter = new BufferedWriter(new FileWriter(clusterConceptLabelsFilename));
			 BufferedWriter clusterMapWriter = new BufferedWriter(new FileWriter(codeMapFilename))) {

			System.out.println();
			System.out.printf("Top %s differentiating nodes:%n", maxClusters);
			clustersWriter.write("clusterCode\tdisplay\tdiffStrength\tinstanceCount\tincludedCodes");
			clustersWriter.newLine();
			clustersWithLabelsWriter.write("clusterCode\tdisplay\tdiffStrength\tinstanceCount\tincludedCode\tincludedCodeDisplay\tincludedCodeDiffStrength" +
					"\tincludedCodeInstanceCount");
			clustersWithLabelsWriter.newLine();
			for (int i = 0; chosenNodes.size() < Math.min(maxClusters, nodesRankedByDifference.size()); i++) {
				Node node = nodesRankedByDifference.get(i);
				String code = node.getCode();
				if (!code.equals(groupBIndicator)) {
					chosenNodes.add(code);
					Float difference = node.getDepthBoostedAggregateGroupDifferenceBackup();
					chosenNodeStrengths.put(code, difference);
					String label = knowledgeGraphLabels.get(code);
					if (label == null) {
						label = code;
					}
					clustersWriter.write(code);
					clustersWriter.write("\t");
					clustersWriter.write(label);
					clustersWriter.write("\t");
					clustersWriter.write(difference.toString());
					clustersWriter.write("\t");
					clustersWriter.write(Integer.toString(node.getInstanceCount()));
					clustersWriter.write("\t");
					Set<Node> descendantAndSelfSet = node.getDescendantsAndSelf();
					List<Node> descendantsFilteredSorted = descendantAndSelfSet.stream()
							.filter(n -> allCodesUsed.contains(n.getCode()))
							.sorted(MAX_DIFF_MAX_DEPTH_COMPARATOR_USING_DIFF_BACKUP)
							.toList();
					clustersWriter.write(descendantsFilteredSorted.stream().map(Node::getCode).collect(Collectors.joining(",")));
					clustersWriter.newLine();

					for (Node includedCode : descendantsFilteredSorted) {
						clustersWithLabelsWriter.write(code);
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(label);
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(difference.toString());
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(Integer.toString(node.getInstanceCount()));
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(includedCode.getCode());
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(knowledgeGraphLabels.get(includedCode.getCode()));
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(includedCode.getDepthBoostedAggregateGroupDifferenceBackup() + "");
						clustersWithLabelsWriter.write("\t");
						clustersWithLabelsWriter.write(Integer.toString(includedCode.getInstanceCount()));
						clustersWithLabelsWriter.newLine();
					}
					System.out.printf("Node %s diff strength %s %s%n", code, difference, label);
				}
			}
			System.out.println();
			System.out.println();

			// Create cluster-map. Can be used for feature reduction. Codes are mapped to cluster codes.

			clusterMapWriter.write("sourceCode\ttargetCode");
			clusterMapWriter.newLine();
			Set<String> mapped = new HashSet<>();
			for (String codeUsed : allCodesUsed) {
				Node node = knowledgeGraph.getNode(codeUsed);
				Set<Node> ancestorsAndSelf = new HashSet<>(node.getAncestors());
				ancestorsAndSelf.add(node);
				for (Node candidate : ancestorsAndSelf) {
					if (chosenNodes.contains(candidate.getCode())) {
						String destinationCode = candidate.getCode();
						if (mapped.add(codeUsed + ">" + destinationCode)) {
							clusterMapWriter.write(codeUsed);
							clusterMapWriter.write("\t");
							clusterMapWriter.write(destinationCode);
							clusterMapWriter.newLine();

							Float diffStrength = chosenNodeStrengths.get(destinationCode);
							String enrichmentMessage = !codeUsed.equals(destinationCode) ? format("clustered at ancestor level (%s)", destinationCode) : "";
							Float diffStrengthDisplay = Math.round(diffStrength * 1000) / 1000f;
//							System.out.printf("code %s diff strength %s %s%n", codeUsed, diffStrengthDisplay, enrichmentMessage);
						}
					}
				}
			}

			System.out.println();
			System.out.printf("Clusters written to %s%n", clusterFilename);
			System.out.printf("Map written to %s%n", codeMapFilename);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write cluster map output file.", e);
		}

		System.out.println();
		System.out.println("Process Complete");
	}

	private Map<String, Set<String>> loadCohorts(String instanceCohorts) {
		System.out.println("Loading cohort data");
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
				String instanceId = columns[0];
				String cohort = columns[1];
				cohortInstanceMap.computeIfAbsent(cohort, i -> new HashSet<>()).add(instanceId);
			}
			return cohortInstanceMap;
		} catch (IOException e) {
			throw new RuntimeException("Failed to read cohort file.", e);
		}
	}

	private List<Node> getNodesRankedByDifferenceAndGain(GraphBuilder knowledgeGraph, int groupASize, int groupBSize, Set<String> codeBanList) {
		System.out.println("Ranking nodes, please wait a moment...");
		Set<String> allCodesUsed = knowledgeGraph.getAllCodesUsed();
		Set<Node> candidateNodes = calculateNodeDiffAndCollect(allCodesUsed, knowledgeGraph, groupASize, groupBSize, codeBanList, false);

		List<Node> bestNodesWithPositiveScore = new ArrayList<>();
		getBestNodes(bestNodesWithPositiveScore, false, candidateNodes, knowledgeGraph, groupASize, groupBSize, codeBanList);

		List<Node> bestNodesWithNegativeScore = new ArrayList<>();
		getBestNodes(bestNodesWithNegativeScore, true, candidateNodes, knowledgeGraph, groupASize, groupBSize, codeBanList);

		System.out.println();

		List<Node> allBestNodes = new ArrayList<>(bestNodesWithPositiveScore);
		allBestNodes.addAll(bestNodesWithNegativeScore);

		return allBestNodes;
	}

	private void getBestNodes(List<Node> bestNodes, boolean negativeSearch, Set<Node> candidateNodes, GraphBuilder knowledgeGraph, int groupASize, int groupBSize, Set<String> codeBanList) {
		Set<Node> candidateNodesCopy = new HashSet<>(candidateNodes);
		while (bestNodes.size() < maxClusters && !candidateNodesCopy.isEmpty()) {
			List<Node> sortedNodes = new ArrayList<>(candidateNodesCopy);
			sortedNodes.sort(MAX_DIFF_MAX_DEPTH_COMPARATOR);
			if (negativeSearch) {
				Collections.reverse(sortedNodes);
			}
			Node candidateNode = sortedNodes.get(0);
			if (negativeSearch) {
				if (candidateNode.getDepthBoostedAggregateGroupDifference() * -1 < minDiff) {
					break;
				}
			} else {
				if (candidateNode.getDepthBoostedAggregateGroupDifference() < minDiff) {
					break;
				}
			}
			if (!anySubsumption(candidateNode, bestNodes)) {
				bestNodes.add(candidateNode);
				// Clear diff of all descendants
				System.out.print(".");
				calculateNodeDiffAndCollect(candidateNode.getCodeAndDescendantCodes(new HashSet<>()), knowledgeGraph, groupASize, groupBSize, codeBanList, true);
			}
			if (candidateNodesCopy.remove(candidateNode)) {
				System.out.println("Failed to remove");
			}
		}
	}

	private Set<Node> calculateNodeDiffAndCollect(Set<String> allCodesUsed, GraphBuilder knowledgeGraph,
				int groupASize, int groupBSize, Set<String> codeBanList, boolean forceZero) {

		Set<Node> nodes = new HashSet<>();
		for (String codeUsed : allCodesUsed) {
			Node node = knowledgeGraph.getNode(codeUsed);
			node.calculateGroupDifferenceWithSubtypes(groupASize, groupBSize, forceZero);
			if (!codeBanList.contains(node.getCode())) {
				nodes.add(node);
			}
			for (Node ancestor : node.getAncestors()) {
				ancestor.calculateGroupDifferenceWithSubtypes(groupASize, groupBSize, forceZero);
				if (!codeBanList.contains(ancestor.getCode())) {
					nodes.add(ancestor);
				}
			}
		}
		return nodes;
	}

	private boolean anySubsumption(Node node, List<Node> otherNodes) {
		Set<Node> nodeAncestors = node.getAncestors();
		for (Node otherNode : otherNodes) {
			if (nodeAncestors.contains(otherNode) || otherNode.getAncestors().contains(node)) {
				return true;
			}
		}
		return false;
	}

	private GraphBuilder loadKnowledgeGraph(String knowledgeGraphHierarchy) {
		System.out.println("Loading knowledge graph");
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

	private Map<String, String> loadKnowledgeGraphLabels(String knowledgeGraphLabelsPath) {
		System.out.println("Loading graph labels");
		Map<String, String> labels = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(knowledgeGraphLabelsPath))) {
			FileUtils.readAndAssertHeader(reader, "code\tlabel");

			String line;
			while ((line = reader.readLine()) != null) {
				String[] pair = line.split("\t");
				labels.put(pair[0], pair[1]);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return labels;

	}

	private Map<String, Set<String>> loadInstanceGraphs(String instanceData) {
		System.out.println("Reading instance data");
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

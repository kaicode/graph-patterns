package io.kaicode.graphpattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kaicode.graphpattern.domain.GraphBuilder;
import io.kaicode.graphpattern.domain.Node;
import io.kaicode.graphpattern.domain.Pair;
import io.kaicode.graphpattern.util.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class GraphClustering {

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

//		args = new String[]{"data/knowledge-graph-child-parent.txt", "data/instance-data.txt", "371087003", 3, 20};

		// TODO: Ban reporting root concept and top 19

		if (args.length != 7) {
			System.out.println("Expecting 7 arguments: path-to-knowledge-graph path-to-knowledge-graph-labels path-to-instance-data path-to-cohorts groupB-indicator " +
					"max-upward-level max-clusters");
			System.exit(1);
		}
		String knowledgeGraphHierarchy = args[0];
		String knowledgeGraphLabels = args[1];
		String instanceData = args[2];
		String instanceCohorts = args[3];
		String groupBIndicator = args[4];
		int upwardLevelLimit = Integer.parseInt(args[5]);
		int maxClusters = Integer.parseInt(args[6]);
		new GraphClustering().run(knowledgeGraphHierarchy, knowledgeGraphLabels, instanceData, instanceCohorts, groupBIndicator, upwardLevelLimit, maxClusters);
	}

	private void run(String knowledgeGraphHierarchy, String knowledgeGraphLabelsPath, String instanceData, String instanceCohorts, String groupBIndicator, int upwardLevelLimit,
			int maxClusters) {
		System.out.println("< Graph Pattern Analysis >");
		System.out.println();

		GraphBuilder knowledgeGraph = loadKnowledgeGraph(knowledgeGraphHierarchy);
		Map<String, String> knowledgeGraphLabels = loadKnowledgeGraphLabels(knowledgeGraphLabelsPath);

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
		// Group A is all patients not in Group B
		Map<String, Set<String>> groupAInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> !groupBCohort.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<String, Set<String>> groupBInstanceGraphs = allInstanceGraphs.entrySet().stream()
				.filter(entry -> groupBCohort.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<String, Integer> groupANodeCounts = getNodeCounts(groupAInstanceGraphs);
		Map<String, Integer> groupBNodeCounts = getNodeCounts(groupBInstanceGraphs);

		Set<String> notFoundCodes = new HashSet<>();
		for (Map.Entry<String, Integer> nodeCount : groupANodeCounts.entrySet()) {
			String code = nodeCount.getKey();
			Node node = knowledgeGraph.getNode(code);
			if (node == null) {
				if (notFoundCodes.add(code)) {// Report only once per code
					System.err.printf("Code %s not found in knowledge graph%n", code);
				}
			} else {
				node.setGroupACount(nodeCount.getValue());
			}
		}
		for (Map.Entry<String, Integer> nodeCount : groupBNodeCounts.entrySet()) {
			String code = nodeCount.getKey();
			Node node = knowledgeGraph.getNode(code);
			if (node == null) {
				if (notFoundCodes.add(code)) {// Report only once per code
					System.err.printf("Code %s not found in knowledge graph%n", code);
				}
			} else {
				node.setGroupBCount(nodeCount.getValue());
			}
		}

		int groupASize = groupAInstanceGraphs.size();
		int groupBSize = groupBInstanceGraphs.size();
		Set<String> allCodesUsed = new HashSet<>(groupANodeCounts.keySet());
		allCodesUsed.addAll(groupANodeCounts.keySet());
		allCodesUsed.removeAll(notFoundCodes);
		double minDiff = 0.005;
		double minGainToCluster = 0.02;

		// 414916001 |Obesity (disorder)|
		Set<String> obConcepts = null;
		try {
			obConcepts = getConcepts("<!73211009");
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		List<Pair<String, Float>> nodesRankedByDifference = getNodesRankedByDifferenceAndGain(knowledgeGraph, allCodesUsed, groupASize, groupBSize, upwardLevelLimit, minDiff,
				minGainToCluster, obConcepts);

		Set<String> chosenNodes = new HashSet<>();
		Map<String, Float> chosenNodeStrengths = new HashMap<>();

		File outputDir = new File("output");
		if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
			System.err.println("Failed to create output directory");
		}
		String codeMapFilename = "output/instance-code-cluster-map.txt";
		try (BufferedWriter clusterMap = new BufferedWriter(new FileWriter(codeMapFilename))) {
			clusterMap.write("sourceCode\ttargetCode");
			clusterMap.newLine();
			System.out.println();
			System.out.printf("Top %s differentiating nodes:%n", maxClusters);
			for (int i = 0; chosenNodes.size() < Math.min(maxClusters, nodesRankedByDifference.size()); i++) {
				Pair<String, Float> codeDiffStrength = nodesRankedByDifference.get(i);
				String code = codeDiffStrength.getFirst();
				if (!code.equals(groupBIndicator)) {
					chosenNodes.add(code);
					Float strength = codeDiffStrength.getSecond();
					chosenNodeStrengths.put(code, strength);
					String label = knowledgeGraphLabels.get(code);
					System.out.printf("Node %s diff strength %s %s%n", code, strength, label != null ? label : "");
				}
			}
			System.out.println();
			System.out.println();

			// Create cluster-map. Can be used for feature reduction. Codes are mapped to cluster codes.

			Set<String> mapped = new HashSet<>();
			for (String codeUsed : allCodesUsed) {
				Node node = knowledgeGraph.getNode(codeUsed);
				Set<Node> ancestorsAndSelf = new HashSet<>(node.getAncestors());
				ancestorsAndSelf.add(node);
				for (Node candidate : ancestorsAndSelf) {
					if (chosenNodes.contains(candidate.getCode())) {
						String destinationCode = candidate.getCode();
						if (mapped.add(codeUsed + ">" + destinationCode)) {
							clusterMap.write(codeUsed);
							clusterMap.write("\t");
							clusterMap.write(destinationCode);
							clusterMap.newLine();

							Float diffStrength = chosenNodeStrengths.get(destinationCode);
							String enrichmentMessage = !codeUsed.equals(destinationCode) ? format("clustered at ancestor level (%s)", destinationCode) : "";
							Float diffStrengthDisplay = Math.round(diffStrength * 1000) / 1000f;
//							System.out.printf("code %s diff strength %s %s%n", codeUsed, diffStrengthDisplay, enrichmentMessage);
						}
					}
				}
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

	static class Page {
		List<ConceptId> items;

		public Page() {
		}

		public List<ConceptId> getItems() {
			return items;
		}

		public void setItems(List<ConceptId> items) {
			this.items = items;
		}
	}

	static class ConceptId {
		String conceptId;

		public ConceptId() {
		}

		public String getConceptId() {
			return conceptId;
		}
	}

	private Set<String> getConcepts(String ecl) throws JsonProcessingException {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> page = restTemplate.getForEntity("https://snowstorm.ihtsdotools.org/snowstorm/snomed-ct/MAIN/concepts?ecl=" + ecl, String.class);
		ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Page page1 = objectMapper.readValue(page.getBody(), Page.class);
		return page1.items.stream().map(ConceptId::getConceptId).collect(Collectors.toSet());
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
				String instanceId = columns[0];
				String cohort = columns[1];
				cohortInstanceMap.computeIfAbsent(cohort, i -> new HashSet<>()).add(instanceId);
			}
			return cohortInstanceMap;
		} catch (IOException e) {
			throw new RuntimeException("Failed to read cohort file.", e);
		}
	}

	static class Score implements Comparable<Score> {

		private float diff;
		private float gain;

		public Score(float diff, float lastDiff) {
			this.diff = diff;
			this.gain = (diff - lastDiff) / ((diff + lastDiff) / 2f);
		}

		public float getDiff() {
			return diff;
		}

		public float getGain() {
			return gain;
		}

		public float getScore() {
			return diff + gain;
		}

		@Override
		public int compareTo(Score other) {
			return Float.compare(getScore(), other.getScore());
		}
	}

	private List<Pair<String, Float>> getNodesRankedByDifferenceAndGain(GraphBuilder knowledgeGraph, Set<String> allCodesUsed, int groupASize, int groupBSize,
			int upwardLevelLimit, double minDiff, double minGainToCluster, Set<String> obConcepts) {

		Map<String, Float> chosenDiffStrengths = new HashMap<>();
		Map<String, Float> nodeDiffCache = new HashMap<>();
		for (String codeUsed : allCodesUsed) {
			System.out.println("Checking " + codeUsed);

			Node node = knowledgeGraph.getNode(codeUsed);
			float usedNodeDifference = node.getScaledAggregatedDifference(groupASize, groupBSize);

			if (obConcepts.contains(codeUsed) && usedNodeDifference > 0.03) {
				System.out.println("DEBUG!");
			}

			Map<String, Float> ancestorDiffs = collectAncestorDiffs(node, groupASize, groupBSize, upwardLevelLimit, new HashMap<>(), nodeDiffCache);
			String best = node.getCode();
			float bestDiff = usedNodeDifference;
			for (Map.Entry<String, Float> ancestorEntry : ancestorDiffs.entrySet()) {
				String ancestorCode = ancestorEntry.getKey();
				float ancestorDiff = ancestorEntry.getValue();
				if (ancestorDiff < minDiff) {
					continue;
				}
				float gain = calculateGain(usedNodeDifference, ancestorDiff);
				if (ancestorDiff > bestDiff && gain > minGainToCluster) {
					best = ancestorCode;
					bestDiff = ancestorDiff;
				}
			}
			if (bestDiff > minDiff) {
				chosenDiffStrengths.put(best, bestDiff);
				System.out.println("Chose " + best + " with " + bestDiff);
			} else {
				System.out.println("Chose none");
			}

//			nodeDiffStrengths.put(codeUsed, difference);
//			for (Node ancestor : node.getAncestors()) {
//				String ancestorCode = ancestor.getCode();
//				if (!nodeDiffStrengths.containsKey(ancestorCode)) {
//					float ancestorDifference = ancestor.getScaledAggregatedDifference(groupASize, groupBSize);
//					nodeDiffStrengths.put(ancestorCode, ancestorDifference);
//				}
//			}
		}
		return chosenDiffStrengths.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	private float calculateGain(float first, float second) {
		return (second - first) / ((first + second) / 2f);
	}

	private Map<String, Float> collectAncestorDiffs(Node node, int groupASize, int groupBSize, int upwardLevelLimit, Map<String, Float> diffs, Map<String, Float> nodeDiffCache) {
		if (upwardLevelLimit == 0) {
			return diffs;
		}

		for (Node parent : node.getParents()) {
			String parentCode = parent.getCode();
			float parentDiff = nodeDiffCache.computeIfAbsent(parentCode, c -> parent.getScaledAggregatedDifference(groupASize, groupBSize));
			diffs.put(parentCode, parentDiff);
			collectAncestorDiffs(parent, groupASize, groupBSize, upwardLevelLimit -1, diffs, nodeDiffCache);
		}

		return diffs;
	}

	private List<Pair<String, Float>> getNodesRankedByDifference(GraphBuilder knowledgeGraph, Set<String> allCodesUsed, int groupASize, int groupBSize, int upwardLevelLimit) {
		Map<String, Float> nodeDiffStrengths = new HashMap<>();
		for (String codeUsed : allCodesUsed) {
			if (!nodeDiffStrengths.containsKey(codeUsed)) {
				Node node = knowledgeGraph.getNode(codeUsed);
				float difference = node.getScaledAggregatedDifference(groupASize, groupBSize);
				nodeDiffStrengths.put(codeUsed, difference);
				for (Node ancestor : node.getAncestors()) {
					String ancestorCode = ancestor.getCode();
					if (!nodeDiffStrengths.containsKey(ancestorCode)) {
						float ancestorDifference = ancestor.getScaledAggregatedDifference(groupASize, groupBSize);
						nodeDiffStrengths.put(ancestorCode, ancestorDifference);
					}
				}
			}
		}
		return nodeDiffStrengths.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	private Map<String, Integer> getNodeCounts(Map<String, Set<String>> groupAInstanceGraphs) {
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

	private Map<String, String> loadKnowledgeGraphLabels(String knowledgeGraphLabelsPath) {
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

package io.kaicode.graphpattern.old;

import io.kaicode.graphpattern.PatternFinder;
import io.kaicode.graphpattern.domain.Graph;
import io.kaicode.graphpattern.domain.GraphSet;
import io.kaicode.graphpattern.domain.Pattern;
import io.kaicode.graphpattern.domain.PatternSets;

import java.io.*;
import java.util.*;

public class AppOld {

	public static void main(String[] args) {
		System.out.println("< Pattern Finder >");
		System.out.println();
		if (args.length == 2 || args.length == 3) {
			String filepath = args[0];
			String cohortMarker = args[1];
			Set<String> ignoreList = args.length == 3 ? new HashSet<>(Arrays.asList(args[2].split(","))) : Collections.emptySet();
			File file = new File(filepath);

			if (!file.isFile()) {
				System.err.println("Not a file.");
			}

			List<Graph> groupA = new ArrayList<>();
			List<Graph> groupB = new ArrayList<>();
			System.out.println("Loading from file " + file.getAbsolutePath());
			GraphSet graphSet = new GraphSet();
			readInput(file, cohortMarker, ignoreList, groupA, groupB, graphSet);
			System.out.println();
			System.out.println("GroupA size: " + groupA.size());
			System.out.println("GroupB size: " + groupB.size());
			System.out.println();

			PatternFinder patternFinder = new PatternFinder(graphSet.getKnowledgeGraph());
			PatternSets patternSets = patternFinder.differentiateGroupB(groupA, groupB);
			List<Pattern> groupBPatterns = patternSets.getGroupBPatterns();
			System.out.println("Found " + groupBPatterns.size() + " group B patterns");
			System.out.println("Top 10:");
			for (int i = 0; i < Math.min(groupBPatterns.size(), 10); i++) {
				System.out.println("- " + groupBPatterns.get(i));
			}
			System.out.println();

			List<Pattern> groupBPatternsMerged = patternFinder.mergeGroups(groupBPatterns, patternSets.getGroupAPatterns());
			System.out.println("After merging there are " + groupBPatternsMerged.size() + " group B patterns");
			System.out.println("Top 10:");
			for (int i = 0; i < Math.min(groupBPatternsMerged.size(), 10); i++) {
				System.out.println("- " + groupBPatternsMerged.get(i));
			}
			System.out.println();
			System.out.println("Finished");
		} else {
			System.out.println("Need at least two arguments: <instance-graphs-file> <group-b-cohort-marker> [<ignore-list>]");
			System.exit(1);
		}
	}

	private static void readInput(File file, String cohortMarker, Set<String> ignoreList, List<Graph> groupA, List<Graph> groupB, GraphSet graphSet) {
		Graph knowledgeGraph = graphSet.createKnowledgeGraph();

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			Graph instanceGraph = null;
			boolean groupBFlag = false;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					String[] split = line.split(",");
					// Result,Criterion,Year,index
					// 0     ,1        ,2   ,3
					if (split[0].equals("1")) {
						String instance = split[3];
						String knowledgeNodeId = split[1];
						if (ignoreList.contains(knowledgeNodeId)) {
							continue;
						}
						if (instanceGraph == null || !instance.equals(instanceGraph.getId())) {
							// New instance
							if (instanceGraph != null) {
								// Add previous instance to a group
								if (groupBFlag) {
									groupB.add(instanceGraph);
								} else {
									groupA.add(instanceGraph);
								}
								groupBFlag = false;
							}
							instanceGraph = graphSet.createInstanceGraph(instance);
						}
						if (knowledgeNodeId.equals(cohortMarker)) {
							groupBFlag = true;
						} else {
							instanceGraph.link(knowledgeGraph.getOrAddChild(knowledgeNodeId));// Auto-populate flat knowledge graph at this stage.
						}
					}
				}
			}
			if (instanceGraph != null) {
				// Add previous instance to a group
				if (groupBFlag) {
					groupB.add(instanceGraph);
				} else {
					groupA.add(instanceGraph);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

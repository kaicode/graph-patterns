package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class PatternFinderTest {

	private GraphSet graphSet;
	private Graph kRootNode;
	private Node kA;
	private Node kB;
	private Node kC;
	private List<Node> otherHundred;

	@BeforeEach
	public void setup() {
		// Setup
		graphSet = new GraphSet();
		otherHundred = new ArrayList<>();

		// Knowledge graph
		kRootNode = graphSet.createKnowledgeGraph();
		kA = kRootNode.addChild(new Node("A"));
		kB = kRootNode.addChild(new Node("B"));
		kC = kRootNode.addChild(new Node("C"));

		for (int i = 0; i < 100; i++) {
			otherHundred.add(kRootNode.addChild(new Node("i" + i)));
		}
	}

	@Test
	public void testCollectAllGroupBPatternsAndAttemptMerge() {
		// Generate instance graphs for group A
		List<Graph> groupAGraphs = graphSet.generateInstanceGraphs(100, (node) -> {
				// Formula for graph A generation
				if (Math.random() < 0.9) node.link(kA);
				if (Math.random() < 0.5) node.link(kB);
			}
		);
		// Generate instance graphs for group B
		List<Graph> groupBGraphs = graphSet.generateInstanceGraphs(10_000, (node) -> {
				// Formula for graph B generation
				if (Math.random() < 0.9) node.link(kA);
				if (Math.random() < 0.5) node.link(kB);
				if (Math.random() < 0.7) node.link(kC);
			}
		);

		// Test
		PatternFinder patternFinder = new PatternFinder(kRootNode);
		PatternSets patternSets = patternFinder.differentiateGroupB(groupAGraphs, groupBGraphs);
		List<Pattern> sortedPatterns = patternSets.getGroupBPatterns();
		for (Pattern sortedPattern : sortedPatterns) {
			System.out.println(sortedPattern);
		}

		// Assert number of unique patterns
		assertEquals(8, sortedPatterns.size());

		// Assert the coverage, accuracy and nodes of first four highest ranking patterns
		assertPatternAndApproxNumbers(sortedPatterns, 0.3f, 1.0f, "A, C", 0, 1);
		assertPatternAndApproxNumbers(sortedPatterns, 0.3f, 1.0f, "A, B, C", 0, 1);
		assertPatternAndApproxNumbers(sortedPatterns, 0.13f, 0.97f, "A", 2, 3);
		assertPatternAndApproxNumbers(sortedPatterns, 0.13f, 0.97f, "A, B", 2, 3);

		// Test merging groups
		List<Pattern> mergedPatterns = patternFinder.mergeGroups(sortedPatterns, patternSets.getGroupAPatterns());
		for (Pattern mergedPattern : mergedPatterns) {
			System.out.println(mergedPattern);
		}

		// Assert number of unique patterns
		assertEquals(5, mergedPatterns.size());
		Iterator<Pattern> iterator = mergedPatterns.iterator();
		Pattern firstMergedPattern = iterator.next();
		// Coverage increased to 70% after merge, accuracy still 100%
		assertPatternAndApproxNumbers(mergedPatterns, 0.7f, 1.0f, "C", 0);
		// Some optional nodes identified during pattern merging
		assertEquals("[A, B]", firstMergedPattern.getOptionalNodes().toString());
	}

	@Test
	public void testCollectAllGroupBPatternsAndAttemptMergeWithMessyGroups() {
		// Generate instance graphs for group A
		List<Graph> groupAGraphs = graphSet.generateInstanceGraphs(10_000, (node) -> {
					// Formula for graph A generation
					if (Math.random() < 0.9) node.link(kA);
					if (Math.random() < 0.5) node.link(kB);
					for (int i = 0; i < 10; i++) {
						node.link(otherHundred.get((int) Math.round(Math.random() * 98)));
					}
				}
		);
		// Generate instance graphs for group B
		List<Graph> groupBGraphs = graphSet.generateInstanceGraphs(100, (node) -> {
					// Formula for graph B generation
					if (Math.random() < 0.9) node.link(kA);
					if (Math.random() < 0.5) node.link(kB);
					if (Math.random() < 0.7) node.link(kC);
					if (Math.random() < 0.3) {
						node.link(otherHundred.get(1));
					} if (Math.random() < 0.5) {
						node.link(otherHundred.get(2));
					} else {
						node.link(otherHundred.get(3));
					}
					for (int i = 0; i < 10; i++) {
						node.link(otherHundred.get((int) Math.round(Math.random() * 20)));
					}
				}
		);

		// Test
		PatternFinder patternFinder = new PatternFinder(kRootNode);

		List<Pair<Float, Node>> mostDifferentiatingNodes = patternFinder.findMostDifferentiatingNodes(groupAGraphs, groupBGraphs, 10);
		for (Pair<Float, Node> mostDifferentiatingNode : mostDifferentiatingNodes) {
			System.out.println(mostDifferentiatingNode.getFirst() + " - " + mostDifferentiatingNode.getSecond());
		}

//		PatternSets patternSets = patternFinder.differentiateGroupB(groupAGraphs, groupBGraphs);
//		List<Pattern> sortedPatterns = patternSets.getGroupBPatterns();
//		for (Pattern sortedPattern : sortedPatterns) {
//			System.out.println(sortedPattern);
//		}
//
//		// Assert number of unique patterns
//		assertTrue(sortedPatterns.size() > 5_000);

		// Assert the coverage, accuracy and nodes of first four highest ranking patterns
//		assertPatternAndApproxNumbers(sortedPatterns, 0.3f, 1.0f, "A, C", 0, 1);
//		assertPatternAndApproxNumbers(sortedPatterns, 0.3f, 1.0f, "A, B, C", 0, 1);
//		assertPatternAndApproxNumbers(sortedPatterns, 0.13f, 0.97f, "A", 2, 3);
//		assertPatternAndApproxNumbers(sortedPatterns, 0.13f, 0.97f, "A, B", 2, 3);

		// Test merging groups
//		List<Pattern> mergedPatterns = patternFinder.mergeGroups(sortedPatterns, patternSets.getGroupAPatterns());
//		for (Pattern mergedPattern : mergedPatterns) {
//			System.out.println(mergedPattern);
//		}
//
//		// Assert number of unique patterns
//		assertEquals(5, mergedPatterns.size());
//		Iterator<Pattern> iterator = mergedPatterns.iterator();
//		Pattern firstMergedPattern = iterator.next();
//		// Coverage increased to 70% after merge, accuracy still 100%
//		assertPatternAndApproxNumbers(mergedPatterns, 0.7f, 1.0f, "C", 0);
//		// Some optional nodes identified during pattern merging
//		assertEquals("[A, B]", firstMergedPattern.getOptionalNodes().toString());
	}

	private void assertPatternAndApproxNumbers(List<Pattern> actualPatterns, float expectedCoverageApprox, float expectedAccuracyApprox, String expectedPattern,
			int... expectedIndexOneOf) {

		boolean found = false;
		for (int index : expectedIndexOneOf) {
			Pattern actual = actualPatterns.get(index);
			// Test actual pattern is within expected set of patterns
			String actualPattern = actual.getNodes().toString().replace("[", "").replace("]", "");
			if (actualPattern.equals(expectedPattern)) {
				found = true;
				// Test coverage
				float giveOrTake = 0.07f;
				assertEquals(expectedCoverageApprox, actual.getCoverage(), giveOrTake, "Assert coverage");
				// Test accuracy
				assertEquals(expectedAccuracyApprox, actual.getAccuracy(), giveOrTake, "Assert accuracy");
			}
		}
		if (!found) {
			fail(format("The pattern %s was not found at the expected index", expectedPattern));
		}
	}

	@Test
	public void testGetMostLinkedNode() {
		GraphSet graphSet = new GraphSet();

		// Knowledge graph
		Node kRoot = graphSet.createKnowledgeGraph();
		Node kA = kRoot.addChild(new Node("A"));
		Node kB = kRoot.addChild(new Node("B"));

		// Instance
		Node instance1 = graphSet.createInstanceGraph("1");
		instance1.link(kA);

		Node instance2 = graphSet.createInstanceGraph("2");
		instance2.link(kB);

		Node instance3 = graphSet.createInstanceGraph("3");
		instance3.link(kB);

		assertEquals(kB, graphSet.getMostLinkedNode());
	}

}

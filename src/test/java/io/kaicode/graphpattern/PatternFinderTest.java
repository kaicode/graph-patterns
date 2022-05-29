package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.Graph;
import io.kaicode.graphpattern.domain.Node;
import io.kaicode.graphpattern.domain.GraphSet;
import io.kaicode.graphpattern.domain.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternFinderTest {

	private GraphSet graphSet;
	private Graph kRootNode;
	private Node kA;
	private Node kB;
	private Node kC;

	@BeforeEach
	public void setup() {
		// Setup
		graphSet = new GraphSet();

		// Knowledge graph
		kRootNode = graphSet.createKnowledgeGraph();
		kA = kRootNode.addChild("A");
		kB = kRootNode.addChild("B");
		kC = kRootNode.addChild("C");
	}

	@Test
	public void test() {
		// Generate instance graphs for group A
		List<Graph> groupAGraphs = graphSet.generateInstanceGraphs(100, (node) -> {
				if (Math.random() < 0.9) node.link(kA);
				if (Math.random() < 0.5) node.link(kB);
			}
		);
		// Generate instance graphs for group B
		List<Graph> groupBGraphs = graphSet.generateInstanceGraphs(10_000, (node) -> {
				if (Math.random() < 0.9) node.link(kA);
				if (Math.random() < 0.5) node.link(kB);
				if (Math.random() < 0.7) node.link(kC);
			}
		);

		// Test
		PatternFinder patternFinder = new PatternFinder(kRootNode);
		List<Pattern> sortedPatterns = patternFinder.differentiateGroupB(groupAGraphs, groupBGraphs);
		for (Pattern sortedPattern : sortedPatterns) {
			System.out.println(sortedPattern);
		}

		// Assert number of unique patterns
		assertEquals(8, sortedPatterns.size());

		// Assert the coverage, accuracy and nodes of first four highest ranking patterns
		Iterator<Pattern> iterator = sortedPatterns.iterator();
		assertPatternAndApproxNumbers(iterator.next(), 0.3f, 1.0f, "A, C", "A, B, C");
		assertPatternAndApproxNumbers(iterator.next(), 0.3f, 1.0f, "A, C", "A, B, C");
		assertPatternAndApproxNumbers(iterator.next(), 0.13f, 0.97f, "A", "A, B");
		assertPatternAndApproxNumbers(iterator.next(), 0.13f, 0.97f, "A", "A, B");

		// Test merging groups
		Collection<Pattern> mergedPatterns = patternFinder.mergeGroups(sortedPatterns);
		for (Pattern mergedPattern : mergedPatterns) {
			System.out.println(mergedPattern);
		}

		// Assert number of unique patterns
		assertEquals(5, mergedPatterns.size());
		iterator = mergedPatterns.iterator();
		Pattern firstMergedPattern = iterator.next();
		// Coverage increased to 70% after merge, accuracy still 100%
		assertPatternAndApproxNumbers(firstMergedPattern, 0.7f, 1.0f, "C");
		// Some optional nodes identified during pattern merging
		assertEquals("[A, B]", firstMergedPattern.getOptionalNodes().toString());
	}

	private void assertPatternAndApproxNumbers(Pattern actual, float expectedCoverageApprox, float expectedAccuracyApprox, String... expectedPatternSet) {
		// Test actual pattern is within expected set of patterns
		Set<String> expectedPatterns = Arrays.stream(expectedPatternSet).collect(Collectors.toSet());
		String actualPattern = actual.getNodes().toString().replace("[", "").replace("]", "");
		assertTrue(expectedPatterns.contains(actualPattern));

		// Test coverage
		float giveOrTake = 0.07f;
		assertEquals(expectedCoverageApprox, actual.getCoverage(), giveOrTake, "Assert coverage");
		// Test accuracy
		assertEquals(expectedAccuracyApprox, actual.getAccuracy(), giveOrTake, "Assert accuracy");
	}

	@Test
	public void test1() {
		GraphSet graphSet = new GraphSet();

		// Knowledge graph
		Node kRoot = graphSet.createKnowledgeGraph();
		Node kA = kRoot.addChild("A");
		Node kB = kRoot.addChild("B");

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
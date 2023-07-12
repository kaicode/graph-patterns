package io.kaicode.graphpattern;

import io.kaicode.graphpattern.domain.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphClusteringTest {

	@Test
	public void testNodeComparator() {
		List<Node> nodes = new ArrayList<>();
		nodes.add(Node.newTestNode("A", 0.1f, 3));
		nodes.add(Node.newTestNode("B", 0.1f, 2));
		nodes.add(Node.newTestNode("C", 0.01f, 1));
		nodes.add(Node.newTestNode("D", 0.02f, 5));
		nodes.sort(GraphClustering.MAX_DIFF_MAX_DEPTH_COMPARATOR);
		assertEquals("[A diff:0.1 depth:3, B diff:0.1 depth:2, D diff:0.02 depth:5, C diff:0.01 depth:1]", Arrays.toString(nodes.toArray()));
	}

}

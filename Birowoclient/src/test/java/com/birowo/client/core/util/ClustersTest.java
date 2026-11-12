package com.birowo.client.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests of the grouping of blocks that touch each other.
 */
class ClustersTest {
	@Test
	void putsBlocksThatTouchIntoOneGroup() {
		List<Clusters.Point> points = List.of(
				point(0, 0, 0, 1, NeighbourFaces.EAST),
				point(1, 0, 0, 1, NeighbourFaces.WEST));

		List<Clusters.Cluster> clusters = Clusters.of(points);

		assertEquals(1, clusters.size());
		assertEquals(2, clusters.get(0).size());
		assertEquals(1.0D, clusters.get(0).x(), 0.0001D);
		assertEquals(0.5D, clusters.get(0).y(), 0.0001D);
		assertEquals(0.5D, clusters.get(0).z(), 0.0001D);
	}

	@Test
	void keepsBlocksOfAnotherKindApart() {
		List<Clusters.Point> points = List.of(
				point(0, 0, 0, 1, NeighbourFaces.EAST),
				point(1, 0, 0, 2, NeighbourFaces.WEST));

		assertEquals(2, Clusters.of(points).size());
	}

	@Test
	void keepsBlocksThatDoNotTouchApart() {
		List<Clusters.Point> points = List.of(
				point(0, 0, 0, 1, NeighbourFaces.DOWN),
				point(1, 0, 0, 1, NeighbourFaces.UP));

		assertEquals(2, Clusters.of(points).size());
	}

	@Test
	void keepsBlocksApartWhenOnlyOneOfThemIsTouched() {
		List<Clusters.Point> points = List.of(
				point(0, 0, 0, 1, NeighbourFaces.EAST),
				point(1, 0, 0, 1, 0));

		assertEquals(2, Clusters.of(points).size());
	}

	@Test
	void findsEveryGroupOfAWholeVein() {
		List<Clusters.Point> points = List.of(
				point(0, 0, 0, 1, NeighbourFaces.EAST | NeighbourFaces.UP),
				point(1, 0, 0, 1, NeighbourFaces.WEST | NeighbourFaces.UP),
				point(1, 1, 0, 1, NeighbourFaces.DOWN),
				point(9, 9, 9, 1, 0));

		List<Clusters.Cluster> clusters = Clusters.of(points);

		assertEquals(2, clusters.size());
		assertEquals(3, clusters.get(0).size());
		assertEquals(1, clusters.get(1).size());
	}

	/**
	 * @param x     the horizontal place of the block
	 * @param y     the height of the block
	 * @param z     the depth of the block
	 * @param kind  the kind of the block
	 * @param faces the sides of the block that are covered
	 * @return the block
	 */
	private static Clusters.Point point(int x, int y, int z, int kind, int faces) {
		return new Clusters.Point(x, y, z, kind, faces);
	}
}

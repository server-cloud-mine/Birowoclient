package com.birowo.client.core.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits blocks into groups of blocks that touch each other.
 *
 * <p>A group is a set of blocks of the same kind that are joined to each other. The overlay uses
 * a group to draw a single line to the middle of it, rather than a line to every block, which
 * keeps the screen readable when a whole vein of ore is in sight.
 *
 * <p>The work is pure arithmetic on places, so it lives in the core and is tested without the
 * game.
 */
public final class Clusters {
	private Clusters() {
	}

	/**
	 * One block that is in a group.
	 *
	 * @param x     the horizontal place of the block
	 * @param y     the height of the block
	 * @param z     the depth of the block
	 * @param kind  what kind of block it is; blocks of another kind are never in the same group
	 * @param faces the sides of the block that are covered by a block of the same kind
	 */
	public record Point(int x, int y, int z, int kind, int faces) {
	}

	/**
	 * One group of blocks.
	 *
	 * @param x    the horizontal middle of the group
	 * @param y    the height of the middle of the group
	 * @param z    the depth of the middle of the group
	 * @param kind what kind of block the group is made of
	 * @param size how many blocks the group holds
	 */
	public record Cluster(double x, double y, double z, int kind, int size) {
	}

	/**
	 * Splits the blocks into groups.
	 *
	 * @param points the blocks that were found
	 * @return the groups, in the order they were found in
	 */
	public static List<Cluster> of(List<Point> points) {
		Map<Long, Point> byPlace = new HashMap<>();
		Map<Long, Point> visited = new HashMap<>();
		List<Cluster> clusters = new ArrayList<>();

		for (Point point : points) {
			byPlace.putIfAbsent(key(point.x(), point.y(), point.z()), point);
		}

		for (Point point : points) {
			long key = key(point.x(), point.y(), point.z());

			if (visited.containsKey(key)) {
				continue;
			}

			List<Point> group = new ArrayList<>();
			ArrayDeque<Point> pending = new ArrayDeque<>();

			pending.add(point);
			visited.put(key, point);

			double sumX = 0.0D;
			double sumY = 0.0D;
			double sumZ = 0.0D;

			while (!pending.isEmpty()) {
				Point current = pending.poll();

				group.add(current);
				sumX += current.x() + 0.5D;
				sumY += current.y() + 0.5D;
				sumZ += current.z() + 0.5D;

				for (Point neighbour : neighbours(current, byPlace)) {
					long neighbourKey = key(neighbour.x(), neighbour.y(), neighbour.z());

					if (visited.containsKey(neighbourKey)) {
						continue;
					}

					visited.put(neighbourKey, neighbour);
					pending.add(neighbour);
				}
			}

			clusters.add(new Cluster(sumX / group.size(), sumY / group.size(), sumZ / group.size(),
					point.kind(), group.size()));
		}

		return List.copyOf(clusters);
	}

	/**
	 * @param point  the block whose neighbours are looked for
	 * @param byPlace every block that was found, by its place
	 * @return the blocks that touch the given block and are of the same kind
	 */
	private static List<Point> neighbours(Point point, Map<Long, Point> byPlace) {
		List<Point> neighbours = new ArrayList<>(6);

		add(neighbours, byPlace, point, point.x(), point.y() - 1, point.z(), NeighbourFaces.DOWN);
		add(neighbours, byPlace, point, point.x(), point.y() + 1, point.z(), NeighbourFaces.UP);
		add(neighbours, byPlace, point, point.x(), point.y(), point.z() - 1, NeighbourFaces.NORTH);
		add(neighbours, byPlace, point, point.x(), point.y(), point.z() + 1, NeighbourFaces.SOUTH);
		add(neighbours, byPlace, point, point.x() - 1, point.y(), point.z(), NeighbourFaces.WEST);
		add(neighbours, byPlace, point, point.x() + 1, point.y(), point.z(), NeighbourFaces.EAST);

		return neighbours;
	}

	/**
	 * Adds a block to the list, when it touches the given block.
	 *
	 * @param list    the list the block is added to
	 * @param byPlace every block that was found, by its place
	 * @param point   the block whose neighbour is looked for
	 * @param x       the horizontal place of the neighbour
	 * @param y       the height of the neighbour
	 * @param z       the depth of the neighbour
	 * @param face    the side of the block the neighbour lies on
	 */
	private static void add(List<Point> list, Map<Long, Point> byPlace, Point point, int x, int y, int z, int face) {
		if (!NeighbourFaces.has(point.faces(), face)) {
			return;
		}

		Point neighbour = byPlace.get(key(x, y, z));

		if (neighbour == null || neighbour.kind() != point.kind()) {
			return;
		}

		if (!NeighbourFaces.has(neighbour.faces(), NeighbourFaces.opposite(face))) {
			return;
		}

		list.add(neighbour);
	}

	/**
	 * @param x the horizontal place of a block
	 * @param y the height of a block
	 * @param z the depth of a block
	 * @return the place of the block as a single number
	 */
	private static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
	}
}

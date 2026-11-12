package com.birowo.client.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Joins the blocks that touch each other into veins.
 *
 * <p>A vein is a set of blocks of the same kind that are joined to each other. The overlay draws
 * one line to the middle of a vein rather than a line to every block of it, so that a whole
 * lode of ore does not fill the screen with lines.
 *
 * <p>The joining is done with a union-find over the blocks, which touches every block a handful
 * of times and nothing else. The places of the blocks are held in a table of its own, so that no
 * object is made for a block while the veins are built: the veins are built again every now and
 * then, and the blocks of the world are many.
 *
 * <p>The work is pure arithmetic on places, so it lives in the core and is tested without the
 * game.
 */
public final class Veins {
	private Veins() {
	}

	/**
	 * One vein of blocks.
	 *
	 * @param x    the horizontal middle of the vein
	 * @param y    the height of the middle of the vein
	 * @param z    the depth of the middle of the vein
	 * @param kind what kind of block the vein is made of
	 * @param size how many blocks the vein holds
	 */
	public record Vein(double x, double y, double z, int kind, int size) {
	}

	/**
	 * The result of joining the blocks.
	 *
	 * @param veinOf the vein every block belongs to, by the number of the block
	 * @param veins  the veins themselves
	 */
	public record Result(int[] veinOf, List<Vein> veins) {
	}

	/**
	 * Joins the blocks into veins.
	 *
	 * <p>Two blocks are joined when they touch each other and are of the same kind. The sides
	 * that are covered are what says whether two blocks touch, so the sides of every block are
	 * all that is needed.
	 *
	 * @param xs    the horizontal place of every block
	 * @param ys    the height of every block
	 * @param zs    the depth of every block
	 * @param kinds what kind of block every block is; blocks of another kind are never joined
	 * @param faces the sides of every block that are covered by a block of the same kind
	 * @return the vein of every block and the veins themselves
	 */
	public static Result group(int[] xs, int[] ys, int[] zs, int[] kinds, int[] faces) {
		int count = xs.length;
		int[] parent = new int[count];
		int[] size = new int[count];
		Places places = new Places(count);

		for (int index = 0; index < count; index++) {
			parent[index] = index;
			size[index] = 1;
			places.put(key(xs[index], ys[index], zs[index]), index);
		}

		for (int index = 0; index < count; index++) {
			int covered = faces[index];

			if (NeighbourFaces.has(covered, NeighbourFaces.DOWN)) {
				join(parent, size, index, places.get(key(xs[index], ys[index] - 1, zs[index])));
			}

			if (NeighbourFaces.has(covered, NeighbourFaces.UP)) {
				join(parent, size, index, places.get(key(xs[index], ys[index] + 1, zs[index])));
			}

			if (NeighbourFaces.has(covered, NeighbourFaces.NORTH)) {
				join(parent, size, index, places.get(key(xs[index], ys[index], zs[index] - 1)));
			}

			if (NeighbourFaces.has(covered, NeighbourFaces.SOUTH)) {
				join(parent, size, index, places.get(key(xs[index], ys[index], zs[index] + 1)));
			}

			if (NeighbourFaces.has(covered, NeighbourFaces.WEST)) {
				join(parent, size, index, places.get(key(xs[index] - 1, ys[index], zs[index])));
			}

			if (NeighbourFaces.has(covered, NeighbourFaces.EAST)) {
				join(parent, size, index, places.get(key(xs[index] + 1, ys[index], zs[index])));
			}
		}

		return collect(parent, xs, ys, zs, kinds);
	}

	/**
	 * Gathers the veins out of the joined blocks.
	 *
	 * @param parent the parent of every block in the join
	 * @param xs     the horizontal place of every block
	 * @param ys     the height of every block
	 * @param zs     the depth of every block
	 * @param kinds  what kind of block every block is
	 * @return the vein of every block and the veins themselves
	 */
	private static Result collect(int[] parent, int[] xs, int[] ys, int[] zs, int[] kinds) {
		int count = xs.length;
		int[] rootOf = new int[count];
		int[] veinOf = new int[count];
		int[] veinSize = new int[count];
		double[] sumX = new double[count];
		double[] sumY = new double[count];
		double[] sumZ = new double[count];
		int[] veinKind = new int[count];

		for (int index = 0; index < count; index++) {
			int root = find(parent, index);

			rootOf[index] = root;
			veinSize[root]++;
			sumX[root] += xs[index] + 0.5D;
			sumY[root] += ys[index] + 0.5D;
			sumZ[root] += zs[index] + 0.5D;

			if (veinSize[root] == 1) {
				veinKind[root] = kinds[index];
			}
		}

		int veins = 0;
		int[] numberOfRoot = new int[count];

		for (int index = 0; index < count; index++) {
			int root = rootOf[index];

			if (numberOfRoot[root] == 0) {
				numberOfRoot[root] = ++veins;
			}

			veinOf[index] = numberOfRoot[root] - 1;
		}

		List<Vein> list = new ArrayList<>(veins);

		for (int root = 0; root < count; root++) {
			if (numberOfRoot[root] == 0) {
				continue;
			}

			int blocks = veinSize[root];

			list.add(new Vein(sumX[root] / blocks, sumY[root] / blocks, sumZ[root] / blocks, veinKind[root],
					blocks));
		}

		return new Result(veinOf, List.copyOf(list));
	}

	/**
	 * Joins two blocks into one vein.
	 *
	 * @param parent the parent of every block in the join
	 * @param size   how many blocks are in the join of every block
	 * @param first  one of the blocks
	 * @param second the other block, or {@code -1} when it is not among the blocks
	 */
	private static void join(int[] parent, int[] size, int first, int second) {
		if (second < 0) {
			return;
		}

		int rootFirst = find(parent, first);
		int rootSecond = find(parent, second);

		if (rootFirst == rootSecond) {
			return;
		}

		if (size[rootFirst] < size[rootSecond]) {
			int swap = rootFirst;
			rootFirst = rootSecond;
			rootSecond = swap;
		}

		parent[rootSecond] = rootFirst;
		size[rootFirst] += size[rootSecond];
	}

	/**
	 * @param parent the parent of every block in the join
	 * @param index  the block that is looked for
	 * @return the block that stands at the head of the join of the given block
	 */
	private static int find(int[] parent, int index) {
		int root = index;

		while (parent[root] != root) {
			root = parent[root];
		}

		while (parent[index] != root) {
			int next = parent[index];
			parent[index] = root;
			index = next;
		}

		return root;
	}

	/**
	 * @param x the horizontal place of a block
	 * @param y the height of a block
	 * @param z the depth of a block
	 * @return the place of the block as a single number
	 */
	public static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
	}

	/**
	 * A table of places, held in a plain array so that no object is made for a place.
	 *
	 * <p>The table is opened by the power of two that is just above the number of places, so
	 * that a place is found by a single mask. A place that is not in the table is told apart from
	 * a place that is, by the sign of its number.
	 */
	private static final class Places {
		private final long[] keys;
		private final int[] values;
		private final int mask;

		/**
		 * @param expected how many places the table is expected to hold
		 */
		Places(int expected) {
			int size = 16;

			while (size < expected * 2) {
				size <<= 1;
			}

			this.keys = new long[size];
			this.values = new int[size];
			this.mask = size - 1;

			for (int index = 0; index < size; index++) {
				values[index] = -1;
			}
		}

		/**
		 * Puts a place in the table.
		 *
		 * @param key  the place
		 * @param value what the place stands for
		 */
		void put(long key, int value) {
			int slot = (int) (hash(key) & mask);

			while (values[slot] != -1) {
				if (keys[slot] == key) {
					values[slot] = value;
					return;
				}

				slot = (slot + 1) & mask;
			}

			keys[slot] = key;
			values[slot] = value;
		}

		/**
		 * @param key the place that is looked for
		 * @return what the place stands for, or {@code -1} when the place is not in the table
		 */
		int get(long key) {
			int slot = (int) (hash(key) & mask);

			while (values[slot] != -1) {
				if (keys[slot] == key) {
					return values[slot];
				}

				slot = (slot + 1) & mask;
			}

			return -1;
		}

		/**
		 * @param key the place
		 * @return a number that is spread out over the whole range
		 */
		private static long hash(long key) {
			long mixed = key * 0x9E3779B97F4A7C15L;
			mixed ^= mixed >>> 29;

			return mixed & 0x7FFFFFFFFFFFFFFFL;
		}
	}
}

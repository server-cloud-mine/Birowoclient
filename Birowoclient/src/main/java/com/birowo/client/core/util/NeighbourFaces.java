package com.birowo.client.core.util;

/**
 * The six sides of a block, as bits that can be put into a single number.
 *
 * <p>The bits are used to remember which sides of a block are covered by a block of the same
 * kind, so that the overlay does not draw the edges and the faces that are hidden inside a group
 * of blocks.
 */
public final class NeighbourFaces {
	/**
	 * The side at the bottom of the block.
	 */
	public static final int DOWN = 1;

	/**
	 * The side at the top of the block.
	 */
	public static final int UP = 2;

	/**
	 * The side at the north of the block, which is the side at the smaller depth.
	 */
	public static final int NORTH = 4;

	/**
	 * The side at the south of the block, which is the side at the greater depth.
	 */
	public static final int SOUTH = 8;

	/**
	 * The side at the west of the block, which is the side at the smaller horizontal place.
	 */
	public static final int WEST = 16;

	/**
	 * The side at the east of the block, which is the side at the greater horizontal place.
	 */
	public static final int EAST = 32;

	/**
	 * All six sides, in the order they are drawn.
	 */
	public static final int[] ALL = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

	private NeighbourFaces() {
	}

	/**
	 * @param face one of the sides
	 * @return the side that lies opposite of it
	 */
	public static int opposite(int face) {
		return switch (face) {
			case DOWN -> UP;
			case UP -> DOWN;
			case NORTH -> SOUTH;
			case SOUTH -> NORTH;
			case WEST -> EAST;
			case EAST -> WEST;
			default -> 0;
		};
	}

	/**
	 * @param faces the sides, as bits
	 * @param face  one of the sides
	 * @return whether that side is among them
	 */
	public static boolean has(int faces, int face) {
		return (faces & face) != 0;
	}
}

package com.birowo.client.core.util;

/**
 * How far the world is looked through.
 *
 * <p>The radius of the overlay is given in blocks, because that is the measure a player thinks
 * in: a chest that is sixty blocks away is close, a chest that is two hundred blocks away is not
 * worth seeing.
 *
 * <p>The world is looked through chunk by chunk, so the radius is turned into a number of chunks.
 * The number is never larger than the distance the game itself shows the world at, because a
 * chunk that is not loaded holds nothing that could be shown anyway.
 */
public final class ScanReach {
	/**
	 * How many blocks a chunk is wide.
	 */
	public static final int BLOCKS_PER_CHUNK = 16;

	/**
	 * The greatest radius that is looked at, in blocks.
	 */
	public static final int MAXIMUM_RADIUS = 256;

	private ScanReach() {
	}

	/**
	 * @param radiusBlocks how far the blocks are looked for, in blocks
	 * @param viewDistance how far the game itself shows the world, in chunks
	 * @return how many chunks around the player are looked through
	 */
	public static int chunks(int radiusBlocks, int viewDistance) {
		int blocks = Math.min(MAXIMUM_RADIUS, Math.max(0, radiusBlocks));
		int reach = (blocks + BLOCKS_PER_CHUNK - 1) / BLOCKS_PER_CHUNK;

		return Math.max(1, Math.min(reach, Math.max(1, viewDistance)));
	}
}

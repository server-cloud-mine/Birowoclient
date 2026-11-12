package com.birowo.client.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The reach of the overlay decides how much of the world is looked through, so it is tested
 * without a game.
 */
class ScanReachTest {
	@Test
	void looksThroughOneChunkForEverySixteenBlocks() {
		assertEquals(1, ScanReach.chunks(16, 12));
		assertEquals(2, ScanReach.chunks(32, 12));
		assertEquals(4, ScanReach.chunks(64, 12));
	}

	@Test
	void roundsAPartOfAChunkUp() {
		// A single block beyond the edge of a chunk still belongs to the next chunk.
		assertEquals(2, ScanReach.chunks(17, 12));
		assertEquals(3, ScanReach.chunks(33, 12));
	}

	@Test
	void neverLooksFurtherThanTheGameShows() {
		// A chunk that is not loaded holds nothing that could be shown anyway.
		assertEquals(12, ScanReach.chunks(256, 12));
		assertEquals(2, ScanReach.chunks(64, 2));
	}

	@Test
	void alwaysLooksThroughTheChunkThePlayerIsIn() {
		assertEquals(1, ScanReach.chunks(0, 12));
		assertEquals(1, ScanReach.chunks(-10, 12));
	}

	@Test
	void neverLooksFurtherThanTheGreatestRadius() {
		assertEquals(ScanReach.MAXIMUM_RADIUS / ScanReach.BLOCKS_PER_CHUNK,
				ScanReach.chunks(100000, 64));
	}
}

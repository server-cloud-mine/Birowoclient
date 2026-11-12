package com.birowo.client.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * The veins of blocks are joined with the arithmetic of the core, so that is tested without a
 * game.
 */
class VeinsTest {
	@Test
	void joinsTwoBlocksThatTouch() {
		Veins.Result result = Veins.group(
				new int[] {0, 1},
				new int[] {10, 10},
				new int[] {0, 0},
				new int[] {1, 1},
				new int[] {NeighbourFaces.EAST, NeighbourFaces.WEST});

		assertEquals(1, result.veins().size());
		assertEquals(2, result.veins().get(0).size());
		assertEquals(0, result.veinOf()[0]);
		assertEquals(0, result.veinOf()[1]);
	}

	@Test
	void leavesBlocksThatDoNotTouchAlone() {
		Veins.Result result = Veins.group(
				new int[] {0, 5},
				new int[] {10, 10},
				new int[] {0, 0},
				new int[] {1, 1},
				new int[] {0, 0});

		assertEquals(2, result.veins().size());
		assertNotEquals(result.veinOf()[0], result.veinOf()[1]);
	}

	@Test
	void joinsAWholeLineOfBlocks() {
		Veins.Result result = Veins.group(
				new int[] {0, 1, 2},
				new int[] {10, 10, 10},
				new int[] {0, 0, 0},
				new int[] {1, 1, 1},
				new int[] {NeighbourFaces.EAST, NeighbourFaces.WEST | NeighbourFaces.EAST,
						NeighbourFaces.WEST});

		assertEquals(1, result.veins().size());
		assertEquals(3, result.veins().get(0).size());
	}

	@Test
	void putsTheMiddleOfAVeinBetweenItsBlocks() {
		Veins.Result result = Veins.group(
				new int[] {0, 1, 2},
				new int[] {10, 10, 10},
				new int[] {0, 0, 0},
				new int[] {1, 1, 1},
				new int[] {NeighbourFaces.EAST, NeighbourFaces.WEST | NeighbourFaces.EAST,
						NeighbourFaces.WEST});

		Veins.Vein vein = result.veins().get(0);

		assertEquals(1.5D, vein.x(), 0.0001D);
		assertEquals(10.5D, vein.y(), 0.0001D);
	}

	@Test
	void keepsBlocksOfAnotherKindApart() {
		// A covered side always means a block of the same kind, so the kind of a block that is
		// alone in its place is the kind of its vein.
		Veins.Result result = Veins.group(
				new int[] {0, 1, 5},
				new int[] {10, 10, 10},
				new int[] {0, 0, 0},
				new int[] {1, 1, 2},
				new int[] {NeighbourFaces.EAST, NeighbourFaces.WEST, 0});

		assertEquals(2, result.veins().size());
		assertEquals(1, result.veins().get(0).kind());
		assertEquals(2, result.veins().get(1).kind());
	}

	@Test
	void joinsBlocksThatTouchAcrossAChunkBorder() {
		// The last block of a chunk and the first block of the next one are neighbours.
		Veins.Result result = Veins.group(
				new int[] {15, 16},
				new int[] {10, 10},
				new int[] {0, 0},
				new int[] {1, 1},
				new int[] {NeighbourFaces.EAST, NeighbourFaces.WEST});

		assertEquals(1, result.veins().size());
		assertEquals(2, result.veins().get(0).size());
	}
}

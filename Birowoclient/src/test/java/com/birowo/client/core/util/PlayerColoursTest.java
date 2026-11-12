package com.birowo.client.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The colours of the players follow their health or their distance, and the arithmetic of that
 * is tested without a game.
 */
class PlayerColoursTest {
	private static final int RED = 0xFF0000;

	private static final int YELLOW = 0xFFFF00;

	private static final int GREEN = 0x00FF00;

	/**
	 * @param colour the colour as it is computed
	 * @return the colour without its opacity
	 */
	private static int rgb(int colour) {
		return colour & 0xFFFFFF;
	}

	@Test
	void showsAPlayerThatIsUntouchedInGreen() {
		assertEquals(GREEN, rgb(PlayerColours.byHealth(20.0F, 20.0F)));
	}

	@Test
	void showsAPlayerThatIsNearlyDeadInRed() {
		assertEquals(RED, rgb(PlayerColours.byHealth(0.0F, 20.0F)));
	}

	@Test
	void showsAPlayerThatIsHalfwayInYellow() {
		assertEquals(YELLOW, rgb(PlayerColours.byHealth(10.0F, 20.0F)));
	}

	@Test
	void keepsTheColourOfAPlayerWithoutHealth() {
		// A player that may have no health at all is not shown as being dead.
		assertEquals(0xFF5555, rgb(PlayerColours.byHealth(20.0F, 0.0F)));
	}

	@Test
	void showsANearPlayerInRedAndAFarOneInGreen() {
		assertEquals(RED, rgb(PlayerColours.byDistance(0.0D)));
		assertEquals(YELLOW, rgb(PlayerColours.byDistance(30.0D)));
		assertEquals(GREEN, rgb(PlayerColours.byDistance(60.0D)));
	}

	@Test
	void showsAPlayerThatIsOutOfReachInGreen() {
		assertEquals(GREEN, rgb(PlayerColours.byDistance(500.0D)));
	}
}

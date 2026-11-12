package com.birowo.client.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The arithmetic that places a line of the tracer on the screen has to be right in every case,
 * so it is tested without a game.
 */
class ScreenProjectionTest {
	private static final double TOLERANCE = 0.001D;

	private static final int WIDTH = 854;

	private static final int HEIGHT = 480;

	@Test
	void putsTheMiddleOfTheScreenInTheMiddleOfTheScreen() {
		ScreenProjection.ScreenPoint point = ScreenProjection.fromNormalised(0.0D, 0.0D, WIDTH, HEIGHT);

		assertEquals(WIDTH / 2.0D, point.x(), TOLERANCE);
		assertEquals(HEIGHT / 2.0D, point.y(), TOLERANCE);
	}

	@Test
	void putsTheRightOfTheScreenToTheRight() {
		ScreenProjection.ScreenPoint point = ScreenProjection.fromNormalised(1.0D, 0.0D, WIDTH, HEIGHT);

		assertEquals(WIDTH, point.x(), TOLERANCE);
	}

	@Test
	void countsTheHeightFromTheTopOfTheScreen() {
		// The game counts the height upwards, the screen counts it downwards.
		ScreenProjection.ScreenPoint top = ScreenProjection.fromNormalised(0.0D, 1.0D, WIDTH, HEIGHT);
		ScreenProjection.ScreenPoint bottom = ScreenProjection.fromNormalised(0.0D, -1.0D, WIDTH, HEIGHT);

		assertEquals(0.0D, top.y(), TOLERANCE);
		assertEquals(HEIGHT, bottom.y(), TOLERANCE);
	}

	@Test
	void measuresHowFarAPointIsInFrontOfTheCamera() {
		// The camera looks towards positive Z when the yaw is zero.
		assertEquals(10.0D, ScreenProjection.depth(0.0D, 0.0D, 10.0D, 0.0F, 0.0F), TOLERANCE);
		assertEquals(-10.0D, ScreenProjection.depth(0.0D, 0.0D, -10.0D, 0.0F, 0.0F), TOLERANCE);
		assertEquals(0.0D, ScreenProjection.depth(10.0D, 0.0D, 0.0D, 0.0F, 0.0F), TOLERANCE);
	}

	@Test
	void followsTheCameraWhenItTurns() {
		// With a yaw of ninety degrees the camera looks towards negative X.
		assertEquals(10.0D, ScreenProjection.depth(-10.0D, 0.0D, 0.0D, 90.0F, 0.0F), TOLERANCE);
		assertEquals(0.0D, ScreenProjection.depth(0.0D, 0.0D, 10.0D, 90.0F, 0.0F), TOLERANCE);
	}

	@Test
	void followsTheCameraWhenItLooksUpOrDown() {
		// A camera that looks down sees what is below it.
		assertTrue(ScreenProjection.depth(0.0D, -10.0D, 0.0D, 0.0F, 90.0F) > 0.0D,
				"the point below should be in front of a camera that looks down");
	}
}

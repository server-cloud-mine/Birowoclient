package com.birowo.client.core.util;

/**
 * The arithmetic that places a point of the world on the screen.
 *
 * <p>The game itself projects a point of the world onto the screen and hands it over as a pair
 * of numbers between minus one and one, one for each side. This class turns those numbers into
 * pixels and tells whether a point is in front of the camera at all.
 *
 * <p>The arithmetic is done here rather than in the game so that it can be tested without the
 * game: the camera of the game only says where it looks, and everything that follows from that
 * is arithmetic.
 */
public final class ScreenProjection {
	private ScreenProjection() {
	}

	/**
	 * A point of the screen, in pixels.
	 *
	 * @param x the horizontal position, counted from the left of the screen
	 * @param y the vertical position, counted from the top of the screen
	 */
	public record ScreenPoint(double x, double y) {
	}

	/**
	 * How far a point of the world is in front of the camera.
	 *
	 * @param dx           how far the point is to the right of the camera
	 * @param dy           how far the point is above the camera
	 * @param dz           how far the point is in front of the camera
	 * @param yawDegrees   the direction the camera looks at, in degrees
	 * @param pitchDegrees how far the camera looks up or down, in degrees
	 * @return the distance in front of the camera; a negative one means the point is behind it
	 */
	public static double depth(double dx, double dy, double dz, float yawDegrees, float pitchDegrees) {
		double yaw = Math.toRadians(yawDegrees);
		double pitch = Math.toRadians(pitchDegrees);

		// The direction the camera looks at, in the world.
		double forwardX = -Math.sin(yaw) * Math.cos(pitch);
		double forwardY = -Math.sin(pitch);
		double forwardZ = Math.cos(yaw) * Math.cos(pitch);

		return dx * forwardX + dy * forwardY + dz * forwardZ;
	}

	/**
	 * Turns the numbers of the game into a place on the screen.
	 *
	 * <p>The numbers run from minus one to one and are counted from the middle of the screen: the
	 * middle of the screen is the point where the crosshair is. The vertical number counts
	 * upwards, the way the game counts it, while the screen counts downwards.
	 *
	 * @param normalisedX the horizontal number, between minus one and one
	 * @param normalisedY the vertical number, between minus one and one
	 * @param screenWidth the width of the screen, in pixels
	 * @param screenHeight the height of the screen, in pixels
	 * @return where the point is on the screen
	 */
	public static ScreenPoint fromNormalised(double normalisedX, double normalisedY,
			int screenWidth, int screenHeight) {
		double screenX = (normalisedX + 1.0D) * 0.5D * screenWidth;
		double screenY = (1.0D - (normalisedY + 1.0D) * 0.5D) * screenHeight;

		return new ScreenPoint(screenX, screenY);
	}
}

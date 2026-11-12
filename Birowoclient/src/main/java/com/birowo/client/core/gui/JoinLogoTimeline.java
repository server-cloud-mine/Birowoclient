package com.birowo.client.core.gui;

/**
 * The timing of the logo that is shown when the player joins a server.
 *
 * <p>The logo fades in over the world, holds for a moment and fades away again. It is a quiet
 * thing: the player is already in the game and must be able to play while it is on screen, so it
 * never takes the game away.
 *
 * <p>This class is pure. It turns a number of seconds into a frame and knows nothing about the
 * game, so the timing can be tested without one.
 */
public final class JoinLogoTimeline {
	/**
	 * When the logo has fully appeared.
	 */
	public static final double FADE_IN_END = 0.45;

	/**
	 * When the logo starts to fade away.
	 */
	public static final double HOLD_END = 2.10;

	/**
	 * When the animation is over.
	 */
	public static final double FADE_OUT_END = 2.70;

	/**
	 * The size the logo is scaled to when it appears.
	 */
	private static final double SCALE_FROM = 0.90;

	/**
	 * One frame of the animation.
	 *
	 * @param alpha    the opacity of the logo, from {@code 0} to {@code 1}
	 * @param scale    the size of the logo, from {@code 0.9} to {@code 1}
	 * @param finished whether the animation has run its course
	 */
	public record Frame(double alpha, double scale, boolean finished) {
	}

	/**
	 * Computes the frame of the animation at a given moment.
	 *
	 * @param elapsed the seconds since the animation started
	 * @return the frame to draw
	 */
	public Frame frameAt(double elapsed) {
		double time = Math.max(0.0, elapsed);

		double alpha = Easing.smoothStep(Easing.clamp01((float) progress(time, 0.0, FADE_IN_END)));

		if (time > HOLD_END) {
			alpha *= 1.0 - Easing.clamp01((float) progress(time, HOLD_END, FADE_OUT_END));
		}

		double scale = SCALE_FROM + (1.0 - SCALE_FROM)
				* Easing.cubicOut(Easing.clamp01((float) progress(time, 0.0, FADE_IN_END)));

		return new Frame(alpha, scale, time >= FADE_OUT_END);
	}

	/**
	 * @param time  the seconds since the animation started
	 * @param start the moment the part begins
	 * @param end   the moment the part ends
	 * @return the progress of the part, from {@code 0} to {@code 1}
	 */
	private static double progress(double time, double start, double end) {
		if (end <= start) {
			return time >= end ? 1.0 : 0.0;
		}

		return (time - start) / (end - start);
	}
}

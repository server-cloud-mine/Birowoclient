package com.birowo.client.core.gui;

/**
 * The timing of the introduction animation.
 *
 * <p>The animation is the story of the logo: the blue slash catches fire, the letter appears out
 * of it, and the name is typed out letter by letter. The logo then holds for a moment before it
 * fades away.
 *
 * <p>This class is pure. It turns a number of seconds into a frame and knows nothing about the
 * game, so the animation can be tested without one.
 */
public final class IntroTimeline {
	/**
	 * When the slash starts to burn.
	 */
	public static final double SLASH_START = 0.0;

	/**
	 * When the slash has burnt all the way through.
	 */
	public static final double SLASH_END = 0.55;

	/**
	 * When the letter starts to appear.
	 */
	public static final double MARK_START = 0.28;

	/**
	 * When the letter has fully appeared.
	 */
	public static final double MARK_END = 0.80;

	/**
	 * When the first letter of the name is typed.
	 */
	public static final double TYPE_START = 0.62;

	/**
	 * When the last letter of the name has been typed.
	 */
	public static final double TYPE_END = 1.60;

	/**
	 * When the logo starts to fade away.
	 */
	public static final double HOLD_END = 2.35;

	/**
	 * When the animation is over.
	 */
	public static final double FADE_END = 2.95;

	/**
	 * When the logo starts to grow into its place.
	 */
	public static final double SCALE_START = 0.20;

	/**
	 * When the logo has reached its place.
	 */
	public static final double SCALE_END = 0.85;

	/**
	 * How many letters the name has.
	 */
	public static final int LETTERS = 12;

	/**
	 * The size the logo is scaled to when it appears.
	 */
	private static final double SCALE_FROM = 0.94;

	/**
	 * One frame of the animation.
	 *
	 * @param slashProgress how far the slash has burnt, from {@code 0} to {@code 1}
	 * @param markAlpha     how visible the letter is, from {@code 0} to {@code 1}
	 * @param groupScale    the size of the logo, from {@code 0.94} to {@code 1}
	 * @param lettersTyped  how many letters of the name have been typed in full
	 * @param letterReveal  how visible the letter that is currently being typed is
	 * @param alpha         the opacity of the whole animation, from {@code 0} to {@code 1}
	 * @param finished      whether the animation has run its course
	 */
	public record Frame(double slashProgress, double markAlpha, double groupScale, int lettersTyped,
			double letterReveal, double alpha, boolean finished) {
	}

	/**
	 * Computes the frame of the animation at a given moment.
	 *
	 * @param elapsed the seconds since the animation started
	 * @return the frame to draw
	 */
	public Frame frameAt(double elapsed) {
		double time = Math.max(0.0, elapsed);

		double slash = Easing.clamp01((float) progress(time, SLASH_START, SLASH_END));
		double mark = Easing.smoothStep(Easing.clamp01((float) progress(time, MARK_START, MARK_END)));
		double scale = SCALE_FROM
				+ (1.0 - SCALE_FROM)
						* Easing.cubicOut(Easing.clamp01((float) progress(time, SCALE_START, SCALE_END)));

		double typed = Easing.clamp01((float) progress(time, TYPE_START, TYPE_END)) * LETTERS;
		int lettersTyped = (int) Math.floor(typed);
		double letterReveal = typed - lettersTyped;

		double alpha = 1.0 - Easing.clamp01((float) progress(time, HOLD_END, FADE_END));

		return new Frame(slash, mark, scale, lettersTyped, letterReveal, alpha, time >= FADE_END);
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

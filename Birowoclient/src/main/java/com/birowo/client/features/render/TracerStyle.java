package com.birowo.client.features.render;

/**
 * The way the tracer is drawn.
 */
public enum TracerStyle {
	/**
	 * The line is drawn on the screen, from the middle of the screen to the target.
	 *
	 * <p>The line is always the same width and is never hidden by what is in front of it.
	 */
	SCREEN,

	/**
	 * The line is drawn in the world, from the eye of the player to the target.
	 *
	 * <p>The line is a part of the world, so it grows thinner as it goes further away and it can
	 * be hidden by what is in front of it.
	 */
	WORLD
}

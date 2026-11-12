package com.birowo.client.core.config;

/**
 * The way one block of the overlay is drawn.
 *
 * <p>Every part of the settings may be left empty, which means that the block follows the
 * setting of the feature as a whole. A block that has a colour of its own but no shape of its own
 * is therefore drawn in the shape that was chosen for all blocks, and in its own colour.
 *
 * @param shape        the way the block is drawn, or {@code null} to follow the feature
 * @param tracer       whether a line is drawn from the screen to the block, or {@code null} to
 *                     follow the feature
 * @param tracerColour the colour of that line, or {@code -1} to use the colour of the block
 * @param sideColour   the colour of the faces of the block, or {@code -1} to use the colour of
 *                     the block
 * @param alpha        how much of the colour is kept when the faces and the filled box of the
 *                     block are drawn, or {@code -1} to follow the feature
 * @param lineWidth    how thick the edges of the block are, in pixels, or {@code -1} to follow the
 *                     feature
 * @param tracerWidth  how thick the line to the block is, in pixels, or {@code -1} to follow the
 *                     feature
 */
public record EspSettings(EspShape shape, Boolean tracer, int tracerColour, int sideColour, int alpha,
		int lineWidth, int tracerWidth) {
	/**
	 * The settings of a block that follows the feature in every way.
	 */
	public static final EspSettings DEFAULT = new EspSettings(null, null, -1, -1, -1, -1, -1);

	/**
	 * @param shape the way the block is drawn
	 * @return the settings with that shape
	 */
	public EspSettings withShape(EspShape shape) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @param tracer whether a line is drawn to the block
	 * @return the settings with that choice
	 */
	public EspSettings withTracer(Boolean tracer) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @param tracerColour the colour of the line
	 * @return the settings with that colour
	 */
	public EspSettings withTracerColour(int tracerColour) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @param sideColour the colour of the faces
	 * @return the settings with that colour
	 */
	public EspSettings withSideColour(int sideColour) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @param alpha how much of the colour is kept by the faces and the filled box
	 * @return the settings with that alpha
	 */
	public EspSettings withAlpha(int alpha) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @param lineWidth how thick the edges are
	 * @return the settings with that width
	 */
	public EspSettings withLineWidth(int lineWidth) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @param tracerWidth how thick the line to the block is
	 * @return the settings with that width
	 */
	public EspSettings withTracerWidth(int tracerWidth) {
		return new EspSettings(shape, tracer, tracerColour, sideColour, alpha, lineWidth, tracerWidth);
	}

	/**
	 * @return whether every part of the settings is the one of the default
	 */
	public boolean isDefault() {
		return shape == null && tracer == null && tracerColour == -1 && sideColour == -1 && alpha == -1
				&& lineWidth == -1 && tracerWidth == -1;
	}
}

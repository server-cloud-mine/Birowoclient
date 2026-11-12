package com.birowo.client.core.config;

/**
 * The way a block of the overlay is drawn.
 *
 * <p>The kinds are the ones the overlay of the game uses as well: a block can be shown by its
 * edges, by its faces, by both, or as a whole filled box. A block that is shown by its faces is
 * drawn in a colour of its own, so that the faces of the block can be told apart from its edges.
 */
public enum EspShape {
	/**
	 * Only the edges of the block are drawn.
	 */
	LINES(true, false, false),

	/**
	 * Only the faces of the block are drawn.
	 */
	SIDES(false, true, false),

	/**
	 * The block is drawn as a whole, filled box.
	 */
	FILL(false, false, true),

	/**
	 * The edges of the block are drawn, and so are its faces.
	 */
	LINES_AND_SIDES(true, true, false);

	private final boolean lines;

	private final boolean sides;

	private final boolean filled;

	EspShape(boolean lines, boolean sides, boolean filled) {
		this.lines = lines;
		this.sides = sides;
		this.filled = filled;
	}

	/**
	 * @return whether the edges of the block are drawn
	 */
	public boolean lines() {
		return lines;
	}

	/**
	 * @return whether the faces of the block are drawn
	 */
	public boolean sides() {
		return sides;
	}

	/**
	 * @return whether the block is drawn as a whole, filled box
	 */
	public boolean filled() {
		return filled;
	}

	/**
	 * @return the name of the kind as it is shown in the interface
	 */
	public String displayName() {
		return switch (this) {
			case LINES -> "Lines";
			case SIDES -> "Sides";
			case FILL -> "Fill";
			case LINES_AND_SIDES -> "Lines + sides";
		};
	}
}

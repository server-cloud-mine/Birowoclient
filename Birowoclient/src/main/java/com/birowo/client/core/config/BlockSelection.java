package com.birowo.client.core.config;

/**
 * One block of a block list, with the colour and the way it is shown in.
 *
 * <p>The block is kept as its identifier, so that the core never has to know what a block is, and
 * the colour is kept as a plain red, green and blue value, so that it can be read and written by
 * a person as well.
 *
 * @param identifier the identifier of the block, such as {@code minecraft:chest}
 * @param colour     the colour of the block, as a {@code 0xRRGGBB} value
 * @param settings   the way the block is drawn; the parts that are empty follow the feature
 */
public record BlockSelection(String identifier, int colour, EspSettings settings) {
	/**
	 * A block that is shown in the way the feature says, in the colour it is given.
	 *
	 * @param identifier the identifier of the block
	 * @param colour     the colour of the block
	 */
	public BlockSelection(String identifier, int colour) {
		this(identifier, colour, EspSettings.DEFAULT);
	}

	/**
	 * @return the colour with a fully opaque alpha channel
	 */
	public int argb() {
		return 0xFF000000 | (colour & 0xFFFFFF);
	}

	/**
	 * @param colour the new colour of the block
	 * @return the block in that colour
	 */
	public BlockSelection withColour(int colour) {
		return new BlockSelection(identifier, colour, settings);
	}

	/**
	 * @param settings the new way the block is drawn
	 * @return the block drawn in that way
	 */
	public BlockSelection withSettings(EspSettings settings) {
		return new BlockSelection(identifier, colour, settings);
	}
}

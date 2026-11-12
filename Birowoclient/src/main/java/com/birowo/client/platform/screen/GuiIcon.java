package com.birowo.client.platform.screen;

import net.minecraft.client.gui.DrawContext;

import com.birowo.client.core.gui.GuiTheme;

/**
 * The glyph drawn in the header of a panel.
 *
 * <p>Every glyph is a bitmap drawn as one pixel per set bit, so the icons share a single visual
 * weight and none of them depends on a font or on an emoji.
 *
 * <p>The categories themselves are shown with an item of the game rather than with a glyph, so
 * only the glyphs of the tools of the interface are left.
 */
public enum GuiIcon {
	/**
	 * A magnifier, for the search panel.
	 */
	SEARCH(7, new int[] {
			0b0111110,
			0b1100011,
			0b1100011,
			0b1100011,
			0b0111110,
			0b0000110,
			0b0000011
	}),

	/**
	 * A chevron pointing down, shown on a panel that is expanded.
	 */
	EXPANDED(7, new int[] {
			0b0000000,
			0b0011100,
			0b0111110,
			0b1101011,
			0b0000000,
			0b0000000,
			0b0000000
	}),

	/**
	 * A chevron pointing up, shown on a panel that is collapsed.
	 */
	COLLAPSED(7, new int[] {
			0b0000000,
			0b0000000,
			0b1101011,
			0b0111110,
			0b0011100,
			0b0000000,
			0b0000000
	});

	private final int width;
	private final int[] mask;

	GuiIcon(int width, int[] mask) {
		this.width = width;
		this.mask = mask;
	}

	/**
	 * Draws this glyph.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param color   the ARGB colour to draw with
	 */
	public void draw(DrawContext context, int x, int y, int color) {
		GuiShapes.drawMask(context, mask, width, GuiTheme.ICON_SIZE, x, y, color);
	}
}

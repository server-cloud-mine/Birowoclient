package com.birowo.client.platform.screen;

import net.minecraft.client.gui.DrawContext;

import com.birowo.client.core.gui.GuiColors;

/**
 * The drawing primitives the interface is built from.
 *
 * <p>The shapes are assembled from rectangles rather than from textures so that the interface has
 * no assets of its own and stays sharp at every GUI scale.
 *
 * <p>The rounded corners are antialiased: the coverage of every pixel of a corner is worked out
 * and the pixel is filled with the colour of the shape at that coverage, so the edge of a shape
 * fades into the background instead of stepping along it. Only the pixels of the corners need the
 * extra work, because the straight edges of a shape always fall on the edge of a pixel.
 *
 * <p>Nothing here allocates while it draws, because the interface is drawn again on every frame.
 */
public final class GuiShapes {
	private static final int MAX_CORNER_RADIUS = 12;

	/**
	 * How many layers the soft shadow of a panel is made of.
	 */
	public static final int SHADOW_LAYERS = 3;

	private GuiShapes() {
	}

	/**
	 * Fills a rectangle with rounded corners.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 */
	public static void fillRounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clamped = Math.min(Math.min(radius, width / 2), Math.min(height / 2, MAX_CORNER_RADIUS));

		if (clamped <= 0) {
			context.fill(x, y, x + width, y + height, color);
			return;
		}

		// The straight parts of the shape.
		context.fill(x + clamped, y, x + width - clamped, y + height, color);
		context.fill(x, y + clamped, x + clamped, y + height - clamped, color);
		context.fill(x + width - clamped, y + clamped, x + width, y + height - clamped, color);

		fillCorners(context, x, y, width, height, clamped, color);
	}

	/**
	 * Draws a one pixel outline around a rectangle with rounded corners.
	 *
	 * <p>The outline is the band between the shape and the same shape one pixel further in, so the
	 * pixels of a corner are shaded by how much of them lie inside that band.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour of the outline
	 */
	public static void strokeRounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clamped = Math.min(Math.min(radius, width / 2), Math.min(height / 2, MAX_CORNER_RADIUS));

		if (clamped <= 0 || width < 2 || height < 2) {
			context.fill(x, y, x + width, y + height, color);
			return;
		}

		for (int column = 0; column < width; column++) {
			strokePixel(context, x, y, width, height, clamped, color, x + column, y);
			strokePixel(context, x, y, width, height, clamped, color, x + column, y + height - 1);
		}

		for (int row = 1; row < height - 1; row++) {
			strokePixel(context, x, y, width, height, clamped, color, x, y + row);
			strokePixel(context, x, y, width, height, clamped, color, x + width - 1, y + row);
		}
	}

	/**
	 * Fills a rectangle whose bottom corners are square and whose top corners are rounded.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius of the top corners, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 */
	public static void fillRoundedTop(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clamped = Math.min(Math.min(radius, width / 2), Math.min(height, MAX_CORNER_RADIUS));

		if (clamped <= 0) {
			context.fill(x, y, x + width, y + height, color);
			return;
		}

		context.fill(x, y + clamped, x + width, y + height, color);

		fillTopCorners(context, x, y, width, height, clamped, color);
	}

	/**
	 * Fills a rectangle with rounded corners, with a colour that fades from the top to the bottom.
	 *
	 * <p>The gradient is drawn one row at a time, so that it stays smooth at every GUI scale and
	 * needs no texture of its own.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param top     the colour at the top, as an ARGB colour
	 * @param bottom  the colour at the bottom, as an ARGB colour
	 */
	public static void fillRoundedGradient(DrawContext context, int x, int y, int width, int height, int radius,
			int top, int bottom) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clamped = Math.min(Math.min(radius, width / 2), Math.min(height / 2, MAX_CORNER_RADIUS));

		if (clamped <= 0) {
			context.fillGradient(x, y, x + width, y + height, top, bottom);
			return;
		}

		for (int row = 0; row < height; row++) {
			float progress = height > 1 ? row / (float) (height - 1) : 0.0F;
			int colour = GuiColors.lerp(top, bottom, progress);

			if (row < clamped) {
				fillTopCornerRow(context, x, y, width, height, clamped, colour, row);
			} else if (row >= height - clamped) {
				fillBottomCornerRow(context, x, y, width, height, clamped, colour, row);
			} else {
				context.fill(x, y + row, x + width, y + row + 1, colour);
			}
		}
	}

	/**
	 * Fills a rectangle whose bottom corners are square and whose top corners are rounded, with a
	 * colour that fades from the top to the bottom.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius of the top corners, in scaled GUI pixels
	 * @param top     the colour at the top, as an ARGB colour
	 * @param bottom  the colour at the bottom, as an ARGB colour
	 */
	public static void fillRoundedTopGradient(DrawContext context, int x, int y, int width, int height, int radius,
			int top, int bottom) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clamped = Math.min(Math.min(radius, width / 2), Math.min(height, MAX_CORNER_RADIUS));

		if (clamped <= 0) {
			context.fillGradient(x, y, x + width, y + height, top, bottom);
			return;
		}

		for (int row = 0; row < height; row++) {
			float progress = height > 1 ? row / (float) (height - 1) : 0.0F;
			int colour = GuiColors.lerp(top, bottom, progress);

			if (row < clamped) {
				fillTopCornerRow(context, x, y, width, height, clamped, colour, row);
			} else {
				context.fill(x, y + row, x + width, y + row + 1, colour);
			}
		}
	}

	/**
	 * Fills a rectangle with a colour that fades from the left to the right.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param left    the colour at the left, as an ARGB colour
	 * @param right   the colour at the right, as an ARGB colour
	 */
	public static void fillHorizontalGradient(DrawContext context, int x, int y, int width, int height, int left,
			int right) {
		if (width <= 0 || height <= 0) {
			return;
		}

		for (int column = 0; column < width; column++) {
			float progress = width > 1 ? column / (float) (width - 1) : 0.0F;
			context.fill(x + column, y, x + column + 1, y + height, GuiColors.lerp(left, right, progress));
		}
	}

	/**
	 * Fills a circle with a soft edge.
	 *
	 * <p>The circle is drawn one pixel at a time, and every pixel is filled with the colour of the
	 * circle at the share of the pixel that lies inside it, so that the rim of the circle fades
	 * into the background rather than stepping along it.
	 *
	 * @param context   the draw context of the current frame
	 * @param centerX   the horizontal centre of the circle, in scaled GUI pixels
	 * @param centerY   the vertical centre of the circle, in scaled GUI pixels
	 * @param radius    the radius of the circle, in scaled GUI pixels
	 * @param color     the ARGB colour to fill with
	 */
	public static void fillCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
		if (radius <= 0) {
			return;
		}

		for (int row = centerY - radius; row <= centerY + radius; row++) {
			for (int column = centerX - radius; column <= centerX + radius; column++) {
				float offsetX = column + 0.5F - centerX;
				float offsetY = row + 0.5F - centerY;
				float distance = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY) - radius;

				fillPixel(context, column, row, color, coverage(distance));
			}
		}
	}

	/**
	 * Draws a soft glow around a rectangle with rounded corners.
	 *
	 * <p>The glow is a few outlines drawn on top of each other, each one further out and each one
	 * fainter than the last. It is not a real blur, but it reads as one and costs a handful of
	 * rectangles.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param colour  the colour of the glow, as an ARGB colour
	 * @param layers  how many outlines the glow is made of
	 */
	public static void glowRounded(DrawContext context, int x, int y, int width, int height, int radius, int colour,
			int layers) {
		for (int layer = layers; layer >= 1; layer--) {
			float factor = layer / (float) layers;

			strokeRounded(context,
					x - layer, y - layer, width + layer * 2, height + layer * 2,
					radius + layer,
					GuiColors.withAlpha(colour, 1.0F - factor * 0.75F));
		}
	}

	/**
	 * Draws a soft shadow under a rectangle with rounded corners.
	 *
	 * <p>The shadow is a few shapes drawn on top of each other, each one further out and each one
	 * fainter than the last, so that it fades away instead of ending in a hard edge.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param colour  the colour of the shadow, as an ARGB colour
	 * @param layers  how many layers the shadow is made of
	 * @param offsetY how far the shadow sits below the shape, in scaled GUI pixels
	 */
	public static void softShadow(DrawContext context, int x, int y, int width, int height, int radius, int colour,
			int layers, int offsetY) {
		for (int layer = layers; layer >= 1; layer--) {
			float factor = 1.0F - layer / (float) (layers + 1);

			fillRounded(context, x - layer, y - layer + offsetY, width + layer * 2, height + layer * 2,
					radius + layer, GuiColors.withAlpha(colour, factor));
		}
	}

	/**
	 * Draws a soft glow around a rectangle with rounded corners.
	 *
	 * <p>The glow is a few filled shapes drawn on top of each other, each one further out and each
	 * one fainter than the last. It is drawn before the shape it belongs to, so that the shape
	 * itself stays crisp.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param colour  the colour of the glow, as an ARGB colour
	 * @param layers  how many layers the glow is made of
	 */
	public static void softGlow(DrawContext context, int x, int y, int width, int height, int radius, int colour,
			int layers) {
		softShadow(context, x, y, width, height, radius, colour, layers, 0);
	}

	/**
	 * Draws a straight line of a given thickness.
	 *
	 * <p>The line is walked one pixel at a time and every step is filled with a square of the
	 * thickness of the line, so that the line is the same width wherever it goes.
	 *
	 * @param context   the draw context of the current frame
	 * @param x1        the left end of the line, in scaled GUI pixels
	 * @param y1        the top end of the line, in scaled GUI pixels
	 * @param x2        the right end of the line, in scaled GUI pixels
	 * @param y2        the bottom end of the line, in scaled GUI pixels
	 * @param thickness how wide the line is, in scaled GUI pixels
	 * @param color     the ARGB colour to draw with
	 */
	public static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int thickness, int color) {
		int size = Math.max(1, thickness);
		int half = size / 2;

		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int stepX = x1 < x2 ? 1 : -1;
		int stepY = y1 < y2 ? 1 : -1;
		int error = dx - dy;

		int x = x1;
		int y = y1;

		while (true) {
			context.fill(x - half, y - half, x - half + size, y - half + size, color);

			if (x == x2 && y == y2) {
				return;
			}

			int doubled = error * 2;

			if (doubled > -dy) {
				error -= dy;
				x += stepX;
			}

			if (doubled < dx) {
				error += dx;
				y += stepY;
			}
		}
	}

	/**
	 * Draws a bitmap glyph.
	 *
	 * <p>Each bit of a row marks one pixel, with the least significant bit on the left, so a mask
	 * reads the same way it is drawn.
	 *
	 * @param context the draw context of the current frame
	 * @param mask    the rows of the glyph, one integer per row
	 * @param width   the width of the glyph, in pixels
	 * @param height  the height of the glyph, in pixels
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param color   the ARGB colour to draw with
	 */
	public static void drawMask(DrawContext context, int[] mask, int width, int height, int x, int y, int color) {
		for (int row = 0; row < height; row++) {
			int bits = mask[row];

			for (int column = 0; column < width; column++) {
				if ((bits & (1 << column)) == 0) {
					continue;
				}

				context.fill(x + column, y + row, x + column + 1, y + row + 1, color);
			}
		}
	}

	// Corners ----------------------------------------------------------------------------------

	/**
	 * Fills the four corners of a rounded rectangle.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 */
	private static void fillCorners(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		for (int row = 0; row < radius; row++) {
			int top = y + row;
			int bottom = y + height - 1 - row;

			for (int column = 0; column < radius; column++) {
				int left = x + column;
				int right = x + width - 1 - column;

				fillPixel(context, left, top, color, cornerCoverage(x, y, width, height, radius, left, top));
				fillPixel(context, right, top, color, cornerCoverage(x, y, width, height, radius, right, top));
				fillPixel(context, left, bottom, color, cornerCoverage(x, y, width, height, radius, left, bottom));
				fillPixel(context, right, bottom, color, cornerCoverage(x, y, width, height, radius, right, bottom));
			}
		}
	}

	/**
	 * Fills the two top corners of a rounded rectangle.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 */
	private static void fillTopCorners(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		for (int row = 0; row < radius; row++) {
			int top = y + row;

			for (int column = 0; column < radius; column++) {
				int left = x + column;
				int right = x + width - 1 - column;

				fillPixel(context, left, top, color, cornerCoverage(x, y, width, height, radius, left, top));
				fillPixel(context, right, top, color, cornerCoverage(x, y, width, height, radius, right, top));
			}
		}
	}

	/**
	 * Fills one row of the two top corners of a rounded rectangle.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 * @param row     the row of the shape, counted from the top
	 */
	private static void fillTopCornerRow(DrawContext context, int x, int y, int width, int height, int radius,
			int color, int row) {
		int top = y + row;

		for (int column = 0; column < radius; column++) {
			int left = x + column;
			int right = x + width - 1 - column;

			fillPixel(context, left, top, color, cornerCoverage(x, y, width, height, radius, left, top));
			fillPixel(context, right, top, color, cornerCoverage(x, y, width, height, radius, right, top));
		}
	}

	/**
	 * Fills one row of the two bottom corners of a rounded rectangle.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge, in scaled GUI pixels
	 * @param y       the top edge, in scaled GUI pixels
	 * @param width   the width, in scaled GUI pixels
	 * @param height  the height, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 * @param row     the row of the shape, counted from the top
	 */
	private static void fillBottomCornerRow(DrawContext context, int x, int y, int width, int height, int radius,
			int color, int row) {
		int bottom = y + row;

		for (int column = 0; column < radius; column++) {
			int left = x + column;
			int right = x + width - 1 - column;

			fillPixel(context, left, bottom, color, cornerCoverage(x, y, width, height, radius, left, bottom));
			fillPixel(context, right, bottom, color, cornerCoverage(x, y, width, height, radius, right, bottom));
		}
	}

	/**
	 * Fills one pixel of the outline of a rounded rectangle.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the shape, in scaled GUI pixels
	 * @param y       the top edge of the shape, in scaled GUI pixels
	 * @param width   the width of the shape, in scaled GUI pixels
	 * @param height  the height of the shape, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param color   the ARGB colour of the outline
	 * @param column  the column of the pixel, in scaled GUI pixels
	 * @param row     the row of the pixel, in scaled GUI pixels
	 */
	private static void strokePixel(DrawContext context, int x, int y, int width, int height, int radius, int color,
			int column, int row) {
		float outer = cornerCoverage(x, y, width, height, radius, column, row);
		float inner = cornerCoverage(x + 1, y + 1, width - 2, height - 2, radius - 1, column, row);

		fillPixel(context, column, row, color, outer - inner);
	}

	// Geometry ---------------------------------------------------------------------------------

	/**
	 * The share of a pixel that lies inside a rounded rectangle.
	 *
	 * <p>The distance from the centre of the pixel to the outline of the shape is worked out and
	 * turned into a share: a pixel whose centre lies half a pixel or more inside the shape is
	 * filled completely, a pixel whose centre lies on the outline is filled by half and a pixel
	 * outside of it is left alone.
	 *
	 * @param x       the left edge of the shape, in scaled GUI pixels
	 * @param y       the top edge of the shape, in scaled GUI pixels
	 * @param width   the width of the shape, in scaled GUI pixels
	 * @param height  the height of the shape, in scaled GUI pixels
	 * @param radius  the corner radius, in scaled GUI pixels
	 * @param column  the column of the pixel, in scaled GUI pixels
	 * @param row     the row of the pixel, in scaled GUI pixels
	 * @return the share of the pixel that lies inside the shape, between zero and one
	 */
	private static float cornerCoverage(int x, int y, int width, int height, int radius, int column, int row) {
		if (radius <= 0) {
			float insideX = column + 0.5F >= x && column + 0.5F <= x + width ? 1.0F : 0.0F;
			float insideY = row + 0.5F >= y && row + 0.5F <= y + height ? 1.0F : 0.0F;
			return Math.min(insideX, insideY);
		}

		float centerX = x + width / 2.0F;
		float centerY = y + height / 2.0F;
		float offsetX = Math.abs(column + 0.5F - centerX) - (width / 2.0F - radius);
		float offsetY = Math.abs(row + 0.5F - centerY) - (height / 2.0F - radius);
		float outside = Math.max(offsetX, offsetY);
		float distanceX = Math.max(offsetX, 0.0F);
		float distanceY = Math.max(offsetY, 0.0F);

		float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY)
				+ Math.min(outside, 0.0F) - radius;

		return coverage(distance);
	}

	/**
	 * Turns a distance from an outline into the share of a pixel that lies inside the shape.
	 *
	 * @param distance the distance from the centre of a pixel to the outline, negative inside
	 * @return the share of the pixel that lies inside the shape, between zero and one
	 */
	private static float coverage(float distance) {
		float share = 0.5F - distance;

		if (share <= 0.0F) {
			return 0.0F;
		}

		if (share >= 1.0F) {
			return 1.0F;
		}

		return share;
	}

	/**
	 * Fills a single pixel with a colour at the given share.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the pixel, in scaled GUI pixels
	 * @param y       the top edge of the pixel, in scaled GUI pixels
	 * @param color   the ARGB colour to fill with
	 * @param share   the share of the pixel that is covered, between zero and one
	 */
	private static void fillPixel(DrawContext context, int x, int y, int color, float share) {
		if (share <= 0.0F) {
			return;
		}

		context.fill(x, y, x + 1, y + 1, share >= 1.0F ? color : GuiColors.withAlpha(color, share));
	}
}

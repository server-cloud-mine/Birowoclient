package com.birowo.client.platform.screen;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Draws the logo of the client.
 *
 * <p>The logo is made of three layers: the blue slash, the letter and the name. They are drawn
 * apart, because the opening animation reveals them one after the other, and the slash is burnt
 * into the screen rather than drawn whole.
 *
 * <p>The layers are drawn in the space of the logo, which is a rectangle of a fixed size. A
 * caller places that rectangle on the screen and says how large it should be; everything inside it
 * is scaled together, so the logo keeps its proportions.
 */
public final class LogoPainter {
	/**
	 * The letter of the logo.
	 */
	private static final Identifier MARK_TEXTURE = Identifier.of("birowo", "textures/gui/logo_mark.png");

	/**
	 * The blue slash of the logo.
	 */
	private static final Identifier SLASH_TEXTURE = Identifier.of("birowo", "textures/gui/logo_slash.png");

	/**
	 * The name of the client.
	 */
	private static final Identifier WORD_TEXTURE = Identifier.of("birowo", "textures/gui/logo_word.png");

	/**
	 * Where the letter sits in the space of the logo.
	 */
	private static final int MARK_X = 0;
	private static final int MARK_Y = 0;
	private static final int MARK_WIDTH = 360;
	private static final int MARK_HEIGHT = 207;

	/**
	 * Where the slash sits in the space of the logo.
	 */
	private static final int SLASH_X = 193;
	private static final int SLASH_Y = 36;
	private static final int SLASH_WIDTH = 279;
	private static final int SLASH_HEIGHT = 169;

	/**
	 * Where the name sits in the space of the logo.
	 */
	private static final int WORD_X = 554;
	private static final int WORD_Y = 36;
	private static final int WORD_WIDTH = 1036;
	private static final int WORD_HEIGHT = 119;

	/**
	 * The width of the whole logo, in pixels.
	 */
	public static final int LOGO_WIDTH = 1590;

	/**
	 * The height of the whole logo, in pixels.
	 */
	public static final int LOGO_HEIGHT = 207;

	/**
	 * The texel range of every letter of the name, one after the other. The name has twelve
	 * letters, and the space between the two words is not one of them.
	 */
	private static final int[] LETTERS = {
			0, 94,
			110, 138,
			153, 215,
			221, 305,
			311, 433,
			439, 524,
			584, 674,
			690, 728,
			739, 768,
			782, 868,
			884, 968,
			975, 1036
	};

	/**
	 * How many letters the name has.
	 */
	public static final int LETTER_COUNT = LETTERS.length / 2;

	/**
	 * The width of the glowing edge of the burning slash, in pixels of the logo.
	 */
	private static final int SPARK_WIDTH = 16;

	private LogoPainter() {
	}

	/**
	 * Draws the whole logo.
	 *
	 * @param context the draw context of the current frame
	 * @param originX the left edge of the logo, in scaled GUI pixels
	 * @param originY the top edge of the logo, in scaled GUI pixels
	 * @param scale   the size of the logo
	 * @param alpha   the opacity of the logo, from {@code 0} to {@code 1}
	 */
	public static void draw(DrawContext context, int originX, int originY, float scale, float alpha) {
		drawMark(context, originX, originY, scale, alpha);
		drawSlash(context, originX, originY, scale, 1.0F, alpha);
		drawWord(context, originX, originY, scale, LETTER_COUNT, 0.0F, alpha);
	}

	/**
	 * Draws the letter of the logo.
	 *
	 * @param context the draw context of the current frame
	 * @param originX the left edge of the logo, in scaled GUI pixels
	 * @param originY the top edge of the logo, in scaled GUI pixels
	 * @param scale   the size of the logo
	 * @param alpha   the opacity of the letter, from {@code 0} to {@code 1}
	 */
	public static void drawMark(DrawContext context, int originX, int originY, float scale, float alpha) {
		if (alpha <= 0.0F) {
			return;
		}

		int x = originX + Math.round(MARK_X * scale);
		int y = originY + Math.round(MARK_Y * scale);
		int markWidth = Math.round(MARK_WIDTH * scale);
		int markHeight = Math.round(MARK_HEIGHT * scale);

		context.drawTexture(RenderPipelines.GUI_TEXTURED, MARK_TEXTURE,
				x, y, 0.0F, 0.0F,
				markWidth, markHeight,
				MARK_WIDTH, MARK_HEIGHT,
				MARK_WIDTH, MARK_HEIGHT,
				tint(0xFFFFFF, alpha));
	}

	/**
	 * Draws the slash as it burns from one end to the other.
	 *
	 * @param context  the draw context of the current frame
	 * @param originX  the left edge of the logo, in scaled GUI pixels
	 * @param originY  the top edge of the logo, in scaled GUI pixels
	 * @param scale    the size of the logo
	 * @param progress how far the slash has burnt, from {@code 0} to {@code 1}
	 * @param alpha    the opacity of the animation, from {@code 0} to {@code 1}
	 */
	public static void drawSlash(DrawContext context, int originX, int originY, float scale, double progress,
			float alpha) {
		if (progress <= 0.0) {
			return;
		}

		int x = originX + Math.round(SLASH_X * scale);
		int y = originY + Math.round(SLASH_Y * scale);
		int slashWidth = Math.round(SLASH_WIDTH * scale);
		int slashHeight = Math.round(SLASH_HEIGHT * scale);
		int edge = x + Math.round(slashWidth * (float) progress);

		context.enableScissor(x, y, Math.max(x + 1, edge), y + slashHeight);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, SLASH_TEXTURE,
				x, y, 0.0F, 0.0F,
				slashWidth, slashHeight,
				SLASH_WIDTH, SLASH_HEIGHT,
				SLASH_WIDTH, SLASH_HEIGHT,
				tint(0xFFFFFF, (float) Math.min(1.0, progress * 2.0) * alpha));
		context.disableScissor();

		// The edge of the burn glows for a moment before it dies down.
		float remaining = 1.0F - (float) progress;
		float spark = remaining * remaining * alpha;

		if (spark <= 0.0F) {
			return;
		}

		int sparkWidth = Math.max(4, Math.round(SPARK_WIDTH * scale));

		context.enableScissor(Math.max(x, edge - sparkWidth), y, Math.min(x + slashWidth, edge + 2),
				y + slashHeight);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, SLASH_TEXTURE,
				x, y, 0.0F, 0.0F,
				slashWidth, slashHeight,
				SLASH_WIDTH, SLASH_HEIGHT,
				SLASH_WIDTH, SLASH_HEIGHT,
				tint(0xFFFFFF, spark));
		context.disableScissor();
	}

	/**
	 * Draws the name of the client, letter by letter.
	 *
	 * @param context      the draw context of the current frame
	 * @param originX      the left edge of the logo, in scaled GUI pixels
	 * @param originY      the top edge of the logo, in scaled GUI pixels
	 * @param scale        the size of the logo
	 * @param lettersTyped how many letters have been typed in full
	 * @param letterReveal how visible the letter that is being typed is, from {@code 0} to {@code 1}
	 * @param alpha        the opacity of the animation, from {@code 0} to {@code 1}
	 */
	public static void drawWord(DrawContext context, int originX, int originY, float scale, int lettersTyped,
			float letterReveal, float alpha) {
		for (int letter = 0; letter < LETTER_COUNT; letter++) {
			float letterAlpha = letter < lettersTyped ? 1.0F
					: letter == lettersTyped ? letterReveal : 0.0F;

			if (letterAlpha <= 0.0F) {
				continue;
			}

			int from = LETTERS[letter * 2];
			int to = LETTERS[letter * 2 + 1];
			int regionWidth = to - from;

			int x = originX + Math.round((WORD_X + from) * scale);
			int y = originY + Math.round(WORD_Y * scale);
			int letterWidth = Math.round(regionWidth * scale);
			int wordHeight = Math.round(WORD_HEIGHT * scale);

			context.drawTexture(RenderPipelines.GUI_TEXTURED, WORD_TEXTURE,
					x, y, (float) from, 0.0F,
					letterWidth, wordHeight,
					regionWidth, WORD_HEIGHT,
					WORD_WIDTH, WORD_HEIGHT,
					tint(0xFFFFFF, letterAlpha * alpha));
		}
	}

	/**
	 * @param colour the colour, as {@code 0xRRGGBB}
	 * @param alpha  the opacity, from {@code 0} to {@code 1}
	 * @return the colour with the given opacity
	 */
	private static int tint(int colour, float alpha) {
		int opacity = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);

		return (opacity << 24) | (colour & 0xFFFFFF);
	}
}

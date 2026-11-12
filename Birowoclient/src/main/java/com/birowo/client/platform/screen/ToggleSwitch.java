package com.birowo.client.platform.screen;

import net.minecraft.client.gui.DrawContext;

import com.birowo.client.core.gui.Easing;
import com.birowo.client.core.gui.GuiColors;
import com.birowo.client.core.gui.GuiTheme;

/**
 * The switch that turns a feature on and off.
 *
 * <p>The switch is a pill with a knob that slides across it. The knob, the colour of the track
 * and the glow around the switch all follow the same animated value, so that the switch looks
 * like one thing that moves rather than like three parts that happen to change at once.
 *
 * <p>The knob is a real circle rather than a rectangle with rounded corners, so that it stays
 * round at every size.
 */
public final class ToggleSwitch {
	/**
	 * The width of the switch, in scaled GUI pixels.
	 */
	public static final int WIDTH = 26;

	/**
	 * The height of the switch, in scaled GUI pixels.
	 */
	public static final int HEIGHT = 14;

	/**
	 * How far the knob rests from the end of the track, in scaled GUI pixels.
	 */
	private static final int INSET = 2;

	/**
	 * How much of the knob stands out of the track, in scaled GUI pixels.
	 */
	private static final int OVERHANG = 2;

	/**
	 * How many layers the glow around the switch is made of.
	 */
	private static final int GLOW_LAYERS = 2;

	private ToggleSwitch() {
	}

	/**
	 * Draws the switch of a feature.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the switch, in scaled GUI pixels
	 * @param y       the top edge of the switch, in scaled GUI pixels
	 * @param amount  how far the switch is turned on, between zero and one
	 * @param width   the width of the switch, in scaled GUI pixels
	 * @param height  the height of the switch, in scaled GUI pixels
	 * @param alpha   the opacity of the switch, between zero and one
	 */
	public static void draw(DrawContext context, int x, int y, float amount, int width, int height, float alpha) {
		float progress = Easing.clamp01(amount);

		// The glow comes up with the knob, so that the switch lights up as it is turned on.
		if (progress > 0.02F) {
			GuiShapes.softGlow(context, x, y, width, height, height / 2,
					GuiColors.withAlpha(GuiTheme.ACCENT, progress * 0.5F * alpha), GLOW_LAYERS);
		}

		int track = GuiColors.lerp(GuiTheme.TOGGLE_OFF_TRACK, GuiTheme.TOGGLE_ON_TRACK, progress);
		GuiShapes.fillRounded(context, x, y, width, height, height / 2, GuiColors.withAlpha(track, alpha));

		int knobSize = Math.max(height - OVERHANG * 2, 2);
		int travel = Math.max(width - knobSize - INSET * 2, 0);

		// The knob eases into its place at both ends, so that it never starts or stops abruptly.
		int knobX = x + INSET + Math.round(travel * Easing.smoothStep(progress));
		int knobY = y + (height - knobSize) / 2;

		int knob = GuiColors.lerp(GuiTheme.TOGGLE_OFF, GuiTheme.ACCENT_BRIGHT, progress);

		GuiShapes.fillCircle(context, knobX + knobSize / 2, knobY + knobSize / 2, knobSize / 2,
				GuiColors.withAlpha(knob, alpha));
	}

	/**
	 * Draws the switch of a feature at the size the theme asks for.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the switch, in scaled GUI pixels
	 * @param y       the top edge of the switch, in scaled GUI pixels
	 * @param amount  how far the switch is turned on, between zero and one
	 * @param alpha   the opacity of the switch, between zero and one
	 */
	public static void draw(DrawContext context, int x, int y, float amount, float alpha) {
		draw(context, x, y, amount, GuiTheme.TOGGLE_WIDTH, GuiTheme.TOGGLE_HEIGHT, alpha);
	}
}

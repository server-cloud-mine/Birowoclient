package com.birowo.client.platform.screen;

import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import com.birowo.client.core.config.BlockListSetting;
import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.gui.GuiColors;
import com.birowo.client.core.gui.GuiTheme;
import com.birowo.client.core.feature.Feature;

/**
 * The settings of a feature, drawn over the interface.
 *
 * <p>The panel is part of the interface rather than a screen of its own, so that opening it never
 * takes the interface down and the position of the panels is not disturbed.
 *
 * <p>The panel fades in and slides into place rather than appearing at once, so that it is clear
 * where it came from.
 */
public final class SettingsPanel {
	/**
	 * The width of the panel, in scaled GUI pixels.
	 */
	public static final int WIDTH = 240;

	/**
	 * The height of the header of the panel, in scaled GUI pixels.
	 */
	public static final int HEADER_HEIGHT = 26;

	/**
	 * The height of a setting row, in scaled GUI pixels.
	 */
	public static final int ROW_HEIGHT = 20;

	/**
	 * The height reserved for the hint at the bottom of the panel, in scaled GUI pixels.
	 */
	public static final int FOOTER_HEIGHT = 26;

	/**
	 * The padding inside the panel, in scaled GUI pixels.
	 */
	public static final int PADDING = 8;

	/**
	 * The width of the switch of a setting, in scaled GUI pixels.
	 */
	private static final int SWITCH_WIDTH = 24;

	/**
	 * The height of the switch of a setting, in scaled GUI pixels.
	 */
	private static final int SWITCH_HEIGHT = 12;

	/**
	 * How far the panel slides into place, in scaled GUI pixels.
	 */
	private static final int SLIDE = 10;

	private static final String HINT = "Click to change, right click to reverse";

	private static final String BLOCK_HINT = "Click a list of blocks to choose the blocks";

	private SettingsPanel() {
	}

	/**
	 * @param rows       the number of settings the panel shows
	 * @param fontHeight the height of the font of the client
	 * @return the height of the panel, in scaled GUI pixels
	 */
	public static int height(int rows, int fontHeight) {
		return HEADER_HEIGHT + PADDING + fontHeight + PADDING + rows * ROW_HEIGHT + FOOTER_HEIGHT;
	}

	/**
	 * @param y          the top edge of the panel, in scaled GUI pixels
	 * @param fontHeight the height of the font of the client
	 * @return the top edge of the first setting row, in scaled GUI pixels
	 */
	public static int listTop(int y, int fontHeight) {
		return y + HEADER_HEIGHT + PADDING + fontHeight + PADDING;
	}

	/**
	 * @param alpha the opacity of the panel, from {@code 0} to {@code 1}
	 * @return how far the panel is still to slide into place, in scaled GUI pixels
	 */
	public static int slideOffset(float alpha) {
		return Math.round((1.0F - alpha) * SLIDE);
	}

	/**
	 * Draws the panel.
	 *
	 * @param context      the draw context of the current frame
	 * @param textRenderer the font of the client
	 * @param feature      the feature whose settings are shown
	 * @param settings     the settings of that feature
	 * @param x            the left edge of the panel, in scaled GUI pixels
	 * @param y            the top edge of the panel, in scaled GUI pixels
	 * @param mouseX       the horizontal position of the cursor, in scaled GUI pixels
	 * @param mouseY       the vertical position of the cursor, in scaled GUI pixels
	 * @param alpha        the opacity of the panel, from {@code 0} to {@code 1}
	 */
	public static void draw(DrawContext context, TextRenderer textRenderer, Feature feature,
			List<ConfigValue<?>> settings, int x, int y, double mouseX, double mouseY, float alpha) {
		int panelHeight = height(settings.size(), textRenderer.fontHeight);

		// The panel slides up into place as it fades in.
		int offset = slideOffset(alpha);
		int top = y - offset;

		GuiShapes.softShadow(context, x, top, WIDTH, panelHeight, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_SHADOW, alpha), GuiShapes.SHADOW_LAYERS, 2);
		GuiShapes.fillRounded(context, x, top, WIDTH, panelHeight, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_BACKGROUND, alpha));
		GuiShapes.fillRoundedTopGradient(context, x, top, WIDTH, HEADER_HEIGHT, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_HEADER_TOP, alpha),
				GuiColors.withAlpha(GuiTheme.PANEL_HEADER_BOTTOM, alpha));
		GuiShapes.strokeRounded(context, x, top, WIDTH, panelHeight, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_BORDER, alpha));

		context.drawTextWithShadow(textRenderer, Text.literal(feature.displayName()), x + PADDING,
				top + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, GuiColors.withAlpha(GuiTheme.PRIMARY_TEXT, alpha));

		int descriptionY = top + HEADER_HEIGHT + PADDING;
		String description = textRenderer.trimToWidth(feature.description(), WIDTH - PADDING * 2);
		context.drawTextWithShadow(textRenderer, Text.literal(description), x + PADDING, descriptionY,
				GuiColors.withAlpha(GuiTheme.SECONDARY_TEXT, alpha));

		int listTop = listTop(top, textRenderer.fontHeight);

		for (int index = 0; index < settings.size(); index++) {
			ConfigValue<?> setting = settings.get(index);
			int rowTop = listTop + index * ROW_HEIGHT;

			if (mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT && mouseX >= x && mouseX < x + WIDTH) {
				context.fill(x + 1, rowTop, x + WIDTH - 1, rowTop + ROW_HEIGHT,
						GuiColors.withAlpha(GuiTheme.ROW_HOVER, alpha));
			}

			int textY = rowTop + (ROW_HEIGHT - textRenderer.fontHeight) / 2;
			String value = setting.display();

			context.drawTextWithShadow(textRenderer, Text.literal(setting.key()), x + PADDING, textY,
					GuiColors.withAlpha(GuiTheme.SECONDARY_TEXT, alpha));

			// A setting that is either on or off is shown as a switch, because a word cannot say
			// as clearly at a glance as a switch can.
			if (setting.value() instanceof Boolean flag) {
				int switchWidth = SWITCH_WIDTH;
				int switchHeight = SWITCH_HEIGHT;
				int switchX = x + WIDTH - PADDING - switchWidth;
				int switchY = rowTop + (ROW_HEIGHT - switchHeight) / 2;

				ToggleSwitch.draw(context, switchX, switchY, flag ? 1.0F : 0.0F, switchWidth, switchHeight, alpha);
				continue;
			}

			context.drawTextWithShadow(textRenderer, Text.literal(value),
					x + WIDTH - PADDING - textRenderer.getWidth(value), textY,
					GuiColors.withAlpha(GuiTheme.ACCENT, alpha));
		}

		int footerY = top + panelHeight - FOOTER_HEIGHT + 6;
		String hint = HINT;

		for (ConfigValue<?> setting : settings) {
			if (setting instanceof BlockListSetting) {
				hint = BLOCK_HINT;
				break;
			}
		}

		context.drawTextWithShadow(textRenderer, Text.literal(hint), x + PADDING, footerY,
				GuiColors.withAlpha(GuiTheme.PLACEHOLDER, alpha));
	}
}

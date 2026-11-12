package com.birowo.client.platform.screen;

import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import com.birowo.client.core.gui.GuiHost;
import com.birowo.client.core.gui.GuiColors;
import com.birowo.client.core.gui.GuiTheme;
import com.birowo.client.core.gui.FeatureVisual;
import com.birowo.client.core.feature.Feature;

/**
 * Draws the panels of the interface.
 *
 * <p>The renderer holds no state of its own: everything it needs is passed in, so that a frame is
 * a pure function of the state of the client.
 */
public final class PanelRenderer {
	private static final long CARET_BLINK_NANOS = 530_000_000L;
	private static final String NO_FEATURES = "No features";

	private final DrawContext context;
	private final TextRenderer textRenderer;
	private final GuiHost host;
	private final double mouseX;
	private final double mouseY;

	/**
	 * @param context      the draw context of the current frame
	 * @param textRenderer the font of the client
	 * @param host         the source of the features and of their appearance
	 * @param mouseX       the horizontal position of the cursor, in scaled GUI pixels
	 * @param mouseY       the vertical position of the cursor, in scaled GUI pixels
	 */
	public PanelRenderer(DrawContext context, TextRenderer textRenderer, GuiHost host, double mouseX, double mouseY) {
		this.context = context;
		this.textRenderer = textRenderer;
		this.host = host;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
	}

	/**
	 * Draws the frame, the header and the body of a category panel.
	 *
	 * @param box the panel to draw
	 */
	public void drawCategoryPanel(PanelBox box) {
		float alpha = box.open();
		int headerHeight = box.sw(GuiTheme.PANEL_HEADER_HEIGHT);

		drawFrame(box, alpha, headerHeight);

		int iconSize = box.sw(GuiTheme.CATEGORY_ICON_SIZE);
		int iconY = box.y() + (headerHeight - iconSize) / 2;
		int titleColor = headerColor(box, headerHeight);
		int padding = box.sw(GuiTheme.PANEL_PADDING);

		// The picture of the category is the item of the game itself, so it is drawn by the game
		// and it grows with the panel.
		CategoryIcons.draw(context, box.category(), box.x() + padding, iconY, box.scale(), alpha);
		context.drawTextWithShadow(textRenderer, Text.literal(box.category().displayName()),
				box.x() + padding + iconSize + box.sw(GuiTheme.ICON_GAP),
				box.y() + (headerHeight - textRenderer.fontHeight) / 2, titleColor);

		GuiIcon chevron = box.state().collapseAmount() > 0.5F ? GuiIcon.COLLAPSED : GuiIcon.EXPANDED;
		int chevronY = box.y() + (headerHeight - box.sw(GuiTheme.ICON_SIZE)) / 2;
		chevron.draw(context, box.x() + box.width() - box.sw(GuiTheme.PANEL_PADDING + GuiTheme.ICON_SIZE), chevronY,
				titleColor);

		drawSeparator(box, headerHeight, alpha);

		if (box.bodyHeight() <= 0) {
			return;
		}

		int bodyTop = box.y() + headerHeight + 1;
		int bodyBottom = bodyTop + box.sw(box.bodyHeight());
		int rowHeight = box.sw(GuiTheme.ROW_HEIGHT);
		int scroll = Math.round(box.state().scroll() * box.scale());

		context.enableScissor(box.x() + 1, bodyTop, box.x() + box.width() - 1, bodyBottom);

		List<Feature> features = box.features();

		// A row that is scrolled out of sight is not under the cursor any more, so its highlight
		// has to be taken away.
		for (Feature feature : features) {
			host.visualFor(feature).setHovered(false);
		}

		for (int index = 0; index < features.size(); index++) {
			int rowTop = bodyTop + index * rowHeight - scroll;

			if (rowTop + rowHeight <= bodyTop || rowTop >= bodyBottom) {
				continue;
			}

			drawFeatureRow(box, features.get(index), rowTop, rowHeight, alpha);
		}

		context.disableScissor();

		if (features.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(NO_FEATURES),
					box.x() + (box.width() - textRenderer.getWidth(NO_FEATURES)) / 2,
					bodyTop + box.sw(GuiTheme.PANEL_PADDING), GuiColors.withAlpha(GuiTheme.PLACEHOLDER, alpha));
		}
	}

	/**
	 * Draws the frame, the header, the search field and the results of the search panel.
	 *
	 * @param box          the panel to draw
	 * @param query        the text that has been typed into the search field
	 * @param cursor       the position of the caret within the text
	 * @param focused      whether the search field has focus
	 * @param results      the features that match the query
	 * @param resultsAlpha the animated opacity of the result list
	 */
	public void drawSearchPanel(PanelBox box, String query, int cursor, boolean focused, List<Feature> results,
			float resultsAlpha) {
		float alpha = box.open();
		int headerHeight = box.sw(GuiTheme.PANEL_HEADER_HEIGHT);

		drawFrame(box, alpha, headerHeight);

		int iconSize = box.sw(GuiTheme.ICON_SIZE);
		int titleColor = headerColor(box, headerHeight);

		GuiIcon.SEARCH.draw(context, box.x() + box.sw(GuiTheme.PANEL_PADDING),
				box.y() + (headerHeight - iconSize) / 2, titleColor);
		context.drawTextWithShadow(textRenderer, Text.literal("Search"),
				box.x() + box.sw(GuiTheme.PANEL_PADDING) + iconSize + box.sw(GuiTheme.ICON_GAP),
				box.y() + (headerHeight - textRenderer.fontHeight) / 2, titleColor);

		drawSeparator(box, headerHeight, alpha);

		if (box.bodyHeight() <= 0) {
			return;
		}

		int bodyTop = box.y() + headerHeight + 1;
		int bodyBottom = bodyTop + box.sw(box.bodyHeight());
		int padding = box.sw(GuiTheme.PANEL_PADDING);
		int fieldTop = bodyTop + padding;
		int fieldHeight = box.sw(GuiTheme.SEARCH_FIELD_HEIGHT);
		int fieldWidth = box.width() - padding * 2;

		context.enableScissor(box.x() + 1, bodyTop, box.x() + box.width() - 1, bodyBottom);

		drawSearchField(box.x() + padding, fieldTop, fieldWidth, fieldHeight, query, cursor, focused, alpha);

		int listTop = fieldTop + fieldHeight + padding;
		int rowHeight = box.sw(GuiTheme.SEARCH_ROW_HEIGHT);

		for (int index = 0; index < results.size(); index++) {
			int rowTop = listTop + index * rowHeight;

			if (rowTop + rowHeight > bodyBottom) {
				break;
			}

			Feature feature = results.get(index);
			boolean hovered = box.contains(mouseX, mouseY) && mouseY >= rowTop && mouseY < rowTop + rowHeight;

			if (hovered) {
				GuiShapes.fillHorizontalGradient(context, box.x() + 1, rowTop, box.width() - 2, rowHeight,
						GuiColors.withAlpha(GuiTheme.ROW_HOVER, resultsAlpha * alpha),
						GuiColors.withAlpha(GuiTheme.ROW_HOVER, 0.0F));
			}

			int textColor = GuiColors.lerp(GuiTheme.SECONDARY_TEXT, GuiTheme.ACCENT,
					host.visualFor(feature).toggleAmount());
			textColor = GuiColors.withAlpha(textColor, resultsAlpha * alpha);

			int categoryColor = GuiColors.withAlpha(GuiTheme.PLACEHOLDER, resultsAlpha * alpha);
			int textY = rowTop + (rowHeight - textRenderer.fontHeight) / 2;

			String name = feature.displayName();
			String category = feature.category().displayName();
			int categoryWidth = textRenderer.getWidth(category);
			int available = Math.max(fieldWidth - categoryWidth - 8, 10);

			context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(name, available)),
					box.x() + padding, textY, textColor);
			context.drawTextWithShadow(textRenderer, Text.literal(category),
					box.x() + box.width() - padding - categoryWidth, textY, categoryColor);
		}

		context.disableScissor();
	}

	private void drawFrame(PanelBox box, float alpha, int headerHeight) {
		boolean hovered = box.contains(mouseX, mouseY);

		GuiShapes.softShadow(context, box.x(), box.y(), box.width(), box.height(), GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_SHADOW, alpha), GuiShapes.SHADOW_LAYERS, 2);
		GuiShapes.fillRounded(context, box.x(), box.y(), box.width(), box.height(), GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_BACKGROUND, alpha));
		GuiShapes.fillRoundedTopGradient(context, box.x(), box.y(), box.width(), headerHeight, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.PANEL_HEADER_TOP, alpha),
				GuiColors.withAlpha(GuiTheme.PANEL_HEADER_BOTTOM, alpha));
		GuiShapes.strokeRounded(context, box.x(), box.y(), box.width(), box.height(), GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(hovered ? GuiTheme.PANEL_BORDER_ACTIVE : GuiTheme.PANEL_BORDER, alpha));

		if (hovered) {
			GuiShapes.glowRounded(context, box.x(), box.y(), box.width(), box.height(), GuiTheme.CORNER_RADIUS,
					GuiColors.withAlpha(GuiTheme.ACCENT, alpha * 0.5F), 2);
		}

		// The accent bar at the left of the header: the one part of the panel that is always the
		// same colour, so that the eye knows where it is looking.
		int barHeight = headerHeight - 8;
		GuiShapes.fillRounded(context, box.x() + 2, box.y() + 4, GuiTheme.ACCENT_BAR_WIDTH, barHeight, 1,
				GuiColors.withAlpha(GuiTheme.ACCENT, alpha));
	}

	private void drawSeparator(PanelBox box, int headerHeight, float alpha) {
		context.fill(box.x() + 1, box.y() + headerHeight, box.x() + box.width() - 1, box.y() + headerHeight + 1,
				GuiColors.withAlpha(GuiTheme.SEPARATOR, alpha));
	}

	private int headerColor(PanelBox box, int headerHeight) {
		boolean hovered = box.contains(mouseX, mouseY) && mouseY < box.y() + headerHeight;
		return GuiColors.withAlpha(hovered ? GuiTheme.PRIMARY_TEXT : GuiTheme.SECONDARY_TEXT, box.open());
	}

	private void drawFeatureRow(PanelBox box, Feature feature, int rowTop, int rowHeight, float alpha) {
		FeatureVisual visual = host.visualFor(feature);
		boolean hovered = box.contains(mouseX, mouseY) && mouseY >= rowTop && mouseY < rowTop + rowHeight;

		visual.setHovered(hovered);

		if (hovered && visual.hoverAmount() > 0.01F) {
			// The highlight fades out towards the right, so that the row does not end in a hard
			// line.
			GuiShapes.fillHorizontalGradient(context, box.x() + 1, rowTop, box.width() - 2, rowHeight,
					GuiColors.withAlpha(GuiTheme.ROW_HOVER, visual.hoverAmount() * alpha),
					GuiColors.withAlpha(GuiTheme.ROW_HOVER, 0.0F));
		}

		// The name takes the colour of the switch over the same animation, so that the row does
		// not change colour a frame before the switch does.
		int textColor = GuiColors.lerp(GuiTheme.SECONDARY_TEXT, GuiTheme.ACCENT, visual.toggleAmount());
		textColor = GuiColors.withAlpha(textColor, alpha);

		int toggleWidth = box.sw(GuiTheme.TOGGLE_WIDTH);
		int toggleHeight = box.sw(GuiTheme.TOGGLE_HEIGHT);
		int toggleX = box.x() + box.width() - box.sw(GuiTheme.PANEL_PADDING) - toggleWidth;
		int toggleY = rowTop + (rowHeight - toggleHeight) / 2;

		int textX = box.x() + box.sw(GuiTheme.PANEL_PADDING);
		int textY = rowTop + (rowHeight - textRenderer.fontHeight) / 2;
		int available = Math.max(box.width() - box.sw(GuiTheme.PANEL_PADDING * 2 + GuiTheme.TOGGLE_WIDTH + 6), 10);

		context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(feature.displayName(), available)),
				textX, textY, textColor);

		drawToggle(toggleX, toggleY, toggleWidth, toggleHeight, visual.toggleAmount(), alpha);
	}

	/**
	 * Draws the switch of a feature row.
	 *
	 * @param x       the left edge of the switch, in scaled GUI pixels
	 * @param y       the top edge of the switch, in scaled GUI pixels
	 * @param width   the width of the switch, in scaled GUI pixels
	 * @param height  the height of the switch, in scaled GUI pixels
	 * @param amount  how far the switch is turned on, between zero and one
	 * @param alpha   the opacity of the switch, between zero and one
	 */
	private void drawToggle(int x, int y, int width, int height, float amount, float alpha) {
		ToggleSwitch.draw(context, x, y, amount, width, height, alpha);
	}

	private void drawSearchField(int x, int y, int width, int height, String query, int cursor, boolean focused,
			float alpha) {
		int borderColor = focused ? GuiTheme.FOCUS_BORDER : GuiTheme.PANEL_BORDER;

		GuiShapes.fillRoundedGradient(context, x, y, width, height, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(GuiTheme.TOGGLE_OFF_TRACK, alpha),
				GuiColors.withAlpha(GuiTheme.PANEL_BACKGROUND, alpha));
		GuiShapes.strokeRounded(context, x, y, width, height, GuiTheme.CORNER_RADIUS,
				GuiColors.withAlpha(borderColor, alpha));

		if (focused) {
			GuiShapes.glowRounded(context, x, y, width, height, GuiTheme.CORNER_RADIUS,
					GuiColors.withAlpha(GuiTheme.ACCENT, alpha * 0.6F), 2);
		}

		int textX = x + 5;
		int textY = y + (height - textRenderer.fontHeight) / 2;
		int textColor = GuiColors.withAlpha(focused ? GuiTheme.PRIMARY_TEXT : GuiTheme.SECONDARY_TEXT, alpha);

		if (query.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal("Search features..."), textX, textY,
					GuiColors.withAlpha(GuiTheme.PLACEHOLDER, alpha));
		} else {
			context.drawTextWithShadow(textRenderer, Text.literal(query), textX, textY, textColor);
		}

		if (!focused || !caretVisible()) {
			return;
		}

		int caretX = textX + textRenderer.getWidth(query.substring(0, Math.min(cursor, query.length())));
		context.fill(caretX, textY, caretX + 1, textY + textRenderer.fontHeight, textColor);
	}

	private boolean caretVisible() {
		return (System.nanoTime() / CARET_BLINK_NANOS) % 2L == 0L;
	}
}

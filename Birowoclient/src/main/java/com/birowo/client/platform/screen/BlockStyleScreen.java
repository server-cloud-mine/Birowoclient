package com.birowo.client.platform.screen;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import com.birowo.client.core.config.BlockSelection;
import com.birowo.client.core.config.EspSettings;
import com.birowo.client.core.config.EspShape;
import com.birowo.client.core.gui.GuiColors;
import com.birowo.client.core.gui.GuiTheme;
import com.birowo.client.core.util.ColourMath;
import com.birowo.client.platform.world.BlockColours;

/**
 * The screen in which the way one block of the overlay is shown is chosen.
 *
 * <p>The colour is chosen the way a colour is chosen anywhere else: a hue, a saturation and a
 * brightness, each of which is a bar that is dragged. The dyes of the game are offered as well,
 * so that the common colours are one click away.
 *
 * <p>Besides the colour of the block, the screen holds the way the block is drawn — by its edges,
 * by its faces, by both, or as a filled box — and whether a line is drawn from the middle of the
 * screen to the block. The line may have a colour of its own.
 *
 * <p>Everything is only handed over when the screen is confirmed; what is left alone is thrown
 * away.
 */
public final class BlockStyleScreen extends Screen {
	/**
	 * The width of the panel, in scaled GUI pixels.
	 */
	private static final int WIDTH = 240;

	/**
	 * The height of the header of the panel, in scaled GUI pixels.
	 */
	private static final int HEADER_HEIGHT = 26;

	/**
	 * The padding inside the panel, in scaled GUI pixels.
	 */
	private static final int PADDING = 10;

	/**
	 * The height of a row of buttons, in scaled GUI pixels.
	 */
	private static final int ROW_HEIGHT = 16;

	/**
	 * The gap between two rows, in scaled GUI pixels.
	 */
	private static final int ROW_GAP = 4;

	/**
	 * The number of rows that hold a number of their own.
	 */
	private static final int NUMBER_ROWS = 1;

	/**
	 * The gap between two of the numbers of a row.
	 */
	private static final int NUMBER_GAP = 2;

	/**
	 * The opacities the fill of a block is shown with, in the order they are offered.
	 */
	private static final int[] ALPHAS = { 0, 25, 50, 64, 100, 128, 160, 200, 255 };

	/**
	 * The widths the edges of a block are shown with, in the order they are offered.
	 */
	private static final int[] WIDTHS = { 1, 2, 3, 4, 5 };

	/**
	 * The opacity the fill of a block is shown with when it is chosen for the first time.
	 */
	private static final int DEFAULT_ALPHA = 64;

	/**
	 * The width the edges of a block are shown with when it is chosen for the first time.
	 */
	private static final int DEFAULT_WIDTH = 1;

	/**
	 * The height of a bar, in scaled GUI pixels.
	 */
	private static final int STRIP_HEIGHT = 12;

	/**
	 * The gap between two bars, in scaled GUI pixels.
	 */
	private static final int STRIP_GAP = 5;

	/**
	 * The width of the preview of the colour, in scaled GUI pixels.
	 */
	private static final int PREVIEW_WIDTH = 34;

	/**
	 * The height of the preview of the colour, in scaled GUI pixels.
	 */
	private static final int PREVIEW_HEIGHT = 20;

	/**
	 * The height of a colour of the palette, in scaled GUI pixels.
	 */
	private static final int PALETTE_HEIGHT = 14;

	/**
	 * The width of a colour of the palette, in scaled GUI pixels.
	 */
	private static final int PALETTE_WIDTH = 26;

	/**
	 * The gap between two colours of the palette, in scaled GUI pixels.
	 */
	private static final int PALETTE_GAP = 3;

	/**
	 * How many colours of the palette are on a row.
	 */
	private static final int PALETTE_COLUMNS = 8;

	/**
	 * Which colour of the block is being changed.
	 */
	private enum Target {
		/**
		 * The colour of the block itself.
		 */
		BLOCK,

		/**
		 * The colour of the line that is drawn to the block.
		 */
		TRACER
	}

	/**
	 * Which bar of the panel is being dragged.
	 */
	private enum Strip {
		/**
		 * The place of the colour on the colour wheel.
		 */
		HUE,

		/**
		 * How much of the colour is of the hue itself.
		 */
		SATURATION,

		/**
		 * How bright the colour is.
		 */
		VALUE
	}

	private final Screen previous;
	private final String name;
	private final Consumer<BlockSelection> confirmed;
	private final int defaultColour;

	/**
	 * The block as it is being changed.
	 */
	private BlockSelection selection;

	/**
	 * Which colour is being changed.
	 */
	private Target target = Target.BLOCK;

	private float hue;
	private float saturation;
	private float value;

	private float tracerHue;
	private float tracerSaturation;
	private float tracerValue;

	private Strip dragged;

	/**
	 * @param previous      the screen that is opened again once the style is chosen
	 * @param name          the name of the block the style belongs to
	 * @param selection     the block and the way it is shown now
	 * @param defaultColour the colour the block has by default
	 * @param confirmed     what is done with the block when the screen is confirmed
	 */
	public BlockStyleScreen(Screen previous, String name, BlockSelection selection, int defaultColour,
			Consumer<BlockSelection> confirmed) {
		super(Text.literal("Style"));

		this.previous = previous;
		this.name = name;
		this.confirmed = confirmed;
		this.selection = selection;
		this.defaultColour = defaultColour;

		ColourMath.Hsv hsv = ColourMath.rgbToHsv(selection.colour());

		this.hue = hsv.hue();
		this.saturation = hsv.saturation();
		this.value = hsv.value();

		int tracerColour = selection.settings().tracerColour() >= 0
				? selection.settings().tracerColour()
				: selection.colour();

		ColourMath.Hsv tracerHsv = ColourMath.rgbToHsv(tracerColour);

		this.tracerHue = tracerHsv.hue();
		this.tracerSaturation = tracerHsv.saturation();
		this.tracerValue = tracerHsv.value();
	}

	/**
	 * @return the colour that is being changed, as a {@code 0xRRGGBB} value
	 */
	private int colour() {
		return target == Target.BLOCK
				? ColourMath.hsvToRgb(hue, saturation, value)
				: ColourMath.hsvToRgb(tracerHue, tracerSaturation, tracerValue);
	}

	/**
	 * @return the height of the panel, in scaled GUI pixels
	 */
	private int panelHeight() {
		return HEADER_HEIGHT + PADDING
				+ ROW_HEIGHT + ROW_GAP
				+ PREVIEW_HEIGHT + STRIP_GAP
				+ STRIP_HEIGHT * 3 + STRIP_GAP * 2
				+ PALETTE_GAP + PALETTE_HEIGHT * 2
				+ ROW_GAP + ROW_HEIGHT
				+ ROW_GAP + ROW_HEIGHT * 2 + ROW_GAP
				+ ROW_GAP + ROW_HEIGHT
				+ ROW_GAP + ROW_HEIGHT * NUMBER_ROWS
				+ PADDING + PADDING;
	}

	/**
	 * @return the left edge of the panel, in scaled GUI pixels
	 */
	private int panelLeft() {
		return (width - WIDTH) / 2;
	}

	/**
	 * @return the top edge of the panel, in scaled GUI pixels
	 */
	private int panelTop() {
		return (height - panelHeight()) / 2;
	}

	/**
	 * @return the top edge of the row of the colours that are being changed
	 */
	private int targetTop() {
		return panelTop() + HEADER_HEIGHT + PADDING;
	}

	/**
	 * @return the top edge of the preview of the colour
	 */
	private int previewTop() {
		return targetTop() + ROW_HEIGHT + ROW_GAP;
	}

	/**
	 * @return the top edge of the bar of the hue
	 */
	private int hueTop() {
		return previewTop() + PREVIEW_HEIGHT + STRIP_GAP;
	}

	/**
	 * @return the top edge of the bar of the saturation
	 */
	private int saturationTop() {
		return hueTop() + STRIP_HEIGHT + STRIP_GAP;
	}

	/**
	 * @return the top edge of the bar of the brightness
	 */
	private int valueTop() {
		return saturationTop() + STRIP_HEIGHT + STRIP_GAP;
	}

	/**
	 * @return the top edge of the palette
	 */
	private int paletteTop() {
		return valueTop() + STRIP_HEIGHT + STRIP_GAP * 2;
	}

	/**
	 * @return the top edge of the row of the ways the block is drawn
	 */
	private int shapeTop() {
		return paletteTop() + PALETTE_HEIGHT * 2 + PALETTE_GAP + ROW_GAP;
	}

	/**
	 * @return the top edge of the row of the tracer
	 */
	private int tracerTop() {
		return shapeTop() + ROW_HEIGHT * 2 + ROW_GAP;
	}

	/**
	 * @return the top edge of the row of the numbers
	 */
	private int alphaTop() {
		return tracerTop() + ROW_HEIGHT + ROW_GAP;
	}

	/**
	 * @return the top edge of the buttons
	 */
	private int buttonTop() {
		return alphaTop() + ROW_HEIGHT + ROW_GAP;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		int x = panelLeft();
		int y = panelTop();

		GuiShapes.fillRounded(context, 0, 0, width, height, 0, GuiColors.withAlpha(GuiTheme.BACKDROP, 0.85F));
		GuiShapes.softShadow(context, x, y, WIDTH, panelHeight(), GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_SHADOW, GuiShapes.SHADOW_LAYERS, 2);
		GuiShapes.fillRounded(context, x, y, WIDTH, panelHeight(), GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_BACKGROUND);
		GuiShapes.fillRoundedTopGradient(context, x, y, WIDTH, HEADER_HEIGHT, GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_HEADER_TOP, GuiTheme.PANEL_HEADER_BOTTOM);
		GuiShapes.strokeRounded(context, x, y, WIDTH, panelHeight(), GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_BORDER);

		context.drawTextWithShadow(textRenderer, Text.literal("Style of " + name), x + PADDING,
				y + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, GuiTheme.PRIMARY_TEXT);

		drawTargets(context, x);
		drawPreview(context, x);
		drawStrip(context, x, hueTop(), Strip.HUE);
		drawStrip(context, x, saturationTop(), Strip.SATURATION);
		drawStrip(context, x, valueTop(), Strip.VALUE);
		drawPalette(context, x);
		drawShapes(context, x);
		drawTracer(context, x);
		drawNumbers(context, x);
		drawButtons(context, x);
	}

	/**
	 * Draws the row of the colours that can be changed.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawTargets(DrawContext context, int x) {
		int left = x + PADDING;
		int top = targetTop();
		int half = (WIDTH - PADDING * 2) / 2;

		drawButton(context, left, top, half - 2, "Block", target == Target.BLOCK);
		drawButton(context, left + half + 2, top, half - 2, "Tracer", target == Target.TRACER);
	}

	/**
	 * Draws the colour that is chosen and the number that stands for it.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawPreview(DrawContext context, int x) {
		int top = previewTop();

		GuiShapes.fillRounded(context, x + PADDING, top, PREVIEW_WIDTH, PREVIEW_HEIGHT, 4,
				colour() | 0xFF000000);
		GuiShapes.strokeRounded(context, x + PADDING, top, PREVIEW_WIDTH, PREVIEW_HEIGHT, 4,
				GuiTheme.PANEL_BORDER);

		String hex = "#" + ColourMath.toHex(colour());
		String what = target == Target.BLOCK ? "Block" : "Tracer";

		context.drawTextWithShadow(textRenderer, Text.literal(what), x + PADDING + PREVIEW_WIDTH + 8,
				top + (PREVIEW_HEIGHT - textRenderer.fontHeight) / 2, GuiTheme.PRIMARY_TEXT);
		context.drawTextWithShadow(textRenderer, Text.literal(hex), x + WIDTH - PADDING
				- textRenderer.getWidth(hex), top + (PREVIEW_HEIGHT - textRenderer.fontHeight) / 2, GuiTheme.ACCENT);
	}

	/**
	 * Draws one of the bars of the panel.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 * @param top     the top edge of the bar, in scaled GUI pixels
	 * @param strip   the bar that is drawn
	 */
	private void drawStrip(DrawContext context, int x, int top, Strip strip) {
		int left = x + PADDING;
		int barWidth = WIDTH - PADDING * 2;

		float baseHue = target == Target.BLOCK ? hue : tracerHue;
		float baseSaturation = target == Target.BLOCK ? saturation : tracerSaturation;
		float baseValue = target == Target.BLOCK ? value : tracerValue;

		for (int column = 0; column < barWidth; column++) {
			float progress = barWidth > 1 ? column / (float) (barWidth - 1) : 0.0F;
			int colour;

			switch (strip) {
				case HUE -> colour = ColourMath.hsvToRgb(progress, 1.0F, 1.0F);
				case SATURATION -> colour = ColourMath.hsvToRgb(baseHue, progress, baseValue);
				default -> colour = ColourMath.hsvToRgb(baseHue, baseSaturation, progress);
			}

			context.fill(left + column, top, left + column + 1, top + STRIP_HEIGHT, colour | 0xFF000000);
		}

		GuiShapes.strokeRounded(context, left, top, barWidth, STRIP_HEIGHT, 3, GuiTheme.PANEL_BORDER);

		// The mark shows where the colour of the bar is.
		float position = switch (strip) {
			case HUE -> baseHue;
			case SATURATION -> baseSaturation;
			default -> baseValue;
		};

		int marker = left + Math.round(position * (barWidth - 1));

		context.fill(marker - 1, top - 1, marker + 2, top + STRIP_HEIGHT + 1, 0xFFFFFFFF);
	}

	/**
	 * Draws the colours of the dyes of the game.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawPalette(DrawContext context, int x) {
		int left = x + PADDING;
		int top = paletteTop();
		List<Map.Entry<String, Integer>> dyes = List.copyOf(BlockColours.dyeColours().entrySet());

		for (int index = 0; index < dyes.size(); index++) {
			int column = index % PALETTE_COLUMNS;
			int row = index / PALETTE_COLUMNS;
			int cellX = left + column * (PALETTE_WIDTH + PALETTE_GAP);
			int cellY = top + row * (PALETTE_HEIGHT + PALETTE_GAP);

			GuiShapes.fillRounded(context, cellX, cellY, PALETTE_WIDTH, PALETTE_HEIGHT, 3,
					dyes.get(index).getValue() | 0xFF000000);
			GuiShapes.strokeRounded(context, cellX, cellY, PALETTE_WIDTH, PALETTE_HEIGHT, 3,
					GuiTheme.PANEL_BORDER);
		}
	}

	/**
	 * Draws the row of the ways the block can be drawn.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawShapes(DrawContext context, int x) {
		EspShape chosen = shape();
		int left = x + PADDING;
		int half = (WIDTH - PADDING * 2) / 2;

		for (EspShape shape : EspShape.values()) {
			int index = shape.ordinal();
			int cellX = left + (index % 2) * (half + 2);
			int cellY = shapeTop() + (index / 2) * (ROW_HEIGHT + ROW_GAP);

			drawButton(context, cellX, cellY, half - 2, shape.displayName(), shape == chosen);
		}
	}

	/**
	 * Draws the row of the tracer.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawTracer(DrawContext context, int x) {
		drawButton(context, x + PADDING, tracerTop(), WIDTH - PADDING * 2,
				"Tracer: " + (tracer() ? "on" : "off"), tracer());
	}

	/**
	 * Draws the row of the numbers that may be chosen for the block.
	 *
	 * <p>The opacity of the fill, the width of the edges and the width of the tracer are chosen
	 * here. A number that is left alone follows the setting of the feature as a whole.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawNumbers(DrawContext context, int x) {
		EspSettings settings = selection.settings();
		int left = x + PADDING;
		int cellWidth = (WIDTH - PADDING * 2 - NUMBER_GAP * 2) / 3;

		drawNumber(context, left, cellWidth, "Alpha", settings.alpha());
		drawNumber(context, left + cellWidth + NUMBER_GAP, cellWidth, "Lines", settings.lineWidth());
		drawNumber(context, left + (cellWidth + NUMBER_GAP) * 2, cellWidth, "Tracer", settings.tracerWidth());
	}

	/**
	 * Draws one of the numbers.
	 *
	 * @param context   the draw context of the current frame
	 * @param x         the horizontal position of the number, in scaled GUI pixels
	 * @param cellWidth the width of the number, in scaled GUI pixels
	 * @param label     the name of the number
	 * @param value     the number as it is set, or a negative one when it follows the feature
	 */
	private void drawNumber(DrawContext context, int x, int cellWidth, String label, int value) {
		String text = value < 0 ? "auto" : String.valueOf(value);

		drawButton(context, x, alphaTop(), cellWidth, label + ": " + text, value >= 0);
	}

	/**
	 * Draws the buttons of the panel.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 */
	private void drawButtons(DrawContext context, int x) {
		int cursor = x + PADDING;
		int top = buttonTop();

		for (Button button : buttons()) {
			int buttonWidth = textRenderer.getWidth(button.label()) + 14;

			GuiShapes.fillRounded(context, cursor, top, buttonWidth, ROW_HEIGHT, 4,
					button.primary() ? GuiTheme.TOGGLE_ON_TRACK : GuiTheme.TOGGLE_OFF_TRACK);
			GuiShapes.strokeRounded(context, cursor, top, buttonWidth, ROW_HEIGHT, 4, GuiTheme.PANEL_BORDER);

			context.drawTextWithShadow(textRenderer, Text.literal(button.label()),
					cursor + (buttonWidth - textRenderer.getWidth(button.label())) / 2,
					top + (ROW_HEIGHT - textRenderer.fontHeight) / 2,
					button.primary() ? GuiTheme.PRIMARY_TEXT : GuiTheme.SECONDARY_TEXT);

			cursor += buttonWidth + 4;
		}
	}

	/**
	 * Draws one button of the panel.
	 *
	 * @param context  the draw context of the current frame
	 * @param x        the left edge of the button
	 * @param y        the top edge of the button
	 * @param buttonWidth the width of the button
	 * @param label    the text on the button
	 * @param chosen   whether the button is the one that is active
	 */
	private void drawButton(DrawContext context, int x, int y, int buttonWidth, String label, boolean chosen) {
		GuiShapes.fillRounded(context, x, y, buttonWidth, ROW_HEIGHT, 4,
				chosen ? GuiTheme.TOGGLE_ON_TRACK : GuiTheme.TOGGLE_OFF_TRACK);
		GuiShapes.strokeRounded(context, x, y, buttonWidth, ROW_HEIGHT, 4, GuiTheme.PANEL_BORDER);

		context.drawTextWithShadow(textRenderer, Text.literal(label),
				x + (buttonWidth - textRenderer.getWidth(label)) / 2,
				y + (ROW_HEIGHT - textRenderer.fontHeight) / 2,
				chosen ? GuiTheme.PRIMARY_TEXT : GuiTheme.SECONDARY_TEXT);
	}

	/**
	 * @return the buttons of the panel
	 */
	private List<Button> buttons() {
		return List.of(
				new Button("Done", true, this::confirm),
				new Button("Reset", false, this::reset),
				new Button("Cancel", false, this::cancel));
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double x = click.x();
		double y = click.y();

		if (clickButton(x, y)) {
			return true;
		}

		if (clickPalette(x, y)) {
			return true;
		}

		if (clickTargets(x, y)) {
			return true;
		}

		if (clickShapes(x, y)) {
			return true;
		}

		if (clickTracer(x, y)) {
			return true;
		}

		if (clickNumbers(x, y)) {
			return true;
		}

		Strip strip = stripAt(x, y);

		if (strip != null) {
			dragged = strip;
			applyStrip(strip, x);
			return true;
		}

		return true;
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		if (dragged != null) {
			applyStrip(dragged, click.x());
		}

		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		dragged = null;
		return true;
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(previous);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/**
	 * Clicks one of the buttons of the panel.
	 *
	 * @param x the horizontal position of the click, in scaled GUI pixels
	 * @param y the vertical position of the click, in scaled GUI pixels
	 * @return whether a button was clicked
	 */
	private boolean clickButton(double x, double y) {
		int cursor = panelLeft() + PADDING;
		int top = buttonTop();

		for (Button button : buttons()) {
			int buttonWidth = textRenderer.getWidth(button.label()) + 14;

			if (y >= top && y < top + ROW_HEIGHT && x >= cursor && x < cursor + buttonWidth) {
				button.action().run();
				return true;
			}

			cursor += buttonWidth + 4;
		}

		return false;
	}

	/**
	 * Clicks the row of the colours that can be changed.
	 *
	 * @param x the horizontal position of the click, in scaled GUI pixels
	 * @param y the vertical position of the click, in scaled GUI pixels
	 * @return whether a colour was chosen
	 */
	private boolean clickTargets(double x, double y) {
		int left = panelLeft() + PADDING;
		int top = targetTop();
		int half = (WIDTH - PADDING * 2) / 2;

		if (y < top || y >= top + ROW_HEIGHT) {
			return false;
		}

		if (x >= left && x < left + half - 2) {
			target = Target.BLOCK;
			return true;
		}

		if (x >= left + half + 2 && x < left + WIDTH - PADDING) {
			target = Target.TRACER;
			return true;
		}

		return false;
	}

	/**
	 * Clicks one of the ways the block can be drawn.
	 *
	 * @param x the horizontal position of the click, in scaled GUI pixels
	 * @param y the vertical position of the click, in scaled GUI pixels
	 * @return whether a way was chosen
	 */
	private boolean clickShapes(double x, double y) {
		int left = panelLeft() + PADDING;
		int half = (WIDTH - PADDING * 2) / 2;

		for (EspShape shape : EspShape.values()) {
			int index = shape.ordinal();
			int cellX = left + (index % 2) * (half + 2);
			int cellY = shapeTop() + (index / 2) * (ROW_HEIGHT + ROW_GAP);

			if (y >= cellY && y < cellY + ROW_HEIGHT && x >= cellX && x < cellX + half - 2) {
				selection = selection.withSettings(selection.settings().withShape(shape));
				return true;
			}
		}

		return false;
	}

	/**
	 * Clicks the row of the tracer.
	 *
	 * @param x the horizontal position of the click, in scaled GUI pixels
	 * @param y the vertical position of the click, in scaled GUI pixels
	 * @return whether the tracer was touched
	 */
	private boolean clickTracer(double x, double y) {
		int left = panelLeft() + PADDING;
		int top = tracerTop();

		if (y < top || y >= top + ROW_HEIGHT || x < left || x >= left + WIDTH - PADDING * 2) {
			return false;
		}

		selection = selection.withSettings(selection.settings().withTracer(!tracer()));
		return true;
	}

	/**
	 * Clicks one of the numbers.
	 *
	 * @param x the horizontal position of the click
	 * @param y the vertical position of the click
	 * @return whether one of the numbers was touched
	 */
	private boolean clickNumbers(double x, double y) {
		int left = panelLeft() + PADDING;
		int cellWidth = (WIDTH - PADDING * 2 - NUMBER_GAP * 2) / 3;
		EspSettings settings = selection.settings();

		if (y < alphaTop() || y >= alphaTop() + ROW_HEIGHT) {
			return false;
		}

		if (x >= left && x < left + cellWidth) {
			selection = selection.withSettings(
					settings.withAlpha(cycle(ALPHAS, settings.alpha(), DEFAULT_ALPHA)));
			return true;
		}

		if (x >= left + cellWidth + NUMBER_GAP && x < left + (cellWidth + NUMBER_GAP) * 2) {
			selection = selection.withSettings(
					settings.withLineWidth(cycle(WIDTHS, settings.lineWidth(), DEFAULT_WIDTH)));
			return true;
		}

		if (x >= left + (cellWidth + NUMBER_GAP) * 2 && x < left + (cellWidth + NUMBER_GAP) * 2 + cellWidth) {
			selection = selection.withSettings(
					settings.withTracerWidth(cycle(WIDTHS, settings.tracerWidth(), DEFAULT_WIDTH)));
			return true;
		}

		return false;
	}

	/**
	 * The next number of a row of numbers.
	 *
	 * @param choices the numbers that are offered, in the order they are offered
	 * @param current the number that is set now, or a negative one when there is none
	 * @param first   the number that is chosen when there is none yet
	 * @return the number that comes next
	 */
	private static int cycle(int[] choices, int current, int first) {
		if (current < 0) {
			return first;
		}

		for (int index = 0; index < choices.length; index++) {
			if (choices[index] == current) {
				return choices[(index + 1) % choices.length];
			}
		}

		return first;
	}

	/**
	 * Clicks one of the colours of the palette.
	 *
	 * @param x the horizontal position of the click, in scaled GUI pixels
	 * @param y the vertical position of the click, in scaled GUI pixels
	 * @return whether a colour was clicked
	 */
	private boolean clickPalette(double x, double y) {
		int left = panelLeft() + PADDING;
		int top = paletteTop();
		List<Map.Entry<String, Integer>> dyes = List.copyOf(BlockColours.dyeColours().entrySet());

		for (int index = 0; index < dyes.size(); index++) {
			int column = index % PALETTE_COLUMNS;
			int row = index / PALETTE_COLUMNS;
			int cellX = left + column * (PALETTE_WIDTH + PALETTE_GAP);
			int cellY = top + row * (PALETTE_HEIGHT + PALETTE_GAP);

			if (x >= cellX && x < cellX + PALETTE_WIDTH && y >= cellY && y < cellY + PALETTE_HEIGHT) {
				setColour(dyes.get(index).getValue());
				return true;
			}
		}

		return false;
	}

	/**
	 * @param x the horizontal position of the cursor, in scaled GUI pixels
	 * @param y the vertical position of the cursor, in scaled GUI pixels
	 * @return the bar the cursor is over, or {@code null} when it is over none of them
	 */
	private Strip stripAt(double x, double y) {
		int left = panelLeft() + PADDING;
		int barWidth = WIDTH - PADDING * 2;

		if (x < left || x >= left + barWidth) {
			return null;
		}

		if (y >= hueTop() && y < hueTop() + STRIP_HEIGHT) {
			return Strip.HUE;
		}

		if (y >= saturationTop() && y < saturationTop() + STRIP_HEIGHT) {
			return Strip.SATURATION;
		}

		if (y >= valueTop() && y < valueTop() + STRIP_HEIGHT) {
			return Strip.VALUE;
		}

		return null;
	}

	/**
	 * Moves the colour to the place of the cursor on a bar.
	 *
	 * @param strip the bar that is dragged
	 * @param x     the horizontal position of the cursor, in scaled GUI pixels
	 */
	private void applyStrip(Strip strip, double x) {
		int left = panelLeft() + PADDING;
		int barWidth = WIDTH - PADDING * 2;
		float progress = (float) Math.max(0.0D, Math.min(1.0D, (x - left) / Math.max(1, barWidth - 1)));

		if (target == Target.BLOCK) {
			switch (strip) {
				case HUE -> hue = progress;
				case SATURATION -> saturation = progress;
				default -> value = progress;
			}

			// The colour of the block is kept as it is changed, so that it is not lost when the
			// colour of the tracer is changed afterwards.
			selection = selection.withColour(colour());
			return;
		}

		switch (strip) {
			case HUE -> tracerHue = progress;
			case SATURATION -> tracerSaturation = progress;
			default -> tracerValue = progress;
		}

		selection = selection.withSettings(selection.settings().withTracerColour(tracerColour()));
	}

	/**
	 * @return the colour of the line to the block; a colour that is the colour of the block is
	 *         not written down, so that the line follows the colour of the block
	 */
	private int tracerColour() {
		int colour = ColourMath.hsvToRgb(tracerHue, tracerSaturation, tracerValue);
		return colour == selection.colour() ? -1 : colour;
	}

	/**
	 * Takes a colour that is known.
	 *
	 * @param colour the colour, as a {@code 0xRRGGBB} value
	 */
	private void setColour(int colour) {
		ColourMath.Hsv hsv = ColourMath.rgbToHsv(colour);

		if (target == Target.BLOCK) {
			this.hue = hsv.hue();
			this.saturation = hsv.saturation();
			this.value = hsv.value();

			selection = selection.withColour(colour());
			return;
		}

		this.tracerHue = hsv.hue();
		this.tracerSaturation = hsv.saturation();
		this.tracerValue = hsv.value();

		selection = selection.withSettings(selection.settings().withTracerColour(tracerColour()));
	}

	/**
	 * @return the way the block is drawn at the moment
	 */
	private EspShape shape() {
		EspShape shape = selection.settings().shape();
		return shape != null ? shape : EspShape.LINES;
	}

	/**
	 * @return whether a line is drawn to the block at the moment
	 */
	private boolean tracer() {
		Boolean tracer = selection.settings().tracer();
		return tracer != null ? tracer : true;
	}

	/**
	 * Hands the block over and closes the screen.
	 */
	private void confirm() {
		confirmed.accept(selection);
		close();
	}

	/**
	 * Puts the block back the way it was.
	 */
	private void reset() {
		selection = new BlockSelection(selection.identifier(), defaultColour, EspSettings.DEFAULT);
		target = Target.BLOCK;

		ColourMath.Hsv hsv = ColourMath.rgbToHsv(defaultColour);

		this.hue = hsv.hue();
		this.saturation = hsv.saturation();
		this.value = hsv.value();
		this.tracerHue = hsv.hue();
		this.tracerSaturation = hsv.saturation();
		this.tracerValue = hsv.value();
	}

	/**
	 * Throws the block away and closes the screen.
	 */
	private void cancel() {
		close();
	}

	/**
	 * A button of the panel.
	 *
	 * @param label   the text on the button
	 * @param primary whether the button is the one that confirms
	 * @param action  what the button does
	 */
	private record Button(String label, boolean primary, Runnable action) {
	}
}

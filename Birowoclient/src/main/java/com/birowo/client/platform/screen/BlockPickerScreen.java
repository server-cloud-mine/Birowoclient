package com.birowo.client.platform.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import net.minecraft.block.Block;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import com.birowo.client.core.config.BlockListSetting;
import com.birowo.client.core.config.BlockSelection;
import com.birowo.client.core.config.EspShape;
import com.birowo.client.core.gui.GuiColors;
import com.birowo.client.core.gui.GuiTheme;
import com.birowo.client.platform.world.BlockColours;
import com.birowo.client.platform.world.BlockList;
import com.birowo.client.platform.world.BlockPreset;

/**
 * The screen in which the blocks of the block overlay are chosen.
 *
 * <p>The list of blocks of the game is long, so it is shown in a list that is searched by name
 * and that is scrolled. A block is chosen by clicking it, and the choice is kept until the
 * screen is closed.
 *
 * <p>Every block has a colour of its own. Clicking a block with the right button opens the
 * screen in which that colour is chosen.
 */
public final class BlockPickerScreen extends Screen {
	/**
	 * The width of the panel, in scaled GUI pixels.
	 */
	private static final int WIDTH = 300;

	/**
	 * The height of the header of the panel, in scaled GUI pixels.
	 */
	private static final int HEADER_HEIGHT = 26;

	/**
	 * The height of the search field, in scaled GUI pixels.
	 */
	private static final int SEARCH_HEIGHT = 20;

	/**
	 * The height of a row of the list, in scaled GUI pixels.
	 */
	private static final int ROW_HEIGHT = 15;

	/**
	 * The height of the list of blocks, in scaled GUI pixels.
	 */
	private static final int LIST_HEIGHT = 200;

	/**
	 * The height of the footer of the panel, in scaled GUI pixels.
	 */
	private static final int FOOTER_HEIGHT = 74;

	/**
	 * The padding inside the panel, in scaled GUI pixels.
	 */
	private static final int PADDING = 8;

	/**
	 * The height of a button of the footer, in scaled GUI pixels.
	 */
	private static final int BUTTON_HEIGHT = 18;

	/**
	 * The longest frame the animations may be advanced by, in seconds.
	 */
	private static final float MAX_FRAME_SECONDS = 0.1F;

	private static final String TITLE = "Select blocks";
	private static final String SEARCH_PLACEHOLDER = "Search blocks...";
	private static final String DONE = "Done";
	private static final String CLEAR = "Clear";
	private static final String RESET = "Reset";
	private static final String LOOKED_AT = "Looked at";
	private static final String NOTHING_FOUND = "No blocks found";
	private static final String HINT = "Left click chooses a block, and takes it away again";
	private static final String COLOUR_HINT = "Right click chooses the colour and the way of the block";

	private final Screen previous;
	private final BlockListSetting setting;

	/**
	 * The blocks that are chosen while the screen is open, by identifier, with their colour.
	 */
	private final LinkedHashMap<String, BlockSelection> selected = new LinkedHashMap<>();

	/**
	 * Every block of the game, in alphabetical order, with its name.
	 */
	private final List<BlockEntry> all = new ArrayList<>();

	/**
	 * The blocks that match the search.
	 */
	private final List<BlockEntry> matches = new ArrayList<>();

	private final StringBuilder query = new StringBuilder();

	private boolean dirty = true;
	private boolean queryFocused = true;

	private float scroll;
	private float scrollTarget;
	private long previousFrameNanos;

	/**
	 * @param previous the screen that is opened again once the blocks are chosen
	 * @param setting  the setting that holds the blocks
	 */
	public BlockPickerScreen(Screen previous, BlockListSetting setting) {
		super(Text.literal(TITLE));

		this.previous = previous;
		this.setting = setting;

		for (BlockSelection selection : setting.selections()) {
			selected.put(selection.identifier(), selection);
		}

		for (Identifier identifier : BlockList.allIdentifiers()) {
			all.add(new BlockEntry(identifier, BlockList.nameOf(identifier)));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float elapsed = previousFrameNanos == 0L ? 0.0F : (now - previousFrameNanos) / 1_000_000_000.0F;
		previousFrameNanos = now;

		advance(Math.min(elapsed, MAX_FRAME_SECONDS));

		if (dirty) {
			refresh();
		}

		int x = (width - WIDTH) / 2;
		int y = (height - panelHeight()) / 2;

		GuiShapes.fillRounded(context, 0, 0, width, height, 0, GuiColors.withAlpha(GuiTheme.BACKDROP, 0.85F));
		GuiShapes.softShadow(context, x, y, WIDTH, panelHeight(), GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_SHADOW, GuiShapes.SHADOW_LAYERS, 2);
		GuiShapes.fillRounded(context, x, y, WIDTH, panelHeight(), GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_BACKGROUND);
		GuiShapes.fillRoundedTopGradient(context, x, y, WIDTH, HEADER_HEIGHT, GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_HEADER_TOP, GuiTheme.PANEL_HEADER_BOTTOM);
		GuiShapes.strokeRounded(context, x, y, WIDTH, panelHeight(), GuiTheme.CORNER_RADIUS,
				GuiTheme.PANEL_BORDER);

		context.drawTextWithShadow(textRenderer, Text.literal(TITLE), x + PADDING,
				y + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, GuiTheme.PRIMARY_TEXT);

		String count = selected.size() + " chosen";
		context.drawTextWithShadow(textRenderer, Text.literal(count),
				x + WIDTH - PADDING - textRenderer.getWidth(count), y + (HEADER_HEIGHT - textRenderer.fontHeight) / 2,
				GuiTheme.ACCENT);

		drawSearchField(context, x, y);
		drawList(context, x, y, mouseX, mouseY);
		drawFooter(context, x, y);
	}

	/**
	 * Draws the field in which the blocks are searched for.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 * @param y       the top edge of the panel, in scaled GUI pixels
	 */
	private void drawSearchField(DrawContext context, int x, int y) {
		int fieldX = x + PADDING;
		int fieldY = y + HEADER_HEIGHT + PADDING / 2;
		int fieldWidth = WIDTH - PADDING * 2;

		GuiShapes.fillRoundedGradient(context, fieldX, fieldY, fieldWidth, SEARCH_HEIGHT, GuiTheme.CORNER_RADIUS,
				GuiTheme.TOGGLE_OFF_TRACK, GuiTheme.PANEL_BACKGROUND);
		GuiShapes.strokeRounded(context, fieldX, fieldY, fieldWidth, SEARCH_HEIGHT, GuiTheme.CORNER_RADIUS,
				queryFocused ? GuiTheme.FOCUS_BORDER : GuiTheme.PANEL_BORDER);

		int textX = fieldX + 5;
		int textY = fieldY + (SEARCH_HEIGHT - textRenderer.fontHeight) / 2;

		if (query.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(SEARCH_PLACEHOLDER), textX, textY,
					GuiTheme.PLACEHOLDER);
		} else {
			context.drawTextWithShadow(textRenderer, Text.literal(query.toString()), textX, textY,
					GuiTheme.PRIMARY_TEXT);
		}

		if (!queryFocused) {
			return;
		}

		int caretX = textX + textRenderer.getWidth(query.toString());
		context.fill(caretX, textY, caretX + 1, textY + textRenderer.fontHeight, GuiTheme.ACCENT);
	}

	/**
	 * Draws the list of blocks.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 * @param y       the top edge of the panel, in scaled GUI pixels
	 * @param mouseX  the horizontal position of the cursor, in scaled GUI pixels
	 * @param mouseY  the vertical position of the cursor, in scaled GUI pixels
	 */
	private void drawList(DrawContext context, int x, int y, int mouseX, int mouseY) {
		int listX = x + PADDING;
		int listY = listTop(y);
		int listWidth = WIDTH - PADDING * 2;

		context.enableScissor(x + 1, listY, x + WIDTH - 1, listY + LIST_HEIGHT);

		int visible = visibleRows();
		int first = Math.max(0, (int) scroll);
		int offset = Math.round((scroll - first) * ROW_HEIGHT);

		for (int row = 0; row < visible; row++) {
			int index = first + row;

			if (index >= matches.size()) {
				break;
			}

			BlockEntry entry = matches.get(index);
			int rowTop = listY + row * ROW_HEIGHT - offset;
			String identifier = entry.identifier().toString();
			BlockSelection selection = selected.get(identifier);
			boolean chosen = selection != null;
			int colour = chosen ? selection.colour() : 0;
			boolean hovered = mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT
					&& mouseX >= listX && mouseX < listX + listWidth;

			if (hovered) {
				GuiShapes.fillHorizontalGradient(context, listX, rowTop, listWidth, ROW_HEIGHT,
						GuiColors.withAlpha(GuiTheme.ROW_HOVER, 1.0F),
						GuiColors.withAlpha(GuiTheme.ROW_HOVER, 0.0F));
			}

			int textY = rowTop + (ROW_HEIGHT - textRenderer.fontHeight) / 2;
			String path = entry.identifier().getPath();
			String name = textRenderer.trimToWidth(entry.name(), listWidth - 20 - textRenderer.getWidth(path));

			if (chosen) {
				// A chosen block is marked with its own colour, so that it cannot be mistaken for
				// a block that is not in the list.
				context.fill(listX, rowTop, listX + 2, rowTop + ROW_HEIGHT, colour | 0xFF000000);
			}

			context.drawTextWithShadow(textRenderer, Text.literal(name), listX + 4, textY,
					chosen ? colour | 0xFF000000 : GuiTheme.SECONDARY_TEXT);
			context.drawTextWithShadow(textRenderer, Text.literal(path),
					listX + listWidth - 12 - textRenderer.getWidth(path), textY, GuiTheme.PLACEHOLDER);

			if (chosen) {
				// The mark is the colour of the block, so that the colours can be seen at a glance.
				GuiShapes.fillRounded(context, listX + listWidth - 8, rowTop + 4, 5, 5, 1,
						colour | 0xFF000000);
				GuiShapes.strokeRounded(context, listX + listWidth - 8, rowTop + 4, 5, 5, 1, GuiTheme.PANEL_BORDER);

				// The way the block is drawn is shown as well, when it is not the usual one.
				EspShape shape = selection.settings().shape();

				if (shape != null && shape != EspShape.LINES) {
					String mark = shape == EspShape.SIDES ? "S"
							: shape == EspShape.FILL ? "F" : "L+S";

					context.drawTextWithShadow(textRenderer, Text.literal(mark),
							listX + listWidth - 20 - textRenderer.getWidth(mark), textY, GuiTheme.ACCENT);
				}

				Boolean tracer = selection.settings().tracer();

				if (tracer != null && !tracer) {
					context.drawTextWithShadow(textRenderer, Text.literal("no tracer"),
							listX + listWidth - 40 - textRenderer.getWidth("no tracer"), textY,
							GuiTheme.PLACEHOLDER);
				}
			}
		}

		context.disableScissor();

		if (matches.isEmpty()) {
			context.drawTextWithShadow(textRenderer, Text.literal(NOTHING_FOUND), listX + 2,
					listY + 4, GuiTheme.PLACEHOLDER);
		}
	}

	/**
	 * Draws the buttons of the footer.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 * @param y       the top edge of the panel, in scaled GUI pixels
	 */
	private void drawFooter(DrawContext context, int x, int y) {
		int top = y + panelHeight() - FOOTER_HEIGHT + 4;

		context.drawTextWithShadow(textRenderer, Text.literal(HINT), x + PADDING, top, GuiTheme.PLACEHOLDER);
		context.drawTextWithShadow(textRenderer, Text.literal(COLOUR_HINT), x + PADDING,
				top + textRenderer.fontHeight, GuiTheme.PLACEHOLDER);

		drawButtons(context, x, top + textRenderer.fontHeight * 2 + 4, presetButtons());
		drawButtons(context, x, top + textRenderer.fontHeight * 2 + 4 + BUTTON_HEIGHT + 4, actionButtons());
	}

	/**
	 * Draws a row of buttons.
	 *
	 * @param context the draw context of the current frame
	 * @param x       the left edge of the panel, in scaled GUI pixels
	 * @param y       the top edge of the row, in scaled GUI pixels
	 * @param buttons the buttons of the row
	 */
	private void drawButtons(DrawContext context, int x, int y, List<Button> buttons) {
		int cursor = x + PADDING;

		for (Button button : buttons) {
			int buttonWidth = textRenderer.getWidth(button.label()) + 14;

			GuiShapes.fillRounded(context, cursor, y, buttonWidth, BUTTON_HEIGHT, 4,
					button.primary() ? GuiTheme.TOGGLE_ON_TRACK : GuiTheme.TOGGLE_OFF_TRACK);
			GuiShapes.strokeRounded(context, cursor, y, buttonWidth, BUTTON_HEIGHT, 4, GuiTheme.PANEL_BORDER);

			context.drawTextWithShadow(textRenderer, Text.literal(button.label()),
					cursor + (buttonWidth - textRenderer.getWidth(button.label())) / 2,
					y + (BUTTON_HEIGHT - textRenderer.fontHeight) / 2,
					button.primary() ? GuiTheme.PRIMARY_TEXT : GuiTheme.SECONDARY_TEXT);

			cursor += buttonWidth + 4;
		}
	}

	/**
	 * @return the buttons that fill the list with a group of blocks
	 */
	private List<Button> presetButtons() {
		return List.of(
				new Button("Ores", false, () -> fill(BlockPreset.ORE)),
				new Button("Valuable", false, () -> fill(BlockPreset.VALUABLE)),
				new Button("Rare", false, () -> fill(BlockPreset.RARE)));
	}

	/**
	 * @return the buttons that change the list itself
	 */
	private List<Button> actionButtons() {
		return List.of(
				new Button(LOOKED_AT, false, this::addLookedAtBlock),
				new Button(RESET, false, () -> fill(BlockPreset.VALUABLE)),
				new Button(CLEAR, false, this::clear),
				new Button(DONE, true, this::apply));
	}

	/**
	 * @return the height of the panel, in scaled GUI pixels
	 */
	private int panelHeight() {
		return HEADER_HEIGHT + PADDING / 2 + SEARCH_HEIGHT + 4 + LIST_HEIGHT + FOOTER_HEIGHT;
	}

	/**
	 * @param y the top edge of the panel, in scaled GUI pixels
	 * @return the top edge of the list of blocks, in scaled GUI pixels
	 */
	private int listTop(int y) {
		return y + HEADER_HEIGHT + PADDING / 2 + SEARCH_HEIGHT + 4;
	}

	/**
	 * @return how many rows of the list are seen at once
	 */
	private int visibleRows() {
		return Math.max(1, LIST_HEIGHT / ROW_HEIGHT);
	}

	/**
	 * @return the greatest scroll offset of the list, in rows
	 */
	private int maxScroll() {
		return Math.max(0, matches.size() - visibleRows());
	}

	/**
	 * Fills the list with the blocks of a group.
	 *
	 * <p>Every block keeps the colour it has by default, so that a group of blocks is told apart
	 * at a glance before any colour is changed.
	 *
	 * @param preset the group
	 */
	private void fill(BlockPreset preset) {
		selected.clear();

		for (String identifier : preset.identifiers()) {
			selected.put(identifier, new BlockSelection(identifier, BlockColours.colourOf(identifier)));
		}

		dirty = true;
	}

	/**
	 * Empties the list.
	 */
	private void clear() {
		selected.clear();
		dirty = true;
	}

	/**
	 * Writes the blocks that were chosen into the setting and closes the screen.
	 */
	private void apply() {
		List<BlockSelection> selections = new ArrayList<>();

		for (Map.Entry<String, BlockSelection> entry : selected.entrySet()) {
			selections.add(entry.getValue());
		}

		setting.selections(selections);
		close();
	}

	/**
	 * Rebuilds the list of the blocks that match the search.
	 */
	private void refresh() {
		matches.clear();

		String needle = query.toString().toLowerCase(Locale.ROOT);

		for (BlockEntry entry : all) {
			if (needle.isEmpty()
					|| entry.identifier().toString().toLowerCase(Locale.ROOT).contains(needle)
					|| entry.name().toLowerCase(Locale.ROOT).contains(needle)) {
				matches.add(entry);
			}
		}

		dirty = false;
		scrollTarget = Math.min(scrollTarget, maxScroll());
	}

	/**
	 * Advances the animation of the list.
	 *
	 * @param elapsed the time since the previous frame, in seconds
	 */
	private void advance(float elapsed) {
		float factor = 1.0F - (float) Math.exp(-20.0F * elapsed);
		scroll += (scrollTarget - scroll) * factor;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double x = click.x();
		double y = click.y();

		int panelX = (width - WIDTH) / 2;
		int panelY = (height - panelHeight()) / 2;

		int fieldTop = panelY + HEADER_HEIGHT + PADDING / 2;
		int fieldBottom = fieldTop + SEARCH_HEIGHT;

		if (y >= fieldTop && y < fieldBottom && x >= panelX + PADDING && x < panelX + WIDTH - PADDING) {
			queryFocused = true;
			return true;
		}

		int footerTop = panelY + panelHeight() - FOOTER_HEIGHT + 4;
		int firstRow = footerTop + textRenderer.fontHeight * 2 + 4;

		if (clickButton(firstRow, x, y, presetButtons())) {
			return true;
		}

		if (clickButton(firstRow + BUTTON_HEIGHT + 4, x, y, actionButtons())) {
			return true;
		}

		int listTop = listTop(panelY);
		int listBottom = listTop + LIST_HEIGHT;

		if (y >= listTop && y < listBottom && x >= panelX + PADDING && x < panelX + WIDTH - PADDING) {
			int index = (int) ((y - listTop + scroll) / ROW_HEIGHT);

			if (index >= 0 && index < matches.size()) {
				BlockEntry entry = matches.get(index);
				String identifier = entry.identifier().toString();

				if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					// The right button opens the style of the block.
					openStyle(entry);
					return true;
				}

				if (selected.remove(identifier) == null) {
					selected.put(identifier, new BlockSelection(identifier, BlockColours.colourOf(identifier)));
				}

				dirty = true;
			}

			return true;
		}

		queryFocused = false;
		return true;
	}

	/**
	 * Opens the screen in which the way a block is shown is chosen.
	 *
	 * @param entry the block whose way is chosen
	 */
	private void openStyle(BlockEntry entry) {
		String identifier = entry.identifier().toString();
		BlockSelection selection = selected.get(identifier);

		if (selection == null) {
			selection = new BlockSelection(identifier, BlockColours.colourOf(identifier));
			selected.put(identifier, selection);
			dirty = true;
		}

		if (client == null) {
			return;
		}

		client.setScreen(new BlockStyleScreen(this, entry.name(), selection, BlockColours.colourOf(identifier),
				chosen -> {
					selected.put(identifier, chosen);
					dirty = true;
				}));
	}

	/**
	 * Clicks a button of the footer.
	 *
	 * @param buttonY the top edge of the row of buttons, in scaled GUI pixels
	 * @param x       the horizontal position of the click, in scaled GUI pixels
	 * @param y       the vertical position of the click, in scaled GUI pixels
	 * @param buttons the buttons of the row
	 * @return whether a button was clicked
	 */
	private boolean clickButton(int buttonY, double x, double y, List<Button> buttons) {
		int cursor = (width - WIDTH) / 2 + PADDING;

		for (Button button : buttons) {
			int buttonWidth = textRenderer.getWidth(button.label()) + 14;

			if (y >= buttonY && y < buttonY + BUTTON_HEIGHT && x >= cursor && x < cursor + buttonWidth) {
				button.action().run();
				return true;
			}

			cursor += buttonWidth + 4;
		}

		return false;
	}

	/**
	 * Adds the block the player is looking at to the list.
	 *
	 * <p>Looking at a block and pressing the button is the quickest way to build a list of the
	 * blocks that are worth looking for.
	 */
	private void addLookedAtBlock() {
		if (client == null || client.world == null) {
			return;
		}

		HitResult hit = client.crosshairTarget;

		if (!(hit instanceof BlockHitResult blockHit)) {
			return;
		}

		Block block = client.world.getBlockState(blockHit.getBlockPos()).getBlock();
		Identifier identifier = Registries.BLOCK.getId(block);

		if (identifier == null) {
			return;
		}

		selected.putIfAbsent(identifier.toString(),
				new BlockSelection(identifier.toString(), BlockColours.colourOf(identifier.toString())));
		dirty = true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scrollTarget = Math.max(0.0F, Math.min(maxScroll(), scrollTarget - (float) vertical));
		return true;
	}

	@Override
	public boolean charTyped(CharInput charInput) {
		if (!queryFocused || !charInput.isValidChar()) {
			return false;
		}

		String typed = charInput.asString();

		if (typed.isEmpty()) {
			return false;
		}

		query.append(typed);
		dirty = true;
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput key) {
		if (key.isEscape()) {
			// Escape throws the blocks that were chosen away.
			close();
			return true;
		}

		if (!queryFocused) {
			return true;
		}

		if (key.getKeycode() == GLFW.GLFW_KEY_BACKSPACE) {
			if (!query.isEmpty()) {
				query.setLength(query.length() - 1);
				dirty = true;
			}

			return true;
		}

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
	 * One block of the list.
	 *
	 * @param identifier the identifier of the block
	 * @param name       the name of the block as the game shows it
	 */
	private record BlockEntry(Identifier identifier, String name) {
	}

	/**
	 * A button of the footer.
	 *
	 * @param label   the text on the button
	 * @param primary whether the button is the one that confirms
	 * @param action  what the button does
	 */
	private record Button(String label, boolean primary, Runnable action) {
	}
}

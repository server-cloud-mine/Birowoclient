package com.birowo.client.core.gui;

/**
 * The visual language of the interface: palette and metrics.
 *
 * <p>Every dimension is expressed in scaled GUI pixels, the same space the game's own screens use,
 * so the interface scales with the player's GUI scale setting.
 *
 * <p>The palette is built around a single blue. Every other colour is either a shade of that blue
 * or a neutral that sits next to it, so the interface reads as one thing rather than as a
 * collection of parts.
 */
public final class GuiTheme {
	private GuiTheme() {
	}

	// Palette ---------------------------------------------------------------------------------

	/**
	 * The colour the screen is dimmed with, behind the panels.
	 */
	public static final int BACKDROP = 0xE60D131C;

	/**
	 * The fill colour of a panel body.
	 */
	public static final int PANEL_BACKGROUND = 0xF0141A24;

	/**
	 * The fill colour at the top of a panel header.
	 */
	public static final int PANEL_HEADER_TOP = 0xF41E2C42;

	/**
	 * The fill colour at the bottom of a panel header.
	 */
	public static final int PANEL_HEADER_BOTTOM = 0xF4161F2E;

	/**
	 * The colour of a panel border.
	 */
	public static final int PANEL_BORDER = 0xFF2B3A50;

	/**
	 * The colour of the border of a panel that the cursor is over.
	 */
	public static final int PANEL_BORDER_ACTIVE = 0xFF3E6FA8;

	/**
	 * The colour of the soft shadow drawn under a panel.
	 */
	public static final int PANEL_SHADOW = 0x60000000;

	/**
	 * The colour of the text of an enabled feature and of the category names.
	 */
	public static final int PRIMARY_TEXT = 0xFFE2E9F5;

	/**
	 * The colour of secondary text, such as a feature that is turned off.
	 */
	public static final int SECONDARY_TEXT = 0xFF8A98AD;

	/**
	 * The accent colour, used for the active state of a toggle and of a feature.
	 */
	public static final int ACCENT = 0xFF4D9BFF;

	/**
	 * The accent colour where it is darkest, used for the bottom of a gradient.
	 */
	public static final int ACCENT_DEEP = 0xFF1E4E8C;

	/**
	 * The accent colour where it is brightest, used for the top of a gradient.
	 */
	public static final int ACCENT_BRIGHT = 0xFF7FBBFF;

	/**
	 * The colour of the glow around an active element.
	 */
	public static final int ACCENT_GLOW = 0x594D9BFF;

	/**
	 * The colour of the toggle that is off.
	 */
	public static final int TOGGLE_OFF = 0xFF4E5A6C;

	/**
	 * The colour of the track of a toggle that is off.
	 */
	public static final int TOGGLE_OFF_TRACK = 0xFF232C3A;

	/**
	 * The colour of the track of a toggle that is on.
	 */
	public static final int TOGGLE_ON_TRACK = 0xFF23578F;

	/**
	 * The colour of the row highlight drawn under the cursor.
	 */
	public static final int ROW_HOVER = 0x1F4D9BFF;

	/**
	 * The colour of the separator drawn between the header and the body of a panel.
	 */
	public static final int SEPARATOR = 0xFF22304A;

	/**
	 * The colour of the text shown in the search field when it is empty.
	 */
	public static final int PLACEHOLDER = 0xFF5E6B80;

	/**
	 * The colour of the border of the search field when it has focus.
	 */
	public static final int FOCUS_BORDER = 0xFF4D9BFF;

	/**
	 * The fill colour of the tooltip that describes a feature.
	 */
	public static final int TOOLTIP_BACKGROUND = 0xF00D1219;

	/**
	 * The colour of the border of the tooltip that describes a feature.
	 */
	public static final int TOOLTIP_BORDER = 0xFF33445E;

	// Metrics ---------------------------------------------------------------------------------

	/**
	 * The width of a category panel, in scaled GUI pixels.
	 */
	public static final int PANEL_WIDTH = 150;

	/**
	 * The height of a panel header, in scaled GUI pixels.
	 */
	public static final int PANEL_HEADER_HEIGHT = 26;

	/**
	 * The height of a feature row, in scaled GUI pixels.
	 */
	public static final int ROW_HEIGHT = 22;

	/**
	 * The padding between the edge of a panel and its content.
	 */
	public static final int PANEL_PADDING = 6;

	/**
	 * The gap between two panels of the default row.
	 */
	public static final int PANEL_GAP = 12;

	/**
	 * The corner radius of a panel, a toggle and the search field.
	 */
	public static final int CORNER_RADIUS = 5;

	/**
	 * The width of the accent bar drawn at the left of the header of a panel, in scaled GUI pixels.
	 */
	public static final int ACCENT_BAR_WIDTH = 2;

	/**
	 * The width of a toggle, in scaled GUI pixels.
	 */
	public static final int TOGGLE_WIDTH = 26;

	/**
	 * The height of a toggle, in scaled GUI pixels.
	 */
	public static final int TOGGLE_HEIGHT = 14;

	/**
	 * The diameter of the knob of a toggle, in scaled GUI pixels.
	 */
	public static final int TOGGLE_KNOB = 10;

	/**
	 * The height of the search field, in scaled GUI pixels.
	 */
	public static final int SEARCH_FIELD_HEIGHT = 20;

	/**
	 * The height of a row in the search result list, in scaled GUI pixels.
	 */
	public static final int SEARCH_ROW_HEIGHT = 19;

	/**
	 * The maximum height of a panel body before it starts to scroll, in scaled GUI pixels.
	 */
	public static final int MAX_BODY_HEIGHT = 220;

	/**
	 * The size of a category icon, in scaled GUI pixels. The glyphs are seven pixels wide.
	 */
	public static final int ICON_SIZE = 7;

	/**
	 * The size of the picture of a category, in scaled GUI pixels. An item is a square of sixteen
	 * pixels, so this is the size it is drawn at.
	 */
	public static final int CATEGORY_ICON_SIZE = 16;

	/**
	 * The gap between the icon of a header and its title, in scaled GUI pixels.
	 */
	public static final int ICON_GAP = 6;

	/**
	 * The scale a panel grows from while it opens.
	 */
	public static final float OPEN_MIN_SCALE = 0.96F;

	/**
	 * The scale the settings panel grows from while it opens.
	 */
	public static final float SETTINGS_MIN_SCALE = 0.94F;

	/**
	 * The maximum width of a panel that may be visible on screen, in scaled GUI pixels.
	 */
	public static final int EDGE_MARGIN = 4;
}

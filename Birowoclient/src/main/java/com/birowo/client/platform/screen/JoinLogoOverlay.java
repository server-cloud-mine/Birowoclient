package com.birowo.client.platform.screen;

import net.minecraft.client.gui.DrawContext;

import com.birowo.client.core.gui.GuiTheme;
import com.birowo.client.core.gui.JoinLogoTimeline;

/**
 * The logo that is shown when the player joins a server.
 *
 * <p>It is drawn on the HUD rather than as a screen of its own, so that the player can play
 * while it is on screen: the game is never taken away from them. The logo fades in over the
 * world, holds for a moment and fades away again.
 */
public final class JoinLogoOverlay {
	/**
	 * How much of the width of the screen the logo may take up.
	 */
	private static final float SCREEN_SHARE = 0.34F;

	/**
	 * The colour of the soft plate the logo is drawn on, which keeps it readable over a bright
	 * sky.
	 */
	private static final int PLATE = 0x66080C14;

	private final JoinLogoTimeline timeline = new JoinLogoTimeline();

	private long startNanos;

	/**
	 * Starts the animation. Calling it again restarts it.
	 */
	public void start() {
		startNanos = System.nanoTime();
	}

	/**
	 * @return whether the animation is on screen at the moment
	 */
	public boolean running() {
		return startNanos != 0L;
	}

	/**
	 * Draws the logo, if it is on screen.
	 *
	 * @param context the draw context of the current frame
	 */
	public void render(DrawContext context) {
		if (startNanos == 0L) {
			return;
		}

		float elapsed = (System.nanoTime() - startNanos) / 1_000_000_000.0F;
		JoinLogoTimeline.Frame frame = timeline.frameAt(elapsed);

		if (frame.finished()) {
			startNanos = 0L;
			return;
		}

		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		float scale = screenWidth * SCREEN_SHARE / LogoPainter.LOGO_WIDTH;
		float animated = scale * (float) frame.scale();

		int logoWidth = Math.round(LogoPainter.LOGO_WIDTH * animated);
		int logoHeight = Math.round(LogoPainter.LOGO_HEIGHT * animated);
		int originX = (screenWidth - logoWidth) / 2;
		int originY = (screenHeight - logoHeight) / 2;

		// The plate is a little larger than the logo, so that the logo does not touch its edge.
		int padding = Math.max(4, Math.round(14 * animated));
		int alpha = (int) Math.round(Math.max(0.0F, Math.min(1.0F, (float) frame.alpha())) * 255.0F);

		GuiShapes.fillRounded(context,
				originX - padding, originY - padding,
				logoWidth + padding * 2, logoHeight + padding * 2,
				GuiTheme.CORNER_RADIUS,
				(PLATE & 0x00FFFFFF) | (alpha << 24));

		LogoPainter.draw(context, originX, originY, animated, (float) frame.alpha());
	}
}

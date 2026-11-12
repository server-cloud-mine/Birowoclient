package com.birowo.client.platform.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

import com.birowo.client.core.gui.IntroTimeline;

/**
 * The animation the client plays when the game opens.
 *
 * <p>The screen is the stage for the logo: the blue slash burns from one end to the other, the
 * letter appears out of it, and the name is typed out letter by letter. The logo holds for a
 * moment and then fades away, leaving the screen that was open before it.
 *
 * <p>Nothing here decides when the animation happens; the timing lives in
 * {@link IntroTimeline} and the screen only draws what the timeline asks for.
 */
public final class IntroScreen extends Screen {
	/**
	 * How much of the screen the logo may take up.
	 */
	private static final float SCREEN_SHARE = 0.78F;

	/**
	 * How much of the height of the screen the logo may take up.
	 */
	private static final float HEIGHT_SHARE = 0.55F;

	/**
	 * The smallest the logo may be drawn.
	 */
	private static final float MINIMUM_SCALE = 0.35F;

	/**
	 * The colour of the backdrop at the top.
	 */
	private static final int BACKDROP_TOP = 0x0A0E1A;

	/**
	 * The colour of the backdrop at the bottom.
	 */
	private static final int BACKDROP_BOTTOM = 0x05070C;

	private final IntroTimeline timeline = new IntroTimeline();
	private final Screen previous;

	private long startNanos;

	/**
	 * @param previous the screen that was open before the animation and that is opened again
	 *                 once the animation is over
	 */
	public IntroScreen(Screen previous) {
		super(Text.literal("Birowo Client"));
		this.previous = previous;
	}

	@Override
	protected void init() {
		// The clock only starts once: the screen is initialised again when the window is resized,
		// and the animation must not start over because of that.
		if (startNanos == 0L) {
			startNanos = System.nanoTime();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		float elapsed = (System.nanoTime() - startNanos) / 1_000_000_000.0F;
		IntroTimeline.Frame frame = timeline.frameAt(elapsed);

		if (frame.finished()) {
			close();
			return;
		}

		drawBackdrop(context, frame);
		drawLogo(context, frame);
	}

	/**
	 * Draws the backdrop the logo stands on.
	 *
	 * @param context the drawing context
	 * @param frame   the current frame of the animation
	 */
	private void drawBackdrop(DrawContext context, IntroTimeline.Frame frame) {
		float alpha = (float) frame.alpha();

		context.fillGradient(0, 0, width, height,
				tint(BACKDROP_TOP, alpha),
				tint(BACKDROP_BOTTOM, alpha));
	}

	/**
	 * Draws the logo: the slash first, then the letter on top of it, then the name.
	 *
	 * @param context the drawing context
	 * @param frame   the current frame of the animation
	 */
	private void drawLogo(DrawContext context, IntroTimeline.Frame frame) {
		float scale = baseScale() * (float) frame.groupScale();
		int originX = Math.round((width - LogoPainter.LOGO_WIDTH * scale) / 2.0F);
		int originY = Math.round((height - LogoPainter.LOGO_HEIGHT * scale) / 2.0F);
		float alpha = (float) frame.alpha();

		LogoPainter.drawMark(context, originX, originY, scale, (float) frame.markAlpha() * alpha);
		LogoPainter.drawSlash(context, originX, originY, scale, frame.slashProgress(), alpha);
		LogoPainter.drawWord(context, originX, originY, scale, frame.lettersTyped(),
				(float) frame.letterReveal(), alpha);
	}

	/**
	 * @return the size the logo is drawn at when the screen is of its usual size
	 */
	private float baseScale() {
		float byWidth = width * SCREEN_SHARE / LogoPainter.LOGO_WIDTH;
		float byHeight = height * HEIGHT_SHARE / LogoPainter.LOGO_HEIGHT;

		return Math.max(MINIMUM_SCALE, Math.min(byWidth, byHeight));
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

	@Override
	public void close() {
		MinecraftClient client = MinecraftClient.getInstance();
		client.setScreen(previous != null ? previous : new TitleScreen());
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
}

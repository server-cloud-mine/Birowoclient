package com.birowo.client.features.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.core.util.ScreenProjection;
import com.birowo.client.platform.render.CameraView;
import com.birowo.client.platform.render.HudRenderable;
import com.birowo.client.platform.render.ShapeRenderer;
import com.birowo.client.platform.render.ViewProjection;
import com.birowo.client.platform.render.WorldLayers;
import com.birowo.client.platform.render.WorldRenderable;
import com.birowo.client.platform.screen.GuiShapes;

/**
 * Draws a line to the target the player is looking at.
 *
 * <p>The line can be drawn in two ways. On the screen it starts in the middle of the screen, at
 * the crosshair, and ends at the target; it is always the same width and is never hidden. In the
 * world it starts at the eye of the player and ends at the middle of the target; it is a part of
 * the world, so it grows thinner as it goes further away and it can be hidden by what is in
 * front of it.
 */
public final class TracerFeature extends Feature implements WorldRenderable, HudRenderable {
	/**
	 * The smallest number of pixels the line is allowed to be away from the middle of the screen
	 * before it is drawn as a mark rather than as a line.
	 */
	private static final double MINIMUM_LENGTH = 1.5D;

	private final ConfigValue<TracerStyle> style = setting(ConfigValues.ofEnum("style", TracerStyle.SCREEN));

	private final ConfigValue<Integer> colour = setting(ConfigValues.ofColor("colour", 0x4D9BFF));

	private final ConfigValue<Integer> width = setting(ConfigValues.ofInt("width", 2));

	private final ConfigValue<Boolean> throughWalls = setting(ConfigValues.ofBoolean("through_walls", true));

	public TracerFeature() {
		super("tracer", "Tracer", "Draws a line to the target the player is looking at.",
				FeatureCategory.RENDER);

		style.format(value -> value == TracerStyle.SCREEN ? "Screen" : "World");
	}

	@Override
	public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		if (style.value() != TracerStyle.SCREEN) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		Entity target = target(client);

		if (target == null) {
			return;
		}

		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();

		Vec3d middle = target.getEntityPos().add(0.0D, target.getHeight() * 0.5D, 0.0D);
		ScreenProjection.ScreenPoint point = ViewProjection.project(client, middle, screenWidth, screenHeight);

		if (point == null) {
			return;
		}

		int centreX = screenWidth / 2;
		int centreY = screenHeight / 2;
		int argb = colour.value() | 0xFF000000;
		int thickness = Math.max(1, width.value());

		if (Math.hypot(point.x() - centreX, point.y() - centreY) < MINIMUM_LENGTH) {
			// The target is right under the crosshair, so there is no line to be seen. A small
			// mark shows that the target has been found.
			GuiShapes.fillRounded(context, centreX - 1, centreY - 1, 3, 3, 1, argb);
			return;
		}

		GuiShapes.drawLine(context, centreX, centreY,
				(int) Math.round(point.x()), (int) Math.round(point.y()), thickness, argb);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (style.value() != TracerStyle.WORLD) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		Entity target = target(client);

		if (target == null) {
			return;
		}

		Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
		Vec3d middle = target.getEntityPos().add(0.0D, target.getHeight() * 0.5D, 0.0D);

		// The line begins a little in front of the camera, on the way to the target, so that it
		// begins in the middle of the screen without being in the eye of the player: a line that
		// begins in the eye cannot be drawn at all.
		Vec3d start = CameraView.start(client.gameRenderer.getCamera(), middle);

		if (start == null) {
			return;
		}

		MatrixStack.Entry entry = context.matrices().peek();
		VertexConsumer consumer = context.consumers()
				.getBuffer(throughWalls.value() ? WorldLayers.linesNoDepth() : WorldLayers.linesWithDepth());

		// The world is drawn relative to the camera, so the camera itself is the origin of that
		// space. The line therefore starts at the origin, which is the middle of the screen, and
		// ends at the target.
		ShapeRenderer.drawSegment(consumer, entry,
				start.x - camera.x, start.y - camera.y, start.z - camera.z,
				middle.x - camera.x, middle.y - camera.y, middle.z - camera.z,
				colour.value() | 0xFF000000,
				Math.max(1.0F, width.value()));
	}

	/**
	 * The entity the player is looking at.
	 *
	 * @param client the client the player belongs to
	 * @return the target, or {@code null} when the player is looking at nothing that is alive
	 */
	private Entity target(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			return null;
		}

		HitResult hit = client.crosshairTarget;

		if (!(hit instanceof EntityHitResult entityHit)) {
			return null;
		}

		Entity target = entityHit.getEntity();

		if (target == client.player || !target.isAlive()) {
			return null;
		}

		return target;
	}
}

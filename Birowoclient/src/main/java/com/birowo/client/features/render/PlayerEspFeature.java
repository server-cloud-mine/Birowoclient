package com.birowo.client.features.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.config.EspColourMode;
import com.birowo.client.core.config.EspShape;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.core.util.NeighbourFaces;
import com.birowo.client.core.util.PlayerColours;
import com.birowo.client.core.util.ScreenProjection;
import com.birowo.client.platform.render.CameraView;
import com.birowo.client.platform.render.HudRenderable;
import com.birowo.client.platform.render.ShapeRenderer;
import com.birowo.client.platform.render.ViewProjection;
import com.birowo.client.platform.render.WorldLayers;
import com.birowo.client.platform.render.WorldRenderable;
import com.birowo.client.platform.screen.GuiShapes;

/**
 * Shows the other players of the server.
 *
 * <p>Every player is marked by a box around them, and the box may be filled, so that a player
 * who is behind a wall is still to be seen. The box is drawn in the world without a depth test,
 * so it is never hidden by what is in front of it, which is what makes it useful.
 *
 * <p>A line may be drawn from the middle of the screen to a player as well, so that the
 * direction of a player is known without having to look at them. The line is drawn on the screen
 * or in the world, whichever is chosen.
 *
 * <p>The colour of a player is either one colour for everybody, the colour of how much health is
 * left, or the colour of how far away the player is. A player that is very close is drawn a
 * little more weakly than a player that is far away, so that the player in front of the screen
 * does not hide everything else.
 */
public final class PlayerEspFeature extends Feature implements WorldRenderable, HudRenderable {
	/**
	 * How much of the colour of the filled box is kept.
	 */
	private static final int DEFAULT_FILL_ALPHA = 0x40;

	/**
	 * The smallest number of pixels the line is allowed to be away from the middle of the screen
	 * before it is drawn as a line at all.
	 */
	private static final double MINIMUM_LENGTH = 1.5D;

	private final ConfigValue<EspShape> shape = setting(ConfigValues.ofEnum("shape", EspShape.LINES_AND_SIDES));

	private final ConfigValue<EspColourMode> colourMode = setting(ConfigValues.ofEnum("colour_mode",
			EspColourMode.FIXED));

	private final ConfigValue<Integer> colour = setting(ConfigValues.ofColor("colour", 0x4D9BFF));

	private final ConfigValue<Integer> lineWidth = setting(ConfigValues.ofInt("line_width", 1));

	private final ConfigValue<Integer> fillAlpha = setting(ConfigValues.ofInt("fill_alpha", DEFAULT_FILL_ALPHA));

	private final ConfigValue<Double> fadeDistance = setting(ConfigValues.ofDouble("fade_distance", 3.0D));

	private final ConfigValue<Boolean> ignoreSelf = setting(ConfigValues.ofBoolean("ignore_self", true));

	private final ConfigValue<Boolean> tracer = setting(ConfigValues.ofBoolean("tracer", true));

	private final ConfigValue<TracerStyle> tracerStyle = setting(ConfigValues.ofEnum("tracer_style",
			TracerStyle.SCREEN));

	private final ConfigValue<Integer> tracerColour = setting(ConfigValues.ofColor("tracer_colour", 0x4D9BFF));

	private final ConfigValue<Integer> tracerWidth = setting(ConfigValues.ofInt("tracer_width", 1));

	public PlayerEspFeature() {
		super("playeresp", "Player ESP", "Shows the other players of the server.", FeatureCategory.RENDER);

		shape.format(value -> value.displayName());
		colourMode.format(value -> value.displayName());
		tracerStyle.format(value -> value == TracerStyle.SCREEN ? "Screen" : "World");
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null) {
			return;
		}

		Camera camera = client.gameRenderer.getCamera();
		Vec3d eye = camera.getCameraPos();
		MatrixStack.Entry entry = context.matrices().peek();
		float delta = client.getRenderTickCounter().getTickProgress(false);

		EspShape way = shape.value();
		VertexConsumer edges = context.consumers().getBuffer(WorldLayers.linesNoDepth());
		VertexConsumer sides = way.sides() || way.filled()
				? context.consumers().getBuffer(WorldLayers.filledBoxNoDepth())
				: null;

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == client.player || !player.isAlive()) {
				continue;
			}

			if (ignoreSelf.value() && player == client.getCameraEntity()) {
				continue;
			}

			// The box of the player is moved along with them, so that it follows them smoothly
			// between two ticks. The way the game itself does it is followed exactly.
			double offsetX = MathHelper.lerp((double) delta, player.lastRenderX, player.getX()) - player.getX();
			double offsetY = MathHelper.lerp((double) delta, player.lastRenderY, player.getY()) - player.getY();
			double offsetZ = MathHelper.lerp((double) delta, player.lastRenderZ, player.getZ()) - player.getZ();

			Box box = player.getBoundingBox().offset(offsetX, offsetY, offsetZ);

			double minX = box.minX - eye.x;
			double minY = box.minY - eye.y;
			double minZ = box.minZ - eye.z;
			double maxX = box.maxX - eye.x;
			double maxY = box.maxY - eye.y;
			double maxZ = box.maxZ - eye.z;

			int argb = colourOf(player, eye);
			float edgeThickness = Math.max(1.0F, lineWidth.value());

			if (way.lines()) {
				ShapeRenderer.drawBoxEdges(edges, entry, minX, minY, minZ, maxX, maxY, maxZ, argb,
						edgeThickness);
			}

			if (sides != null) {
				int bodyColour = (argb & 0x00FFFFFF) | (alpha(fillAlpha.value()) << 24);

				if (way.filled()) {
					ShapeRenderer.drawBoxFill(sides, entry, minX, minY, minZ, maxX, maxY, maxZ, bodyColour,
							bodyColour);
				} else {
					for (int face : NeighbourFaces.ALL) {
						ShapeRenderer.drawBoxFace(sides, entry, face, minX, minY, minZ, maxX, maxY, maxZ,
								bodyColour);
					}
				}
			}
		}

		if (tracerStyle.value() == TracerStyle.WORLD) {
			drawWorldTracers(context, entry, eye);
		}
	}

	@Override
	public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		if (tracerStyle.value() != TracerStyle.SCREEN) {
			return;
		}

		drawScreenTracers(context);
	}

	/**
	 * Draws the line to every player that has one, on the screen.
	 *
	 * @param context the draw context of the current frame
	 */
	private void drawScreenTracers(DrawContext context) {
		if (!tracer.value()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null) {
			return;
		}

		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		int centreX = screenWidth / 2;
		int centreY = screenHeight / 2;
		int argb = tracerColour.value() | 0xFF000000;
		int thickness = Math.max(1, tracerWidth.value());

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == client.player || !player.isAlive()) {
				continue;
			}

			Vec3d middle = middle(player);
			ScreenProjection.ScreenPoint point = ViewProjection.project(client, middle, screenWidth, screenHeight);

			if (point == null) {
				continue;
			}

			int endX = (int) Math.round(point.x());
			int endY = (int) Math.round(point.y());

			if (Math.hypot(endX - centreX, endY - centreY) < MINIMUM_LENGTH) {
				continue;
			}

			GuiShapes.drawLine(context, centreX, centreY, endX, endY, thickness, argb);
		}
	}

	/**
	 * Draws the line to every player that has one, in the world.
	 *
	 * @param context the render context of the current frame
	 * @param entry   the transformation the lines are drawn with
	 * @param eye     the place of the camera in the world
	 */
	private void drawWorldTracers(WorldRenderContext context, MatrixStack.Entry entry, Vec3d eye) {
		if (!tracer.value()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		Camera camera = client.gameRenderer.getCamera();
		VertexConsumer consumer = context.consumers().getBuffer(WorldLayers.linesNoDepth());
		int argb = tracerColour.value() | 0xFF000000;
		float thickness = Math.max(1.0F, tracerWidth.value());

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == client.player || !player.isAlive()) {
				continue;
			}

			Vec3d middle = middle(player);
			Vec3d start = CameraView.start(camera, middle);

			if (start == null) {
				continue;
			}

			ShapeRenderer.drawSegment(consumer, entry,
					start.x - eye.x, start.y - eye.y, start.z - eye.z,
					middle.x - eye.x, middle.y - eye.y, middle.z - eye.z,
					argb, thickness);
		}
	}

	/**
	 * The colour a player is shown in.
	 *
	 * <p>A player that is very close is shown a little more weakly than a player that is far
	 * away, so that the player in front of the screen does not hide everything else.
	 *
	 * @param player the player that is shown
	 * @param eye    the place of the camera in the world
	 * @return the colour of the player
	 */
	private int colourOf(AbstractClientPlayerEntity player, Vec3d eye) {
		int base = switch (colourMode.value()) {
			case HEALTH -> PlayerColours.byHealth(player.getHealth(), player.getMaxHealth());
			case DISTANCE -> PlayerColours.byDistance(Math.sqrt(player.squaredDistanceTo(eye)));
			default -> colour.value() | 0xFF000000;
		};

		double fade = fadeDistance.value();

		if (fade <= 0.0D) {
			return base;
		}

		double distance = Math.sqrt(player.squaredDistanceTo(eye));
		double strength = Math.min(1.0D, distance / fade);

		if (strength >= 1.0D) {
			return base;
		}

		int alpha = (int) (((base >>> 24) & 0xFF) * strength);

		return (alpha << 24) | (base & 0x00FFFFFF);
	}

	/**
	 * The middle of a player, which is where the line to the player ends.
	 *
	 * @param player the player the line points at
	 * @return the middle of the player, in world coordinates
	 */
	private Vec3d middle(AbstractClientPlayerEntity player) {
		return player.getEntityPos().add(0.0D, player.getHeight() * 0.5D, 0.0D);
	}

	/**
	 * @param value the alpha as it is set
	 * @return the alpha as it is used
	 */
	private static int alpha(int value) {
		return Math.min(255, Math.max(0, value));
	}
}

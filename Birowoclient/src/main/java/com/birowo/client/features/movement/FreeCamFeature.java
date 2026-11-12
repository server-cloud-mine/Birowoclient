package com.birowo.client.features.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.core.gui.GuiTheme;
import com.birowo.client.platform.camera.FreeCam;
import com.birowo.client.platform.render.HudRenderable;

/**
 * Lets the camera leave the player.
 *
 * <p>While the camera is free it can be anywhere and it can look anywhere, and the keys of the
 * movement move it instead of the player. The player stays where it is and keeps its direction, so
 * that the server sees nothing at all: the movement packets carry the same position and the same
 * rotation as they did before the camera was let go.
 *
 * <p>The camera moves with the time that has passed since the last frame, so that it travels the
 * same distance whatever the number of frames is. The mouse wheel changes how fast it travels,
 * and the way the game looks is left alone while it is free: the view does not bob, and the field
 * of view does not change with the speed.
 *
 * <p>The camera is let go with the key the client binds to it, and it is put back with the same
 * key. The feature is not written to the configuration file, so the camera is never free by
 * accident when the game is opened.
 */
public final class FreeCamFeature extends Feature implements HudRenderable {
	/**
	 * How many blocks the camera travels in one second at the speed of one.
	 */
	private static final double BLOCKS_PER_SECOND = 10.0D;

	/**
	 * The longest time that may pass between two frames, in seconds.
	 *
	 * <p>A frame that took longer than this — because the game was paused, for instance — must not
	 * throw the camera across the world.
	 */
	private static final float MAXIMUM_FRAME_SECONDS = 0.1F;

	/**
	 * The shortest time that may pass between two frames, in seconds.
	 */
	private static final float MINIMUM_FRAME_SECONDS = 0.001F;

	private final ConfigValue<Double> speed = setting(ConfigValues.ofDouble("speed", 0.8D));

	private final ConfigValue<Boolean> staySneaking = setting(ConfigValues.ofBoolean("stay_sneaking", true));

	/**
	 * Whether the camera is attached to the player.
	 */
	private boolean attached;

	/**
	 * The way the game was looked at before the camera was let go.
	 */
	private Perspective savedPerspective = Perspective.FIRST_PERSON;

	/**
	 * How much the field of view changed with the speed, before the camera was let go.
	 */
	private double savedFovEffect = 1.0D;

	/**
	 * Whether the view bobbed, before the camera was let go.
	 */
	private boolean savedBobView = true;

	/**
	 * Whether the player was sneaking when the camera was let go.
	 */
	private boolean sneakOnEnable;

	/**
	 * The time of the last frame, in nanoseconds.
	 */
	private long lastFrameNanos;

	public FreeCamFeature() {
		super("freecam", "FreeCam", "Lets the camera leave the player.", FeatureCategory.MOVEMENT);
	}

	@Override
	public boolean persistable() {
		// The camera is never free when the game is opened; it is let go by the player.
		return false;
	}

	@Override
	protected void onEnable() {
		attached = false;

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.options == null) {
			return;
		}

		// The way the game is looked at is remembered, so that it can be put back the way it was.
		savedPerspective = client.options.getPerspective();
		savedFovEffect = client.options.getFovEffectScale().getValue();
		savedBobView = client.options.getBobView().getValue();

		// The view of a free camera does not bob and does not change with the speed.
		client.options.getFovEffectScale().setValue(0.0D);
		client.options.getBobView().setValue(false);
		client.options.setPerspective(Perspective.FIRST_PERSON);

		// The wheel of the mouse changes the speed of the camera, and the speed is a setting of
		// the feature, so that it is kept and shown like every other setting.
		FreeCam.onSpeedChanged(value -> speed.set(value));
	}

	@Override
	protected void onDisable() {
		attached = false;

		FreeCam.deactivate();
		FreeCam.onSpeedChanged(null);

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.options == null) {
			return;
		}

		client.options.getFovEffectScale().setValue(savedFovEffect);
		client.options.getBobView().setValue(savedBobView);
		client.options.setPerspective(savedPerspective);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null) {
			// The camera goes back with the player when the player leaves the world, and it is let
			// go again when the player comes back.
			if (attached) {
				attached = false;
				FreeCam.deactivate();
			}

			return;
		}

		if (!attached) {
			attach(client);
		}

		// The player must not drift away while the camera is somewhere else. The velocity is set
		// on the client alone, so that the server is not told about it.
		client.player.setVelocityClient(Vec3d.ZERO);

		// The speed is a setting of the feature, and the wheel of the mouse changes it as well.
		FreeCam.speed(speed.value());

		// A player that was sneaking keeps sneaking, so that it does not fall off a ledge or
		// into water while the camera is somewhere else.
		FreeCam.sneaking(staySneaking.value() && sneakOnEnable);
	}

	@Override
	public void onRender() {
		if (!attached) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.options == null) {
			return;
		}

		long now = System.nanoTime();
		float elapsed = (now - lastFrameNanos) / 1_000_000_000.0F;
		lastFrameNanos = now;

		elapsed = Math.max(MINIMUM_FRAME_SECONDS, Math.min(MAXIMUM_FRAME_SECONDS, elapsed));

		GameOptions options = client.options;

		double forward = pressed(options.forwardKey) - pressed(options.backKey);
		double sideways = pressed(options.rightKey) - pressed(options.leftKey);
		double lift = pressed(options.jumpKey) - pressed(options.sneakKey);

		if (forward == 0.0D && sideways == 0.0D && lift == 0.0D) {
			return;
		}

		double travel = FreeCam.speed() * BLOCKS_PER_SECOND * elapsed;

		if (options.sprintKey.isPressed()) {
			travel *= 2.0D;
		}

		float yaw = FreeCam.yaw();
		float pitch = FreeCam.pitch();

		// The camera looks where the mouse looks, and it moves along the direction it looks.
		Vec3d front = client.player.getRotationVector(pitch, yaw);

		// The right hand side of the camera, on the level of the ground.
		double yawRadians = Math.toRadians(yaw);
		double rightX = -Math.cos(yawRadians);
		double rightZ = -Math.sin(yawRadians);

		double moveX = front.x * forward + rightX * sideways;
		double moveY = front.y * forward + lift;
		double moveZ = front.z * forward + rightZ * sideways;

		Vec3d position = FreeCam.position();

		FreeCam.moveTo(new Vec3d(
				position.x + moveX * travel,
				position.y + moveY * travel,
				position.z + moveZ * travel));
	}

	@Override
	public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		if (!enabled()) {
			return;
		}

		context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
				Text.literal("Free camera"), 4, 4, GuiTheme.ACCENT);
	}

	/**
	 * Lets the camera go.
	 *
	 * @param client the client the player belongs to
	 */
	private void attach(MinecraftClient client) {
		attached = true;

		float yaw = client.player.getYaw();
		float pitch = client.player.getPitch();

		if (savedPerspective == Perspective.THIRD_PERSON_FRONT) {
			// The camera looked at the player from the front, so it has to look the other way to
			// take the place of the eyes of the player.
			yaw += 180.0F;
			pitch = -pitch;
		}

		FreeCam.activate(client.player.getEyePos(), yaw, pitch);
		FreeCam.speed(speed.value());

		sneakOnEnable = client.player.isSneaking();
		lastFrameNanos = System.nanoTime();
	}

	/**
	 * @param key the key to look at
	 * @return one when the key is held down, and zero when it is not
	 */
	private static double pressed(net.minecraft.client.option.KeyBinding key) {
		return key.isPressed() ? 1.0D : 0.0D;
	}
}

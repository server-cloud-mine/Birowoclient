package com.birowo.client.platform.camera;

import java.util.function.DoubleConsumer;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The state of the camera that is free of the player.
 *
 * <p>The state is held here rather than in the feature, because the mixins that put the camera
 * where it belongs have to reach it from outside the client.
 *
 * <p>The camera is free in three ways: it can be anywhere, it can look anywhere, and it can move
 * on its own. The player is left alone while it is free, so that the server sees nothing at all.
 */
public final class FreeCam {
	/**
	 * How much of the movement of the mouse is one degree of turning.
	 *
	 * <p>The game turns the player by this much for every unit of mouse movement, and the free
	 * camera turns by exactly the same, so that the mouse feels the same as it always did.
	 */
	private static final float TURN = 0.15F;

	/**
	 * The slowest the camera may travel.
	 */
	private static final double MINIMUM_SPEED = 0.05D;

	/**
	 * The fastest the camera may travel.
	 */
	private static final double MAXIMUM_SPEED = 20.0D;

	private static boolean active;

	private static Vec3d position = Vec3d.ZERO;

	private static float yaw;

	private static float pitch;

	/**
	 * How fast the camera travels, as a multiple of the step of a tick.
	 */
	private static double speed = 1.0D;

	/**
	 * Whether the player is to keep sneaking while the camera is free.
	 */
	private static boolean sneaking;

	/**
	 * What is told when the speed of the camera is changed.
	 */
	private static DoubleConsumer speedChanged = value -> {
	};

	private FreeCam() {
	}

	/**
	 * @return whether the camera is free of the player
	 */
	public static boolean active() {
		return active;
	}

	/**
	 * @return where the free camera is
	 */
	public static Vec3d position() {
		return position;
	}

	/**
	 * @return the direction the free camera looks at, in degrees
	 */
	public static float yaw() {
		return yaw;
	}

	/**
	 * @return how far the free camera looks up or down, in degrees
	 */
	public static float pitch() {
		return pitch;
	}

	/**
	 * @return how fast the camera travels
	 */
	public static double speed() {
		return speed;
	}

	/**
	 * @param speed how fast the camera travels
	 */
	public static void speed(double speed) {
		FreeCam.speed = Math.max(MINIMUM_SPEED, Math.min(MAXIMUM_SPEED, speed));
	}

	/**
	 * @return whether the player is to keep sneaking while the camera is free
	 */
	public static boolean sneaking() {
		return sneaking;
	}

	/**
	 * @param sneaking whether the player is to keep sneaking while the camera is free
	 */
	public static void sneaking(boolean sneaking) {
		FreeCam.sneaking = sneaking;
	}

	/**
	 * Asks to be told when the speed of the camera is changed.
	 *
	 * @param listener what is told, or {@code null} to be told nothing
	 */
	public static void onSpeedChanged(DoubleConsumer listener) {
		FreeCam.speedChanged = listener != null ? listener : value -> {
		};
	}

	/**
	 * Changes the speed of the camera.
	 *
	 * <p>The speed grows and shrinks by a part of itself, so that the camera can be slowed down
	 * and sped up at the same rate whatever its speed is.
	 *
	 * @param direction the way the speed changes; positive makes the camera faster
	 */
	public static void adjustSpeed(int direction) {
		double step = Math.max(0.25D, speed * 0.15D);

		speed(Math.max(MINIMUM_SPEED, Math.min(MAXIMUM_SPEED, speed + direction * step)));
		speedChanged.accept(speed);
	}

	/**
	 * Lets the camera go.
	 *
	 * @param position where the camera starts
	 * @param yaw      the direction the camera looks at, in degrees
	 * @param pitch    how far the camera looks up or down, in degrees
	 */
	public static void activate(Vec3d position, float yaw, float pitch) {
		FreeCam.position = position;
		FreeCam.yaw = yaw;
		FreeCam.pitch = pitch;
		FreeCam.active = true;
	}

	/**
	 * Puts the camera back with the player.
	 */
	public static void deactivate() {
		active = false;
		sneaking = false;
	}

	/**
	 * Moves the camera.
	 *
	 * @param position where the camera is now
	 */
	public static void moveTo(Vec3d position) {
		FreeCam.position = position;
	}

	/**
	 * Turns the camera, the way the game turns the player.
	 *
	 * @param deltaX how far the mouse was moved sideways
	 * @param deltaY how far the mouse was moved up or down
	 */
	public static void look(double deltaX, double deltaY) {
		yaw += (float) deltaX * TURN;
		pitch = MathHelper.clamp(pitch + (float) deltaY * TURN, -90.0F, 90.0F);
	}
}

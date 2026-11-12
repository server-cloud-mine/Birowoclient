package com.birowo.client.platform.render;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/**
 * The direction the camera looks at, and the place a line that starts in the middle of the screen
 * has to begin at.
 *
 * <p>The world is drawn relative to the camera, so the camera itself is the origin of that space
 * and the middle of the screen is the origin of the picture. A line that is drawn from the origin
 * towards a block would begin exactly at the eye of the player, and the eye is the one point of
 * the world that the projection cannot divide: the line would vanish.
 *
 * <p>The line is therefore begun a little in front of the camera, on the way to what it points
 * at. A point on that line is still seen in the middle of the screen, but it is in front of the
 * camera and can be drawn.
 */
public final class CameraView {
	/**
	 * How far in front of the camera a line begins, in blocks.
	 */
	private static final double START_DISTANCE = 0.5D;

	private CameraView() {
	}

	/**
	 * The direction the camera looks at, as a vector of the world.
	 *
	 * <p>The direction is worked out from the angles of the camera rather than from its
	 * quaternion, because the angles are the very same ones the game itself uses to work out
	 * where it is looking, and because they are the angles of the free camera as well.
	 *
	 * @param camera the camera of the client
	 * @return the direction the camera looks at, as a vector of the world
	 */
	public static Vec3d forward(Camera camera) {
		double yaw = Math.toRadians(camera.getYaw());
		double pitch = Math.toRadians(camera.getPitch());
		double cosPitch = Math.cos(pitch);

		return new Vec3d(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
	}

	/**
	 * The place a line towards the given point begins at.
	 *
	 * @param camera the camera of the client
	 * @param target the point the line points at, in the world
	 * @return the place the line begins at, in the world, or {@code null} when the point is behind
	 *         the camera and cannot be reached by a line
	 */
	public static Vec3d start(Camera camera, Vec3d target) {
		Vec3d eye = camera.getCameraPos();
		Vec3d forward = forward(camera);
		double depth = target.subtract(eye).dotProduct(forward);

		if (depth <= 0.0D) {
			// The point is behind the camera, so there is no line to it.
			return null;
		}

		double distance = Math.min(START_DISTANCE, depth * 0.5D);

		return eye.add(forward.multiply(distance));
	}
}

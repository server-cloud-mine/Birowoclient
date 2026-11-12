package com.birowo.client.platform.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import com.birowo.client.core.util.ScreenProjection;

/**
 * Asks the game where a point of the world is on the screen.
 *
 * <p>The game projects a point of the world with the very matrix it draws the world with, so
 * the answer is the one that belongs to the frame that is being drawn: the field of view, the
 * aspect and the place of the camera are the ones of that frame.
 */
public final class ViewProjection {
	private ViewProjection() {
	}

	/**
	 * Places a point of the world on the screen.
	 *
	 * @param client the client the frame belongs to
	 * @param target the point of the world that is placed on the screen
	 * @param width  the width of the screen, in pixels
	 * @param height the height of the screen, in pixels
	 * @return where the point is on the screen, or {@code null} when it is behind the camera
	 */
	public static ScreenProjection.ScreenPoint project(MinecraftClient client, Vec3d target, int width, int height) {
		Camera camera = client.gameRenderer.getCamera();
		Vec3d position = camera.getCameraPos();

		double dx = target.x - position.x;
		double dy = target.y - position.y;
		double dz = target.z - position.z;

		// A point behind the camera has no place on the screen: the game would draw it mirrored.
		if (ScreenProjection.depth(dx, dy, dz, camera.getYaw(), camera.getPitch()) <= 0.0D) {
			return null;
		}

		Vec3d normalised = client.gameRenderer.project(target);

		return ScreenProjection.fromNormalised(normalised.x, normalised.y, width, height);
	}
}

package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import com.birowo.client.platform.camera.FreeCam;

/**
 * Lets the camera leave the player.
 *
 * <p>The camera of the game is put where the player is every frame. When the camera is free, the
 * place and the direction of the free camera are written over that, so that the world is looked at
 * from there.
 *
 * <p>The two methods that move the camera are protected, so that the game alone may move it. The
 * access widener of the client opens them for the mixin.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "update", at = @At("RETURN"), require = 1, expect = 1)
	private void birowo$applyFreeCam(CallbackInfo info) {
		if (!FreeCam.active()) {
			return;
		}

		Camera camera = (Camera) (Object) this;
		Vec3d position = FreeCam.position();

		camera.setPos(position.x, position.y, position.z);
		camera.setRotation(FreeCam.yaw(), FreeCam.pitch());
	}
}

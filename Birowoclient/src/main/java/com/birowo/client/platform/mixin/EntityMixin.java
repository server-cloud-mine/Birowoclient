package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import com.birowo.client.platform.camera.FreeCam;

/**
 * Turns the free camera instead of the player.
 *
 * <p>The mouse turns the player by asking the entity to change its direction. While the camera is
 * free, the player must not turn: the server would see the player look around, and the body of the
 * player would turn with the camera. The request is therefore taken away from the player and given
 * to the free camera, which turns in exactly the same way.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 1, expect = 1)
	private void birowo$turnFreeCam(double cursorDeltaX, double cursorDeltaY, CallbackInfo info) {
		if (!FreeCam.active()) {
			return;
		}

		if (!((Object) this instanceof ClientPlayerEntity)) {
			return;
		}

		FreeCam.look(cursorDeltaX, cursorDeltaY);
		info.cancel();
	}
}

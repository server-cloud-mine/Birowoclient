package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

import com.birowo.client.platform.camera.FreeCam;

/**
 * Lets the wheel of the mouse change how fast the free camera travels.
 *
 * <p>The wheel of the mouse belongs to the game while a screen is open, and to the free camera
 * while the camera is free: the wheel then changes the speed of the camera, and the game does not
 * hear of it at all.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6).
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 1, expect = 1)
	private void birowo$changeFreeCamSpeed(long window, double horizontal, double vertical, CallbackInfo info) {
		if (!FreeCam.active() || vertical == 0.0D) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		if (client == null || client.currentScreen != null) {
			return;
		}

		FreeCam.adjustSpeed(vertical > 0.0D ? 1 : -1);
		info.cancel();
	}
}

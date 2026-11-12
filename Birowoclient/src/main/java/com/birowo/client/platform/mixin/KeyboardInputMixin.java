package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

import com.birowo.client.platform.camera.FreeCam;

/**
 * Keeps the player where the player is.
 *
 * <p>The keys of the movement are read every tick and turned into the movement of the player. While
 * the camera is free, the movement of the player is thrown away: the keys move the camera instead.
 *
 * <p>A player that was sneaking keeps sneaking, so that it does not fall off a ledge while the
 * camera is somewhere else.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6).
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	/**
	 * The keys that are held down when the player keeps sneaking.
	 */
	private static final PlayerInput SNEAKING = new PlayerInput(false, false, false, false, false, true, false);

	@Inject(method = "tick", at = @At("RETURN"), require = 1, expect = 1)
	private void birowo$blockMovement(CallbackInfo info) {
		if (!FreeCam.active()) {
			return;
		}

		InputAccessor accessor = (InputAccessor) this;

		accessor.birowo$setPlayerInput(FreeCam.sneaking() ? SNEAKING : PlayerInput.DEFAULT);
		accessor.birowo$setMovementVector(Vec2f.ZERO);
	}
}

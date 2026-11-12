package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

/**
 * Gives the client access to the movement the player has asked for.
 *
 * <p>The movement of the player is read from two fields of the input of the player, and both of
 * them live in the class the input of the keyboard builds on. They are reached through accessors
 * rather than through shadows, because a shadow cannot name a field of a class above the one the
 * mixin is written for.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6).
 */
@Mixin(Input.class)
public interface InputAccessor {
	/**
	 * Replaces the keys the player is holding down.
	 *
	 * @param input the keys that are held down
	 */
	@Accessor("playerInput")
	void birowo$setPlayerInput(PlayerInput input);

	/**
	 * Replaces the direction the player is moving in.
	 *
	 * @param vector the direction the player is moving in
	 */
	@Accessor("movementVector")
	void birowo$setMovementVector(Vec2f vector);
}

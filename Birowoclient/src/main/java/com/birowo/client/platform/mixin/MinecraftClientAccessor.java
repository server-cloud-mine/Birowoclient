package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.MinecraftClient;

/**
 * Gives the client access to the attack the game itself performs.
 *
 * <p>The attack of the game is a private method, and it is the one that knows about the cooldown
 * of the attack, about the charge a weapon needs and about the way a sweep is made. A feature
 * that wants to attack the way a player does has to go through it rather than through the network
 * handler alone.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6).
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
	/**
	 * Performs the attack of the game, as if the player had pressed the attack button.
	 *
	 * @return whether the attack was made
	 */
	@Invoker("doAttack")
	boolean birowo$doAttack();
}

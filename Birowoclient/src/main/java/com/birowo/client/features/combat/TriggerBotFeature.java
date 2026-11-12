package com.birowo.client.features.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.platform.mixin.MinecraftClientAccessor;

/**
 * Attacks what the player is looking at, without the player having to click.
 *
 * <p>The attack is the one of the game: the same method that runs when the attack button is
 * pressed. That method knows about the cooldown of the attack, about the charge a weapon needs
 * and about the way a sweep is made, so the server sees an attack that is the same as any other.
 *
 * <p>The feature waits for a moment before it attacks, so that the attack is not sent in the very
 * tick the target comes into the crosshair.
 */
public final class TriggerBotFeature extends Feature {
	/**
	 * The smallest number of ticks between two attacks.
	 */
	private static final int MINIMUM_DELAY = 1;

	/**
	 * The greatest distance at which a target is attacked, in blocks.
	 */
	private static final int MAXIMUM_RANGE = 6;

	private final ConfigValue<Boolean> playersOnly = setting(ConfigValues.ofBoolean("players_only", true));

	private final ConfigValue<Boolean> cooldown = setting(ConfigValues.ofBoolean("cooldown", true));

	private final ConfigValue<Integer> charge = setting(ConfigValues.ofInt("charge", 100));

	private final ConfigValue<Integer> delay = setting(ConfigValues.ofInt("delay", 0));

	private final ConfigValue<Integer> range = setting(ConfigValues.ofInt("range", 4));

	/**
	 * How many ticks are left before the next attack.
	 */
	private int waiting;

	public TriggerBotFeature() {
		super("triggerbot", "TriggerBot", "Attacks the target the player is looking at.",
				FeatureCategory.COMBAT);
	}

	@Override
	public void onTick() {
		waiting = Math.max(0, waiting - 1);

		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.world == null || client.interactionManager == null) {
			return;
		}

		// The interface and the screens of the game take the mouse away, so nothing is attacked
		// while one of them is open.
		if (client.currentScreen != null) {
			return;
		}

		HitResult hit = client.crosshairTarget;

		if (!(hit instanceof EntityHitResult entityHit)) {
			return;
		}

		Entity target = entityHit.getEntity();

		if (target == client.player || !target.isAlive()) {
			return;
		}

		if (target instanceof PlayerEntity player) {
			if (player.isCreative() || player.isSpectator()) {
				return;
			}
		} else if (playersOnly.value()) {
			return;
		}

		double reach = Math.min(range.value(), MAXIMUM_RANGE);

		if (client.player.squaredDistanceTo(target) > reach * reach) {
			return;
		}

		// The weapon of the player has a cooldown of its own: a sword that is swung as fast as it
		// can be only deals its full damage every few ticks. The attack waits for it, so that the
		// player never attacks faster than they could by hand.
		if (cooldown.value() && !charged(client)) {
			return;
		}

		if (waiting > 0) {
			return;
		}

		waiting = Math.max(MINIMUM_DELAY, delay.value()) - 1;

		attack(client, target);
	}

	/**
	 * Whether the weapon of the player is charged enough to be swung.
	 *
	 * <p>The game counts how many ticks have passed since the last attack and divides them by
	 * the time the weapon needs to be charged. A weapon that is fully charged deals its full
	 * damage; one that is not deals less, and the amount it deals grows with every tick.
	 *
	 * @param client the client the player belongs to
	 * @return whether the weapon is charged enough
	 */
	private boolean charged(MinecraftClient client) {
		int needed = Math.max(0, Math.min(100, charge.value()));
		float progress = client.player.getAttackCooldownProgress(0.0F);

		return progress >= needed / 100.0F;
	}

	/**
	 * Attacks the target the way the game does.
	 *
	 * <p>The attack of the game is private, so it is reached through the accessor of the client.
	 * If that ever fails, the attack is made by hand instead, which sends the same packet but
	 * leaves out the checks of the game.
	 *
	 * @param client the client the player belongs to
	 * @param target the entity that is attacked
	 */
	private void attack(MinecraftClient client, Entity target) {
		try {
			((MinecraftClientAccessor) client).birowo$doAttack();
		} catch (Throwable t) {
			client.interactionManager.attackEntity(client.player, target);
			client.player.swingHand(Hand.MAIN_HAND);
		}
	}
}

package com.birowo.client.features.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;

/**
 * Runs without the sprint key having to be held down.
 *
 * <p>The player starts sprinting as soon as they move forward, and stops when they stop. The
 * sprint is set on the player itself, so the server is told about it in the ordinary way.
 */
public final class SprintFeature extends Feature {
	/**
	 * Whether the player also sprints sideways and backwards.
	 */
	private final ConfigValue<Boolean> anyDirection = setting(ConfigValues.ofBoolean("any_direction", false));

	public SprintFeature() {
		super("sprint", "AutoSprint", "Sprints without the sprint key having to be held.",
				FeatureCategory.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;

		if (player == null || player.isSneaking() || player.isSpectator()) {
			return;
		}

		boolean forward = client.options.forwardKey.isPressed();
		boolean moving = forward
				|| client.options.leftKey.isPressed()
				|| client.options.rightKey.isPressed()
				|| client.options.backKey.isPressed();

		// Without the setting the player only sprints forwards, which is what the game does.
		if (!moving || (!forward && !anyDirection.value())) {
			return;
		}

		player.setSprinting(true);
	}
}

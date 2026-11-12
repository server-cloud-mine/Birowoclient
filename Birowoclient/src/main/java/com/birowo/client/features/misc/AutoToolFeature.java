package com.birowo.client.features.misc;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.ConfigValues;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;

/**
 * Puts the right tool into the hand before the player starts to mine.
 *
 * <p>The tool is chosen by how fast it breaks the block the player is looking at. The slot is
 * changed the way the game changes it when a number key is pressed, so that the server is told
 * about it in the ordinary way.
 */
public final class AutoToolFeature extends Feature {
	/**
	 * Whether the tool is only put into the hand when the player starts to mine.
	 */
	private final ConfigValue<Boolean> onlyWhenMining = setting(ConfigValues.ofBoolean("only_when_mining", false));

	public AutoToolFeature() {
		super("autotool", "AutoTool", "Puts the right tool into the hand.",
				FeatureCategory.MISC);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;

		if (player == null || world == null || client.currentScreen != null) {
			return;
		}

		HitResult hit = client.crosshairTarget;

		if (!(hit instanceof BlockHitResult blockHit)) {
			return;
		}

		if (onlyWhenMining.value() && !client.options.attackKey.isPressed()) {
			return;
		}

		BlockPos pos = blockHit.getBlockPos();
		BlockState state = world.getBlockState(pos);

		if (state.isAir()) {
			return;
		}

		PlayerInventory inventory = player.getInventory();
		int held = inventory.getSelectedSlot();
		int best = held;
		float bestSpeed = speed(inventory.getStack(held), state);

		for (int slot = 0; slot < PlayerInventory.HOTBAR_SIZE; slot++) {
			ItemStack stack = inventory.getStack(slot);

			if (stack.isEmpty()) {
				continue;
			}

			float speed = speed(stack, state);

			if (speed > bestSpeed) {
				bestSpeed = speed;
				best = slot;
			}
		}

		// A speed of one is the speed of a bare hand: nothing in the hotbar is any better.
		if (bestSpeed <= 1.0F || best == held) {
			return;
		}

		inventory.setSelectedSlot(best);
	}

	/**
	 * @param stack the item in the slot
	 * @param state the block that is looked at
	 * @return how fast the item breaks that block
	 */
	private static float speed(ItemStack stack, BlockState state) {
		if (stack.isEmpty()) {
			return 1.0F;
		}

		return stack.getMiningSpeedMultiplier(state);
	}
}

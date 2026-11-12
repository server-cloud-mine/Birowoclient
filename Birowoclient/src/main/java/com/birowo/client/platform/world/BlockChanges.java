package com.birowo.client.platform.world;

import java.util.function.BiConsumer;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Tells the client that a block of the world has changed.
 *
 * <p>The game does not say anything when a block is broken or placed; it only changes the block
 * and goes on. The overlay of the blocks has to know about it all the same, so that a block that
 * is mined disappears at once and a block that is placed appears at once.
 *
 * <p>The telling is done through a hook rather than through the features themselves, because the
 * change is seen in a mixin, which is not allowed to know about the features.
 */
public final class BlockChanges {
	/**
	 * What is told when a block changes; nothing is told when there is none.
	 */
	private static BiConsumer<BlockPos, BlockState> listener;

	private BlockChanges() {
	}

	/**
	 * Asks to be told when a block changes.
	 *
	 * @param listener what is told, or {@code null} to be told nothing
	 */
	public static void listen(BiConsumer<BlockPos, BlockState> listener) {
		BlockChanges.listener = listener;
	}

	/**
	 * Says that a block has changed.
	 *
	 * @param pos   the place of the block
	 * @param state the state the block has now
	 */
	public static void changed(BlockPos pos, BlockState state) {
		BiConsumer<BlockPos, BlockState> current = listener;

		if (current == null) {
			return;
		}

		current.accept(pos, state);
	}
}

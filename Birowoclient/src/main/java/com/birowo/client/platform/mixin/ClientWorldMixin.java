package com.birowo.client.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import com.birowo.client.platform.world.BlockChanges;

/**
 * Says that a block of the world has changed.
 *
 * <p>The game changes a block without saying anything about it. The overlay of the blocks has to
 * know about it all the same, so that a block that is mined disappears at once.
 *
 * <p>The state the block had before the change is remembered when the change begins, so that the
 * old state can be told together with the new one. A block that is written again with the state
 * it already had is not a change, and is not told.
 *
 * <p>The method of the world returns whether the block was really changed, so the change is only
 * told when it was. That is why the callback holds the value the method returns.
 *
 * <p>Verified against Minecraft 1.21.11 (Yarn 1.21.11+build.6):
 * {@code public boolean setBlockState(BlockPos, BlockState, int, int)}.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	/**
	 * The state the block had before the change began.
	 */
	@Unique
	private BlockState birowo$oldState;

	@Inject(method = "setBlockState", at = @At("HEAD"), require = 1, expect = 1)
	private void birowo$rememberState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> info) {
		this.birowo$oldState = ((ClientWorld) (Object) this).getBlockState(pos);
	}

	@Inject(method = "setBlockState", at = @At("RETURN"), require = 1, expect = 1)
	private void birowo$reportChange(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> info) {
		// The world says that nothing was changed, so nothing has to be told.
		if (!info.getReturnValueZ()) {
			return;
		}

		// A block that was written again with the state it already had is not a change either.
		BlockState old = this.birowo$oldState;

		if (old != null && old.equals(state)) {
			return;
		}

		BlockChanges.changed(pos, state);
	}
}

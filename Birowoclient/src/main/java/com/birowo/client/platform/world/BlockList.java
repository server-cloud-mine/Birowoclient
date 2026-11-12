package com.birowo.client.platform.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import com.birowo.client.core.config.BlockSelection;

/**
 * Turns the identifiers of blocks into the blocks themselves, and back.
 *
 * <p>The identifiers are what the configuration file holds, so that a block list survives a
 * version of the game that does not have those blocks. A block that is not in the registry is
 * skipped rather than being an error, so that a list written by another version never costs the
 * player the rest of it.
 */
public final class BlockList {
	private BlockList() {
	}

	/**
	 * Resolves the identifiers into blocks.
	 *
	 * @param identifiers the identifiers of the blocks
	 * @return the blocks that are in the registry of the game
	 */
	public static Set<Block> resolve(Collection<String> identifiers) {
		Set<Block> blocks = new LinkedHashSet<>();

		for (String identifier : identifiers) {
			Identifier parsed = Identifier.tryParse(identifier);

			if (parsed == null) {
				continue;
			}

			Registries.BLOCK.getOptionalValue(parsed).ifPresent(blocks::add);
		}

		return Set.copyOf(blocks);
	}

	/**
	 * Resolves the identifiers into blocks, together with the way each block is shown.
	 *
	 * @param selections the blocks that are chosen and the way each of them is shown
	 * @return the blocks that are in the registry of the game, by the way they are shown
	 */
	public static Map<Block, BlockSelection> resolveSelections(Map<String, BlockSelection> selections) {
		Map<Block, BlockSelection> blocks = new LinkedHashMap<>();

		for (Map.Entry<String, BlockSelection> entry : selections.entrySet()) {
			Identifier parsed = Identifier.tryParse(entry.getKey());

			if (parsed == null) {
				continue;
			}

			Registries.BLOCK.getOptionalValue(parsed).ifPresent(block -> blocks.put(block, entry.getValue()));
		}

		return blocks;
	}

	/**
	 * Turns the blocks into their identifiers.
	 *
	 * @param blocks the blocks
	 * @return the identifiers of the blocks
	 */
	public static List<String> identifiersOf(Collection<Block> blocks) {
		List<String> identifiers = new ArrayList<>();

		for (Block block : blocks) {
			Identifier identifier = Registries.BLOCK.getId(block);

			if (identifier != null) {
				identifiers.add(identifier.toString());
			}
		}

		return identifiers;
	}

	/**
	 * @return the identifiers of every block of the game, in alphabetical order
	 */
	public static List<Identifier> allIdentifiers() {
		List<Identifier> identifiers = new ArrayList<>();

		for (Block block : Registries.BLOCK) {
			Identifier identifier = Registries.BLOCK.getId(block);

			if (identifier != null) {
				identifiers.add(identifier);
			}
		}

		identifiers.sort((left, right) -> left.toString().compareTo(right.toString()));

		return identifiers;
	}

	/**
	 * @param identifier the identifier of a block
	 * @return the name of the block as the game shows it, or the identifier itself when the block
	 *         is not in the registry
	 */
	public static String nameOf(Identifier identifier) {
		return Registries.BLOCK.getOptionalValue(identifier)
				.map(block -> block.getName().getString())
				.orElse(identifier.toString());
	}
}

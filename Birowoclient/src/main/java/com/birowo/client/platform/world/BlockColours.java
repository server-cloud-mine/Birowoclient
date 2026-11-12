package com.birowo.client.platform.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The colours the blocks of the overlay are shown in.
 *
 * <p>Every block that is worth looking for has a colour of its own, so that the blocks of one
 * list can be told apart at a glance: a chest is orange, a diamond ore is the colour of a
 * diamond, and the shulker boxes keep the colours they are named after.
 *
 * <p>The colours are the ones the game itself uses for its dyes, so that a colour chosen here
 * looks the same as the colour of the thing it stands for.
 */
public final class BlockColours {
	/**
	 * The colour of a block that has no colour of its own.
	 */
	public static final int DEFAULT = 0x4D9BFF;

	/**
	 * The colours of the dyes of the game, by the name of the dye.
	 */
	private static final Map<String, Integer> DYES = buildDyes();

	/**
	 * The colour every known block has by default, by identifier.
	 */
	private static final Map<String, Integer> DEFAULTS = buildDefaults();

	private BlockColours() {
	}

	/**
	 * @return the colour every known block has by default, by identifier
	 */
	public static Map<String, Integer> defaults() {
		return DEFAULTS;
	}

	/**
	 * @return the colours of the dyes of the game, by the name of the dye
	 */
	public static Map<String, Integer> dyeColours() {
		return DYES;
	}

	/**
	 * @param identifier the identifier of a block
	 * @return the colour the block has by default
	 */
	public static int colourOf(String identifier) {
		Integer known = DEFAULTS.get(identifier);

		if (known != null) {
			return known;
		}

		// The shulker boxes are named after the dye they are of.
		if (identifier.endsWith("_shulker_box")) {
			String name = identifier.substring(identifier.lastIndexOf(':') + 1,
					identifier.length() - "_shulker_box".length());
			Integer dye = DYES.get(name);

			if (dye != null) {
				return dye;
			}
		}

		return DEFAULT;
	}

	/**
	 * @return the colours of the dyes of the game, by the name of the dye
	 */
	private static Map<String, Integer> buildDyes() {
		Map<String, Integer> dyes = new LinkedHashMap<>();

		dyes.put("white", 0xF9FFFE);
		dyes.put("orange", 0xF9801D);
		dyes.put("magenta", 0xC74EBD);
		dyes.put("light_blue", 0x3AB3DA);
		dyes.put("yellow", 0xFED83D);
		dyes.put("lime", 0x80C71F);
		dyes.put("pink", 0xF38BAA);
		dyes.put("gray", 0x474F52);
		dyes.put("light_gray", 0x9D9D97);
		dyes.put("cyan", 0x169C9C);
		dyes.put("purple", 0x8932B8);
		dyes.put("blue", 0x3C44AA);
		dyes.put("brown", 0x835432);
		dyes.put("green", 0x5E7C16);
		dyes.put("red", 0xB02E26);
		dyes.put("black", 0x1D1D21);

		return Collections.unmodifiableMap(dyes);
	}

	/**
	 * @return the colour every known block has by default, by identifier
	 */
	private static Map<String, Integer> buildDefaults() {
		Map<String, Integer> defaults = new LinkedHashMap<>();

		// The ores are shown in the colour of the metal they hold.
		defaults.put("minecraft:diamond_ore", 0x5DECF5);
		defaults.put("minecraft:deepslate_diamond_ore", 0x5DECF5);
		defaults.put("minecraft:gold_ore", 0xFED83D);
		defaults.put("minecraft:deepslate_gold_ore", 0xFED83D);
		defaults.put("minecraft:nether_gold_ore", 0xFED83D);
		defaults.put("minecraft:iron_ore", 0xD8D8D8);
		defaults.put("minecraft:deepslate_iron_ore", 0xD8D8D8);
		defaults.put("minecraft:copper_ore", 0xD08050);
		defaults.put("minecraft:deepslate_copper_ore", 0xD08050);
		defaults.put("minecraft:emerald_ore", 0x17DD62);
		defaults.put("minecraft:deepslate_emerald_ore", 0x17DD62);
		defaults.put("minecraft:redstone_ore", 0xE02B26);
		defaults.put("minecraft:deepslate_redstone_ore", 0xE02B26);
		defaults.put("minecraft:lapis_ore", 0x3C44AA);
		defaults.put("minecraft:deepslate_lapis_ore", 0x3C44AA);
		defaults.put("minecraft:coal_ore", 0x1D1D21);
		defaults.put("minecraft:deepslate_coal_ore", 0x1D1D21);
		defaults.put("minecraft:nether_quartz_ore", 0xE8E4D8);
		defaults.put("minecraft:ancient_debris", 0x6A4A38);

		// The blocks that are worth carrying away.
		defaults.put("minecraft:chest", 0xF9801D);
		defaults.put("minecraft:trapped_chest", 0xC46A16);
		defaults.put("minecraft:ender_chest", 0x28392C);
		defaults.put("minecraft:barrel", 0x835432);
		defaults.put("minecraft:shulker_box", 0x9D9D97);
		defaults.put("minecraft:hopper", 0x474F52);
		defaults.put("minecraft:dropper", 0x6E7C8C);
		defaults.put("minecraft:dispenser", 0x8C8C8C);
		defaults.put("minecraft:brewing_stand", 0xC08A5B);
		defaults.put("minecraft:furnace", 0x6E6E6E);
		defaults.put("minecraft:blast_furnace", 0x3A3A3A);
		defaults.put("minecraft:smoker", 0x6B4A2F);
		defaults.put("minecraft:spawner", 0x2E5B8C);
		defaults.put("minecraft:enchanting_table", 0x8932B8);
		defaults.put("minecraft:anvil", 0x474F52);
		defaults.put("minecraft:chipped_anvil", 0x474F52);
		defaults.put("minecraft:damaged_anvil", 0x474F52);
		defaults.put("minecraft:beacon", 0x169C9C);
		defaults.put("minecraft:end_portal_frame", 0x2E6E4E);
		defaults.put("minecraft:diamond_block", 0x5DECF5);
		defaults.put("minecraft:gold_block", 0xFED83D);
		defaults.put("minecraft:iron_block", 0xD8D8D8);
		defaults.put("minecraft:emerald_block", 0x17DD62);
		defaults.put("minecraft:netherite_block", 0x4A4245);
		defaults.put("minecraft:lapis_block", 0x3C44AA);
		defaults.put("minecraft:redstone_block", 0xE02B26);
		defaults.put("minecraft:coal_block", 0x1D1D21);

		// The blocks that are rare and worth a lot.
		defaults.put("minecraft:dragon_egg", 0x8932B8);

		return Collections.unmodifiableMap(defaults);
	}
}

package com.birowo.client.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A setting that holds a list of blocks, each with a colour of its own.
 *
 * <p>The list is written to the configuration file as an array of identifiers and colours, so that
 * a block list survives a version that does not know the blocks themselves.
 */
public final class BlockListValue extends ConfigValue<List<BlockSelection>> implements BlockListSetting {
	/**
	 * The colour of a block that has no colour of its own.
	 */
	private static final int FALLBACK = 0x4D9BFF;

	/**
	 * @param key          the config key
	 * @param defaultValue the blocks that are chosen when the key is absent
	 * @param defaults     the colour every block has by default, by identifier
	 */
	BlockListValue(String key, List<String> defaultValue, Map<String, Integer> defaults) {
		super(key, toSelections(defaultValue, defaults), new BlockSelectionAdapter(defaults));

		format(blocks -> blocks.size() + (blocks.size() == 1 ? " block" : " blocks"));
	}

	@Override
	public List<BlockSelection> selections() {
		return value();
	}

	@Override
	public void selections(List<BlockSelection> selections) {
		set(List.copyOf(selections));
	}

	/**
	 * Turns a list of identifiers into a list of blocks with the colours they have by default.
	 *
	 * @param identifiers the identifiers of the blocks
	 * @param defaults    the colour every block has by default, by identifier
	 * @return the blocks and their colours
	 */
	private static List<BlockSelection> toSelections(List<String> identifiers, Map<String, Integer> defaults) {
		List<BlockSelection> selections = new ArrayList<>();

		for (String identifier : identifiers) {
			selections.add(new BlockSelection(identifier, defaults.getOrDefault(identifier, FALLBACK)));
		}

		return List.copyOf(selections);
	}
}

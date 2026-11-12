package com.birowo.client.core.config;

import java.util.List;

/**
 * A setting whose value is a list of blocks.
 *
 * <p>The blocks are kept as their identifiers, so that the core never has to know what a block
 * is. The platform resolves them, and the platform is also where the list is edited: a list of
 * blocks is far too long to be changed one value at a time, so it is picked in a screen of its
 * own.
 *
 * <p>Every block carries a colour of its own, so that the blocks of one list can be told apart at
 * a glance.
 */
public interface BlockListSetting {
	/**
	 * @return the blocks that are chosen, in the order they were chosen
	 */
	List<BlockSelection> selections();

	/**
	 * Replaces the blocks that are chosen.
	 *
	 * @param selections the new blocks
	 */
	void selections(List<BlockSelection> selections);

	/**
	 * @return the identifiers of the blocks that are chosen, in the order they were chosen
	 */
	default List<String> blocks() {
		return selections().stream().map(BlockSelection::identifier).toList();
	}
}

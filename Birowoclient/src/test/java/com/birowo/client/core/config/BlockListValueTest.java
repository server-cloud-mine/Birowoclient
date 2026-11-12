package com.birowo.client.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Tests of the setting that holds a list of blocks.
 */
class BlockListValueTest {
	@Test
	void keepsTheBlocksItWasGiven() {
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of("minecraft:chest", "minecraft:iron_ore"));

		assertEquals(List.of("minecraft:chest", "minecraft:iron_ore"), value.blocks());
	}

	@Test
	void showsHowManyBlocksAreChosen() {
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of("minecraft:chest"));

		assertEquals("1 block", value.display());

		value.selections(List.of(new BlockSelection("minecraft:chest", 0xF9801D),
				new BlockSelection("minecraft:iron_ore", 0xD8D8D8)));

		assertEquals("2 blocks", value.display());
	}

	@Test
	void givesEveryBlockTheColourItHasByDefault() {
		Map<String, Integer> defaults = Map.of("minecraft:chest", 0xF9801D);
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of("minecraft:chest", "minecraft:stone"),
				defaults);

		assertEquals(List.of(new BlockSelection("minecraft:chest", 0xF9801D),
				new BlockSelection("minecraft:stone", 0x4D9BFF)), value.selections());
	}

	@Test
	void survivesTheConfigFile() {
		BlockListValue written = ConfigValues.ofBlockList("blocks", List.of());
		JsonObject target = new JsonObject();

		written.selections(List.of(new BlockSelection("minecraft:chest", 0xF9801D),
				new BlockSelection("minecraft:gold_ore", 0xFED83D)));
		written.write(target);

		BlockListValue read = ConfigValues.ofBlockList("blocks", List.of());

		assertTrue(read.read(target.get("blocks")));
		assertEquals(List.of(new BlockSelection("minecraft:chest", 0xF9801D),
				new BlockSelection("minecraft:gold_ore", 0xFED83D)), read.selections());
	}

	@Test
	void readsAListThatWasWrittenBeforeTheBlocksHadColours() {
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of(),
				Map.of("minecraft:chest", 0xF9801D));
		JsonArray array = new JsonArray();

		array.add(new JsonPrimitive("minecraft:chest"));
		array.add(new JsonPrimitive("minecraft:stone"));

		assertTrue(value.read(array));
		assertEquals(List.of(new BlockSelection("minecraft:chest", 0xF9801D),
				new BlockSelection("minecraft:stone", 0x4D9BFF)), value.selections());
	}

	@Test
	void keepsTheDefaultWhenTheFileIsBroken() {
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of("minecraft:chest"));
		JsonObject broken = new JsonObject();

		broken.add("blocks", new JsonPrimitive(7));

		assertTrue(!value.read(broken.get("blocks")));
		assertEquals(List.of("minecraft:chest"), value.blocks());
	}

	@Test
	void keepsTheWayEveryBlockIsShown() {
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of());
		JsonObject target = new JsonObject();

		value.selections(List.of(new BlockSelection("minecraft:chest", 0xF9801D,
				EspSettings.DEFAULT.withShape(EspShape.SIDES).withTracer(false).withTracerColour(0x112233))));
		value.write(target);

		BlockListValue read = ConfigValues.ofBlockList("blocks", List.of());

		assertTrue(read.read(target.get("blocks")));

		BlockSelection selection = read.selections().get(0);

		assertEquals(EspShape.SIDES, selection.settings().shape());
		assertEquals(Boolean.FALSE, selection.settings().tracer());
		assertEquals(0x112233, selection.settings().tracerColour());
	}

	@Test
	void readsABlockThatHasNoWayOfItsOwn() {
		BlockListValue value = ConfigValues.ofBlockList("blocks", List.of());
		JsonArray array = new JsonArray();
		JsonObject block = new JsonObject();

		block.addProperty("id", "minecraft:chest");
		block.addProperty("colour", "#F9801D");
		array.add(block);

		assertTrue(value.read(array));

		BlockSelection selection = value.selections().get(0);

		assertEquals(0xF9801D, selection.colour());
		assertTrue(selection.settings().isDefault());
	}
}

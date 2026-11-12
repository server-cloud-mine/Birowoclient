package com.birowo.client.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.birowo.client.core.event.EventBus;
import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.core.feature.FeatureRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class ConfigStoreTest {
	/**
	 * A feature with two settings and a state of its own.
	 */
	private static final class SettingFeature extends Feature {
		private final ConfigValue<Boolean> flag = setting(ConfigValues.ofBoolean("flag", false));
		private final ConfigValue<Double> amount = setting(ConfigValues.ofDouble("amount", 1.5));

		private SettingFeature() {
			super("settings", "Settings", "a feature with settings", FeatureCategory.MISC);
		}
	}

	private static SettingFeature featureWith(FeatureRegistry registry) {
		SettingFeature feature = new SettingFeature();
		registry.register(feature);
		return feature;
	}

	@Test
	void theSettingsSurviveARestart(@TempDir Path directory) {
		Path file = directory.resolve("birowo.json");
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		SettingFeature feature = featureWith(registry);

		feature.settings().register(feature.flag);
		feature.flag.set(true);
		feature.amount.set(9.25);

		new ConfigStore(file).save(registry.features());

		FeatureRegistry restored = new FeatureRegistry(new EventBus());
		SettingFeature restoredFeature = featureWith(restored);

		new ConfigStore(file).load(restored.features());

		assertTrue(restoredFeature.flag.value());
		assertEquals(9.25, restoredFeature.amount.value(), 0.0001);
	}

	@Test
	void theStateOfAFeatureSurvivesARestart(@TempDir Path directory) {
		Path file = directory.resolve("birowo.json");
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		SettingFeature feature = featureWith(registry);

		feature.setEnabled(true);

		new ConfigStore(file).save(registry.features());

		FeatureRegistry restored = new FeatureRegistry(new EventBus());
		SettingFeature restoredFeature = featureWith(restored);

		new ConfigStore(file).load(restored.features());

		assertTrue(restoredFeature.enabled());
	}

	@Test
	void aSettingThatIsNotKnownIsKept(@TempDir Path directory) throws IOException {
		Path file = directory.resolve("birowo.json");
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		featureWith(registry);

		Files.writeString(file, """
				{"features":{"settings":{"settings":{"future":42}}}}
				""", StandardCharsets.UTF_8);

		new ConfigStore(file).save(registry.features());

		JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
		JsonObject settings = root.getAsJsonObject("features").getAsJsonObject("settings")
				.getAsJsonObject("settings");

		assertEquals(42, settings.get("future").getAsInt());
	}

	@Test
	void aFileThatCannotBeReadIsBackedUpAndTheDefaultsAreUsed(@TempDir Path directory) throws IOException {
		Path file = directory.resolve("birowo.json");
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		SettingFeature feature = featureWith(registry);

		Files.writeString(file, "this is not a json file", StandardCharsets.UTF_8);

		new ConfigStore(file).load(registry.features());

		assertFalse(feature.flag.value());
		assertEquals(1.5, feature.amount.value(), 0.0001);
		assertTrue(Files.exists(directory.resolve("birowo.json.bak")));
	}

	@Test
	void aFileThatDoesNotExistIsNotAnError(@TempDir Path directory) {
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		SettingFeature feature = featureWith(registry);

		new ConfigStore(directory.resolve("missing.json")).load(registry.features());

		assertFalse(feature.enabled());
	}

	@Test
	void theFeaturesThatAreSavedAreTheOnesThatAreGiven(@TempDir Path directory) {
		Path file = directory.resolve("birowo.json");
		EventBus bus = new EventBus();
		FeatureRegistry registry = new FeatureRegistry(bus);
		featureWith(registry);

		new ConfigStore(file).save(List.of());

		assertFalse(Files.exists(file));
	}
}

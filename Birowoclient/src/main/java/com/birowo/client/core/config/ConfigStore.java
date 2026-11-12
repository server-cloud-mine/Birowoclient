package com.birowo.client.core.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.birowo.client.core.feature.Feature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Reads and writes the client's configuration file.
 *
 * <p>Two properties matter here and are guaranteed by this implementation:
 *
 * <ul>
 *   <li><b>No data loss.</b> Unknown keys — settings belonging to a newer version, or a feature
 *       that has since been removed — are preserved verbatim on write. A user who downgrades and
 *       upgrades again does not lose their configuration.
 *   <li><b>No silent failure.</b> A corrupt file is backed up and reported, then the client
 *       continues with defaults rather than refusing to start.
 * </ul>
 */
public final class ConfigStore {
	private static final Logger LOGGER = LoggerFactory.getLogger("BirowoClient/ConfigStore");

	private static final String FEATURES_KEY = "features";
	private static final String ENABLED_KEY = "enabled";
	private static final String SETTINGS_KEY = "settings";

	private final Path file;
	private final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	/**
	 * @param file the file to read from and write to
	 */
	public ConfigStore(Path file) {
		this.file = file;
	}

	/**
	 * Loads the state of every given feature from disk.
	 *
	 * <p>Settings are applied before the enabled flag, so a feature is never enabled with the
	 * previous run's settings.
	 *
	 * @param features the features to restore
	 */
	public void load(Collection<Feature> features) {
		JsonObject root = read();

		if (root == null) {
			return;
		}

		JsonElement featuresElement = root.get(FEATURES_KEY);

		if (featuresElement == null || !featuresElement.isJsonObject()) {
			return;
		}

		JsonObject stored = featuresElement.getAsJsonObject();

		for (Feature feature : features) {
			if (!feature.persistable()) {
				continue;
			}

			JsonElement featureElement = stored.get(feature.id());

			if (featureElement == null || !featureElement.isJsonObject()) {
				continue;
			}

			JsonObject node = featureElement.getAsJsonObject();
			applySettings(feature, node);
			applyEnabledState(feature, node);
		}
	}

	/**
	 * Writes the state of every given feature to disk.
	 *
	 * <p>Nothing is written when there is no feature to save and no file to keep: a client without
	 * features does not leave a configuration file behind.
	 *
	 * @param features the features to persist
	 */
	public void save(Collection<Feature> features) {
		if (features.isEmpty() && !Files.isRegularFile(file)) {
			return;
		}

		JsonObject root = read();
		JsonObject stored = new JsonObject();
		JsonElement featuresElement = root == null ? null : root.get(FEATURES_KEY);

		if (featuresElement != null && featuresElement.isJsonObject()) {
			stored = featuresElement.getAsJsonObject();
		}

		for (Feature feature : features) {
			if (!feature.persistable()) {
				continue;
			}

			JsonObject node = new JsonObject();
			JsonElement existing = stored.get(feature.id());

			if (existing != null && existing.isJsonObject()) {
				// Preserve settings the current build does not know about.
				node = existing.getAsJsonObject();
			}

			node.addProperty(ENABLED_KEY, feature.enabled());
			writeSettings(feature, node);
			stored.add(feature.id(), node);
		}

		JsonObject output = root == null ? new JsonObject() : root;
		output.add(FEATURES_KEY, stored);

		write(output);
	}

	private void applySettings(Feature feature, JsonObject node) {
		JsonElement settingsElement = node.get(SETTINGS_KEY);

		if (settingsElement == null || !settingsElement.isJsonObject()) {
			return;
		}

		JsonObject settings = settingsElement.getAsJsonObject();

		for (ConfigValue<?> value : feature.settingValues()) {
			JsonElement raw = settings.get(value.key());

			if (raw == null) {
				continue;
			}

			if (!value.read(raw)) {
				LOGGER.warn("Setting {}.{} has an unusable value; keeping the default", feature.id(), value.key());
			}
		}
	}

	private void writeSettings(Feature feature, JsonObject node) {
		JsonObject settings = new JsonObject();
		JsonElement existing = node.get(SETTINGS_KEY);

		if (existing != null && existing.isJsonObject()) {
			settings = existing.getAsJsonObject();
		}

		for (ConfigValue<?> value : feature.settingValues()) {
			value.write(settings);
		}

		node.add(SETTINGS_KEY, settings);
	}

	private void applyEnabledState(Feature feature, JsonObject node) {
		JsonElement enabled = node.get(ENABLED_KEY);

		if (enabled == null || !enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean()) {
			return;
		}

		feature.setEnabled(enabled.getAsBoolean());
	}

	/**
	 * Reads the whole file, or returns {@code null} if it does not exist or cannot be parsed.
	 */
	private JsonObject read() {
		if (!Files.isRegularFile(file)) {
			return null;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);

			if (!parsed.isJsonObject()) {
				backup("the file does not contain a JSON object");
				return null;
			}

			return parsed.getAsJsonObject();
		} catch (JsonParseException e) {
			backup("the file is not valid JSON");
			LOGGER.warn("Config file {} could not be parsed; starting from defaults", file, e);
			return null;
		} catch (IOException e) {
			LOGGER.warn("Config file {} could not be read; starting from defaults", file, e);
			return null;
		}
	}

	private void write(JsonObject root) {
		try {
			Path parent = file.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			Path temporary = file.resolveSibling(file.getFileName() + ".tmp");

			try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				gson.toJson(root, writer);
			}

			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("Config file {} could not be written", file, e);
		}
	}

	private void backup(String reason) {
		Path backup = file.resolveSibling(file.getFileName() + ".bak");

		try {
			Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.warn("Config file {} was backed up to {} because {}", file, backup, reason);
		} catch (IOException e) {
			LOGGER.error("Config file {} is unusable and could not be backed up", file, e);
		}
	}
}

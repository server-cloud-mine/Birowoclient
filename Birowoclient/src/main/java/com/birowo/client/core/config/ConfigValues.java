package com.birowo.client.core.config;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/**
 * Factory methods for the setting types a feature can declare.
 *
 * <p>The set of types is deliberately small and fixed: a feature setting is a boolean, a number, a
 * string or an enum choice. Anything richer belongs in feature state, not in the config file.
 */
public final class ConfigValues {
	private static final Gson GSON = new Gson();

	private ConfigValues() {
	}

	/**
	 * @param key          the config key
	 * @param defaultValue the value used when the key is absent
	 * @return a boolean setting
	 */
	public static ConfigValue<Boolean> ofBoolean(String key, boolean defaultValue) {
		return new ConfigValue<>(key, defaultValue, GSON.getAdapter(Boolean.class));
	}

	/**
	 * @param key          the config key
	 * @param defaultValue the value used when the key is absent
	 * @return an integer setting
	 */
	public static ConfigValue<Integer> ofInt(String key, int defaultValue) {
		return new ConfigValue<>(key, defaultValue, GSON.getAdapter(Integer.class));
	}

	/**
	 * A colour setting.
	 *
	 * <p>The colour is stored as an integer with an alpha channel, but it is shown as the six
	 * hexadecimal digits a person can read.
	 *
	 * @param key          the config key
	 * @param defaultValue the colour used when the key is absent, as an ARGB value
	 * @return a colour setting
	 */
	public static ConfigValue<Integer> ofColor(String key, int defaultValue) {
		ConfigValue<Integer> value = new ConfigValue<>(key, defaultValue, GSON.getAdapter(Integer.class));
		value.format(color -> String.format("#%06X", color & 0xFFFFFF));
		return value;
	}

	/**
	 * @param key          the config key
	 * @param defaultValue the value used when the key is absent
	 * @return a floating-point setting
	 */
	public static ConfigValue<Double> ofDouble(String key, double defaultValue) {
		return new ConfigValue<>(key, defaultValue, GSON.getAdapter(Double.class));
	}

	/**
	 * A list of blocks.
	 *
	 * <p>The blocks are stored as their identifiers, so that the list survives a version of the
	 * game that does not have them. A list this long is not changed through the settings panel
	 * but in a picker of its own.
	 *
	 * @param key          the config key
	 * @param defaultValue the identifiers of the blocks that are chosen when the key is absent
	 * @return a list of blocks
	 */
	public static BlockListValue ofBlockList(String key, List<String> defaultValue) {
		return ofBlockList(key, defaultValue, Map.of());
	}

	/**
	 * A list of blocks, every block with a colour of its own.
	 *
	 * @param key          the config key
	 * @param defaultValue the identifiers of the blocks that are chosen when the key is absent
	 * @param defaults     the colour every block has by default, by identifier
	 * @return a list of blocks
	 */
	public static BlockListValue ofBlockList(String key, List<String> defaultValue,
			Map<String, Integer> defaults) {
		return new BlockListValue(key, defaultValue, defaults);
	}

	/**
	 * @param key          the config key
	 * @param defaultValue the value used when the key is absent
	 * @return a string setting
	 */
	public static ConfigValue<String> ofString(String key, String defaultValue) {
		return new ConfigValue<>(key, defaultValue, GSON.getAdapter(String.class));
	}

	/**
	 * @param key          the config key
	 * @param defaultValue the value used when the key is absent
	 * @param <E>          the enum type
	 * @return an enum setting, stored by constant name
	 */
	public static <E extends Enum<E>> ConfigValue<E> ofEnum(String key, E defaultValue) {
		@SuppressWarnings("unchecked")
		Class<E> type = (Class<E>) defaultValue.getDeclaringClass();
		TypeAdapter<E> adapter = GSON.getAdapter(type);
		return new ConfigValue<>(key, defaultValue, adapter);
	}
}

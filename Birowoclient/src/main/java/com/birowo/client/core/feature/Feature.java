package com.birowo.client.core.feature;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.birowo.client.core.config.ConfigValue;
import com.birowo.client.core.config.FeatureSettings;

/**
 * Base class for every feature.
 *
 * <p>A feature is <em>toggleable</em>, <em>configurable</em> and <em>independently disposable</em>:
 * disabling a feature must restore vanilla behaviour exactly. Subclasses therefore install their
 * hooks in {@link #onEnable()} and remove them in {@link #onDisable()}, and they must be
 * symmetrical.
 *
 * <p>This class deliberately has no dependency on Minecraft, so that the framework can be tested
 * without the game. A feature that acts on the game lives in the {@code features} package and is
 * written against the game itself; the services it shares with the rest of the client — the way
 * it draws in the world, the way it looks at the blocks around it — live in the platform layer.
 */
public abstract class Feature {
	private final String id;
	private final String displayName;
	private final String description;
	private final FeatureCategory category;
	private final FeatureSettings settings = new FeatureSettings();
	private final List<Consumer<Boolean>> stateListeners = new CopyOnWriteArrayList<>();

	private boolean enabled;

	/**
	 * @param id          stable machine-readable identifier, unique across all features; used as
	 *                    the config key and by the command line
	 * @param displayName human-readable name shown in the UI
	 * @param description one-line summary of what the feature does, shown as a tooltip
	 * @param category    category this feature is listed under
	 */
	protected Feature(String id, String displayName, String description, FeatureCategory category) {
		this.id = id;
		this.displayName = displayName;
		this.description = description;
		this.category = category;
	}

	/**
	 * @return the stable identifier of this feature
	 */
	public final String id() {
		return id;
	}

	/**
	 * @return the human-readable name of this feature
	 */
	public final String displayName() {
		return displayName;
	}

	/**
	 * @return a one-line summary of what this feature does
	 */
	public final String description() {
		return description;
	}

	/**
	 * @return the category this feature is listed under
	 */
	public final FeatureCategory category() {
		return category;
	}

	/**
	 * @return the mutable settings of this feature
	 */
	public final FeatureSettings settings() {
		return settings;
	}

	/**
	 * @return whether this feature is currently enabled
	 */
	public final boolean enabled() {
		return enabled;
	}

	/**
	 * Enables or disables this feature.
	 *
	 * <p>Calling this with the state the feature is already in is a no-op, so the enable and disable
	 * hooks always run in matching pairs.
	 *
	 * @param enabled the desired state
	 */
	public final void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}

		this.enabled = enabled;

		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}

		for (Consumer<Boolean> listener : stateListeners) {
			listener.accept(enabled);
		}
	}

	/**
	 * Registers a listener that is notified whenever this feature is enabled or disabled.
	 *
	 * <p>This is how state changes stay observable no matter who triggers them — the feature
	 * manager, a key binding, a command, or the feature itself.
	 *
	 * @param listener the listener to notify
	 */
	public void addStateListener(Consumer<Boolean> listener) {
		stateListeners.add(listener);
	}

	/**
	 * Flips the enabled state of this feature.
	 */
	public final void toggle() {
		setEnabled(!enabled);
	}

	/**
	 * Registers a setting on this feature's configuration.
	 *
	 * @param value the setting to register
	 * @param <T>   the type of the setting
	 * @return the same instance, for chaining
	 */
	protected final <T extends ConfigValue<?>> T setting(T value) {
		return settings.register(value);
	}

	/**
	 * @return whether the state of this feature is written to the configuration file
	 */
	public boolean persistable() {
		return true;
	}

	/**
	 * Called after the feature has been enabled. Install hooks here.
	 */
	protected void onEnable() {
	}

	/**
	 * Called after the feature has been disabled. Remove hooks here.
	 */
	protected void onDisable() {
	}

	/**
	 * Called once per client tick while the feature is enabled.
	 */
	public void onTick() {
	}

	/**
	 * Called once per frame while the feature is enabled.
	 *
	 * <p>The hook is for the work that does not need a rendering context, such as advancing an
	 * animation. Work that draws in the world or on the screen is done through the platform
	 * interfaces instead, because the core must not know about Minecraft.
	 */
	public void onRender() {
	}

	/**
	 * @return the values of the settings of this feature, in declaration order
	 */
	public final List<ConfigValue<?>> settingValues() {
		return settings.values();
	}
}

package com.birowo.client.core.feature;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.birowo.client.core.event.EventBus;
import com.birowo.client.core.event.FeatureToggleEvent;
import com.birowo.client.core.event.TickEvent;

/**
 * Owns every feature and drives their lifecycle.
 *
 * <p>Features tick in registration order, and a feature that throws is logged and skipped so that
 * one broken feature cannot stop the rest of the client from working.
 */
public final class FeatureRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger("BirowoClient/FeatureRegistry");

	private final EventBus eventBus;
	private final Map<String, Feature> features = new LinkedHashMap<>();

	/**
	 * @param eventBus the bus that feature state changes are posted to, and that drives the tick
	 */
	public FeatureRegistry(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.subscribe(TickEvent.class, this::onTick);
	}

	/**
	 * Registers a feature.
	 *
	 * @param feature the feature to register
	 * @throws IllegalArgumentException if a feature with the same id is already registered
	 */
	public void register(Feature feature) {
		if (features.putIfAbsent(feature.id(), feature) != null) {
			throw new IllegalArgumentException("Duplicate feature id: " + feature.id());
		}

		feature.addStateListener(enabled -> eventBus.post(new FeatureToggleEvent(feature, enabled)));
	}

	/**
	 * @param id the feature identifier
	 * @return the feature with that identifier, if registered
	 */
	public Optional<Feature> byId(String id) {
		return Optional.ofNullable(features.get(id));
	}

	/**
	 * @return every registered feature, in registration order
	 */
	public Collection<Feature> features() {
		return Collections.unmodifiableCollection(features.values());
	}

	/**
	 * @return the currently enabled features, in registration order
	 */
	public List<Feature> enabledFeatures() {
		return features.values().stream()
				.filter(Feature::enabled)
				.toList();
	}

	/**
	 * Ticks every enabled feature. Driven by the {@link TickEvent} this registry subscribes to.
	 *
	 * @param event the tick event
	 */
	private void onTick(TickEvent event) {
		for (Feature feature : features.values()) {
			if (!feature.enabled()) {
				continue;
			}

			try {
				feature.onTick();
			} catch (Throwable t) {
				LOGGER.error("Feature {} failed to tick", feature.id(), t);
			}
		}
	}
}

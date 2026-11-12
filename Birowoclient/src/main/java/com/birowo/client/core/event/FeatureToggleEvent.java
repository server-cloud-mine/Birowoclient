package com.birowo.client.core.event;

import com.birowo.client.core.feature.Feature;

/**
 * Posted when a feature is enabled or disabled.
 *
 * @param feature  the feature whose state changed
 * @param enabled the state the feature is now in
 */
public record FeatureToggleEvent(Feature feature, boolean enabled) implements Event {
}

/**
 * The features of the client.
 *
 * <p>This package holds the implementations of the features: the things the player switches on
 * and off, such as a combat helper or an overlay. A feature is a subclass of
 * {@link com.birowo.client.core.feature.Feature} that is registered with the
 * {@link com.birowo.client.core.feature.FeatureRegistry} when the client starts.
 *
 * <p>The framework itself lives in {@code com.birowo.client.core.feature} and knows nothing
 * about Minecraft. The services a feature shares with the rest of the client — the way it draws
 * in the world, the way it looks at the blocks around it — live in
 * {@code com.birowo.client.platform}.
 *
 * <p>Features are grouped by what they do, in one package per group:
 *
 * <pre>
 * com.birowo.client.features.combat
 * com.birowo.client.features.movement
 * com.birowo.client.features.render
 * com.birowo.client.features.misc
 * </pre>
 */
package com.birowo.client.features;

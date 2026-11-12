package com.birowo.client.platform.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureRegistry;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Routes the game's render passes to the features that implement them.
 *
 * <p>A feature that throws while rendering is logged and skipped: a single broken overlay must not
 * take the whole frame — or the game — down with it.
 */
public final class RenderDispatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger("BirowoClient/RenderDispatcher");

	private final FeatureRegistry featureRegistry;

	/**
	 * @param featureRegistry the registry holding the features to dispatch to
	 */
	public RenderDispatcher(FeatureRegistry featureRegistry) {
		this.featureRegistry = featureRegistry;
	}

	/**
	 * Draws the HUD layer of every feature that implements {@link HudRenderable}.
	 *
	 * <p>Every enabled feature is given its frame hook first, so that a feature can advance its
	 * animations before anything is drawn.
	 *
	 * @param context     the draw context of the current frame
	 * @param tickCounter the tick counter of the current frame
	 */
	public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		for (Feature feature : featureRegistry.features()) {
			if (!feature.enabled()) {
				continue;
			}

			try {
				feature.onRender();
			} catch (Throwable t) {
				LOGGER.error("Feature {} failed to render", feature.id(), t);
			}

			if (!(feature instanceof HudRenderable renderable)) {
				continue;
			}

			try {
				renderable.onHudRender(context, tickCounter);
			} catch (Throwable t) {
				LOGGER.error("Feature {} failed to render on the HUD", feature.id(), t);
			}
		}
	}

	/**
	 * Draws the world layer of every feature that implements {@link WorldRenderable}.
	 *
	 * @param context the render context of the current frame
	 */
	public void onWorldRender(WorldRenderContext context) {
		for (Feature feature : featureRegistry.features()) {
			if (!feature.enabled() || !(feature instanceof WorldRenderable renderable)) {
				continue;
			}

			try {
				renderable.onWorldRender(context);
			} catch (Throwable t) {
				LOGGER.error("Feature {} failed to render in the world", feature.id(), t);
			}
		}
	}
}

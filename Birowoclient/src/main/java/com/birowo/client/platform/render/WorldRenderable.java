package com.birowo.client.platform.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

/**
 * Implemented by features that draw in the world, after entities have been rendered.
 */
public interface WorldRenderable {
	/**
	 * Draws in the world.
	 *
	 * @param context the render context of the current frame
	 */
	void onWorldRender(WorldRenderContext context);
}

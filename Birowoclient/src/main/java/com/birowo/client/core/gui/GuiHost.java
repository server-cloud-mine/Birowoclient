package com.birowo.client.core.gui;

import java.util.Collection;
import java.util.List;

import com.birowo.client.core.feature.Feature;

/**
 * What the interface needs from the client in order to draw itself.
 *
 * <p>The contract is implemented by the core and consumed by the screen, which lives in the
 * platform layer. Keeping the contract in the core means the interface never has to know which
 * feature it is displaying, and it means the screen can be tested without a game.
 */
public interface GuiHost {
	/**
	 * @return every feature that may be displayed, in registration order
	 */
	Collection<Feature> features();

	/**
	 * @return the state of every panel, in display order
	 */
	List<PanelState> panels();

	/**
	 * @param feature the feature to look up
	 * @return the animated appearance of that feature
	 */
	FeatureVisual visualFor(Feature feature);

	/**
	 * Enables or disables a feature.
	 *
	 * @param feature the feature to toggle
	 */
	void toggleFeature(Feature feature);

	/**
	 * Called once the interface has finished closing.
	 */
	void onGuiClosed();
}

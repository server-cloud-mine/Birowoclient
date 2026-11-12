package com.birowo.client.core.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.birowo.client.core.feature.Feature;
import com.birowo.client.core.feature.FeatureCategory;
import com.birowo.client.core.feature.FeatureRegistry;

/**
 * The interface of the client, wired to the registry of the features.
 *
 * <p>The host owns the state that belongs to the interface rather than to any feature: the
 * position of the panels, the animated appearance of a row, and the layout on disk. A feature
 * that is added to the registry shows up in the interface without the interface having to know
 * anything about it.
 *
 * <p>The host is not a feature itself. The interface is the window through which the features are
 * seen, so it stays open whatever the player switches on or off, and it is opened by a key
 * binding rather than by a row of its own.
 */
public final class ClientGuiHost implements GuiHost {
	private final FeatureRegistry registry;
	private final GuiScreenLauncher launcher;
	private final GuiLayoutStore layoutStore;
	private final List<PanelState> panels = new ArrayList<>();
	private final Map<Feature, FeatureVisual> visuals = new HashMap<>();

	/**
	 * @param registry    the registry whose features the interface lists
	 * @param launcher    the platform service that opens and closes the interface
	 * @param layoutStore the store the positions of the panels are kept in
	 */
	public ClientGuiHost(FeatureRegistry registry, GuiScreenLauncher launcher, GuiLayoutStore layoutStore) {
		this.registry = registry;
		this.launcher = launcher;
		this.layoutStore = layoutStore;

		for (FeatureCategory category : FeatureCategory.values()) {
			panels.add(new PanelState(GuiPanelIds.forCategory(category), 0, 0));
		}

		panels.add(new PanelState(GuiPanelIds.SEARCH, 0, 0));
		layoutStore.load(panels);
	}

	/**
	 * Opens the interface.
	 */
	public void open() {
		launcher.open(this);
	}

	/**
	 * Closes the interface, if it is open.
	 */
	public void close() {
		launcher.close();
	}

	/**
	 * Writes the layout of the interface to disk.
	 */
	public void saveLayout() {
		layoutStore.save(panels);
	}

	@Override
	public Collection<Feature> features() {
		return registry.features();
	}

	@Override
	public List<PanelState> panels() {
		return panels;
	}

	@Override
	public FeatureVisual visualFor(Feature feature) {
		return visuals.computeIfAbsent(feature, key -> new FeatureVisual());
	}

	@Override
	public void toggleFeature(Feature feature) {
		feature.toggle();
	}

	@Override
	public void onGuiClosed() {
		// A panel that was still moving when the interface closed rests where it was let go.
		for (PanelState panel : panels) {
			panel.settle();
		}

		layoutStore.save(panels);
	}
}

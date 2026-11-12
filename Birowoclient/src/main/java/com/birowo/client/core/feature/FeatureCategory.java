package com.birowo.client.core.feature;

/**
 * Categories group features in the feature list and in the settings interface.
 *
 * <p>The order of the constants is the order they are presented to the user.
 */
public enum FeatureCategory {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	RENDER("Render"),
	CLIENT("Client"),
	MISC("Misc");

	private final String displayName;

	FeatureCategory(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the human-readable label shown in the feature list
	 */
	public String displayName() {
		return displayName;
	}
}

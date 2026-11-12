package com.birowo.client.core.config;

/**
 * The way the colour of a player is chosen.
 *
 * <p>The kinds are the ones the overlay of Meteor has: a player is shown in one colour that is
 * the same for everybody, in the colour of how much health is left, or in the colour of how far
 * away the player is.
 */
public enum EspColourMode {
	/**
	 * Every player is shown in the colour that is chosen.
	 */
	FIXED("Fixed"),

	/**
	 * The colour follows the health of the player: green when the player is untouched, red when
	 * the player is nearly dead.
	 */
	HEALTH("Health"),

	/**
	 * The colour follows the distance to the player: red when the player is close, green when the
	 * player is far away.
	 */
	DISTANCE("Distance");

	private final String displayName;

	EspColourMode(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the name of the kind as it is shown in the interface
	 */
	public String displayName() {
		return displayName;
	}
}

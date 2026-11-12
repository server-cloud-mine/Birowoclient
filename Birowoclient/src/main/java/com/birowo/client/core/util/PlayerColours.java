package com.birowo.client.core.util;

/**
 * The colours the players are shown in when their colour follows their health or their distance.
 *
 * <p>Both kinds of colour go from red to yellow to green: a player that is nearly dead or that
 * is very close is red, and a player that is untouched or that is far away is green.
 *
 * <p>The arithmetic is done here rather than in the game, so that it can be tested without the
 * game.
 */
public final class PlayerColours {
	/**
	 * How far away a player is when the colour has become green, in blocks.
	 */
	public static final double FAR_DISTANCE = 60.0D;

	/**
	 * The colour of a player that has no health to speak of.
	 */
	private static final int NO_HEALTH = 0xFF5555;

	private PlayerColours() {
	}

	/**
	 * The colour of a player, by the health that is left.
	 *
	 * @param health    how much health the player has left
	 * @param maxHealth how much health the player may have
	 * @return the colour, as a {@code 0xRRGGBB} value
	 */
	public static int byHealth(float health, float maxHealth) {
		if (maxHealth <= 0.0F) {
			return NO_HEALTH;
		}

		float percent = Math.max(0.0F, Math.min(1.0F, health / maxHealth));

		return gradient(percent);
	}

	/**
	 * The colour of a player, by the distance to the player.
	 *
	 * @param distance how far the player is away, in blocks
	 * @return the colour, as a {@code 0xRRGGBB} value
	 */
	public static int byDistance(double distance) {
		double percent = distance / FAR_DISTANCE;

		if (percent < 0.0D || percent > 1.0D) {
			// A player that is out of reach is as far away as it gets.
			return 0x00FF00;
		}

		return gradient(percent);
	}

	/**
	 * The colour that goes from red through yellow to green.
	 *
	 * @param percent where the colour stands between red and green, from zero to one
	 * @return the colour, as a {@code 0xRRGGBB} value
	 */
	private static int gradient(double percent) {
		int red;
		int green;

		if (percent < 0.5D) {
			// From red to yellow.
			red = 255;
			green = (int) (255 * percent / 0.5D);
		} else {
			// From yellow to green.
			green = 255;
			red = 255 - (int) (255 * (percent - 0.5D) / 0.5D);
		}

		return 0xFF000000 | (red << 16) | (green << 8);
	}
}

package com.birowo.client.core.util;

/**
 * The arithmetic of colours.
 *
 * <p>A colour is handled in two ways: as three numbers between zero and one, which is how a
 * colour is chosen by a person, and as a plain {@code 0xRRGGBB} value, which is how a colour is
 * written down.
 */
public final class ColourMath {
	/**
	 * A colour in the hue, saturation and brightness form.
	 *
	 * @param hue        the place of the colour on the colour wheel, from {@code 0} to {@code 1}
	 * @param saturation how much of the colour is of the hue itself, from {@code 0} to {@code 1}
	 * @param value     how bright the colour is, from {@code 0} to {@code 1}
	 */
	public record Hsv(float hue, float saturation, float value) {
	}

	private ColourMath() {
	}

	/**
	 * Turns a colour of the colour wheel into a plain colour.
	 *
	 * @param hue        the place of the colour on the colour wheel, from {@code 0} to {@code 1}
	 * @param saturation how much of the colour is of the hue itself, from {@code 0} to {@code 1}
	 * @param value     how bright the colour is, from {@code 0} to {@code 1}
	 * @return the colour as a {@code 0xRRGGBB} value
	 */
	public static int hsvToRgb(float hue, float saturation, float value) {
		float h = wrap(hue) * 6.0F;
		float s = clamp(saturation);
		float v = clamp(value);

		int sector = (int) Math.floor(h);
		float fraction = h - sector;

		float p = v * (1.0F - s);
		float q = v * (1.0F - fraction * s);
		float t = v * (1.0F - (1.0F - fraction) * s);

		float red;
		float green;
		float blue;

		switch (Math.floorMod(sector, 6)) {
			case 0 -> {
				red = v;
				green = t;
				blue = p;
			}
			case 1 -> {
				red = q;
				green = v;
				blue = p;
			}
			case 2 -> {
				red = p;
				green = v;
				blue = t;
			}
			case 3 -> {
				red = p;
				green = q;
				blue = v;
			}
			case 4 -> {
				red = t;
				green = p;
				blue = v;
			}
			default -> {
				red = v;
				green = p;
				blue = q;
			}
		}

		return (toByte(red) << 16) | (toByte(green) << 8) | toByte(blue);
	}

	/**
	 * Turns a plain colour into a colour of the colour wheel.
	 *
	 * @param rgb the colour as a {@code 0xRRGGBB} value
	 * @return the colour in the hue, saturation and brightness form
	 */
	public static Hsv rgbToHsv(int rgb) {
		float red = ((rgb >> 16) & 0xFF) / 255.0F;
		float green = ((rgb >> 8) & 0xFF) / 255.0F;
		float blue = (rgb & 0xFF) / 255.0F;

		float max = Math.max(red, Math.max(green, blue));
		float min = Math.min(red, Math.min(green, blue));
		float difference = max - min;

		float hue;

		if (difference == 0.0F) {
			hue = 0.0F;
		} else if (max == red) {
			hue = ((green - blue) / difference) / 6.0F;
		} else if (max == green) {
			hue = (2.0F + (blue - red) / difference) / 6.0F;
		} else {
			hue = (4.0F + (red - green) / difference) / 6.0F;
		}

		if (hue < 0.0F) {
			hue += 1.0F;
		}

		float saturation = max == 0.0F ? 0.0F : difference / max;

		return new Hsv(wrap(hue), saturation, max);
	}

	/**
	 * @param rgb the colour as a {@code 0xRRGGBB} value
	 * @return the colour as six hexadecimal digits, such as {@code F9801D}
	 */
	public static String toHex(int rgb) {
		return String.format("%06X", rgb & 0xFFFFFF);
	}

	/**
	 * Reads a colour written as hexadecimal digits.
	 *
	 * @param text the colour, with or without a hash in front of it
	 * @return the colour as a {@code 0xRRGGBB} value, or {@code -1} when the text is not a colour
	 */
	public static int fromHex(String text) {
		String trimmed = text.trim();

		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		}

		if (trimmed.length() > 6) {
			trimmed = trimmed.substring(0, 6);
		}

		try {
			return Integer.parseInt(trimmed, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * @param hue a place on the colour wheel
	 * @return the place on the colour wheel, from {@code 0} to {@code 1}
	 */
	private static float wrap(float hue) {
		float wrapped = hue % 1.0F;

		if (wrapped < 0.0F) {
			wrapped += 1.0F;
		}

		return wrapped;
	}

	/**
	 * @param value a number
	 * @return the number, from {@code 0} to {@code 1}
	 */
	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	/**
	 * @param value a number between zero and one
	 * @return the number as a byte
	 */
	private static int toByte(float value) {
		return Math.round(clamp(value) * 255.0F);
	}
}

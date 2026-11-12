package com.birowo.client.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests of the arithmetic of colours.
 */
class ColourMathTest {
	@Test
	void turnsTheColoursOfTheWheelIntoPlainColours() {
		assertEquals(0xFF0000, ColourMath.hsvToRgb(0.0F, 1.0F, 1.0F));
		assertEquals(0x00FF00, ColourMath.hsvToRgb(1.0F / 3.0F, 1.0F, 1.0F));
		assertEquals(0x0000FF, ColourMath.hsvToRgb(2.0F / 3.0F, 1.0F, 1.0F));
	}

	@Test
	void turnsTheColoursOfTheWheelIntoGreyWhenTheyAreNotSaturated() {
		assertEquals(0xFFFFFF, ColourMath.hsvToRgb(0.5F, 0.0F, 1.0F));
		assertEquals(0x808080, ColourMath.hsvToRgb(0.5F, 0.0F, 0.5F));
		assertEquals(0x000000, ColourMath.hsvToRgb(0.5F, 1.0F, 0.0F));
	}

	@Test
	void readsAPlainColourBackAsAColourOfTheWheel() {
		ColourMath.Hsv hsv = ColourMath.rgbToHsv(0x00FF00);

		assertEquals(1.0F / 3.0F, hsv.hue(), 0.001F);
		assertEquals(1.0F, hsv.saturation(), 0.001F);
		assertEquals(1.0F, hsv.value(), 0.001F);
	}

	@Test
	void readsAGreyBackAsAColourWithoutAHue() {
		ColourMath.Hsv hsv = ColourMath.rgbToHsv(0x808080);

		assertEquals(0.0F, hsv.saturation(), 0.001F);
		assertEquals(128.0F / 255.0F, hsv.value(), 0.001F);
	}

	@Test
	void survivesTheRoundTrip() {
		int[] colours = {0xF9801D, 0x5DECF5, 0xFED83D, 0x1D1D21, 0x3C44AA};

		for (int colour : colours) {
			ColourMath.Hsv hsv = ColourMath.rgbToHsv(colour);

			assertEquals(colour, ColourMath.hsvToRgb(hsv.hue(), hsv.saturation(), hsv.value()));
		}
	}

	@Test
	void writesAColourAsSixDigits() {
		assertEquals("F9801D", ColourMath.toHex(0xF9801D));
	}

	@Test
	void readsAColourWrittenAsSixDigits() {
		assertEquals(0xF9801D, ColourMath.fromHex("#F9801D"));
		assertEquals(0xF9801D, ColourMath.fromHex("F9801D"));
	}

	@Test
	void refusesTextThatIsNotAColour() {
		assertEquals(-1, ColourMath.fromHex("orange"));
		assertTrue(ColourMath.fromHex("") == -1);
	}
}

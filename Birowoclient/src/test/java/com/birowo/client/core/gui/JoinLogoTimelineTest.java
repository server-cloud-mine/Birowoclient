package com.birowo.client.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests of the timing of the logo that is shown when the player joins a server.
 */
class JoinLogoTimelineTest {
	private final JoinLogoTimeline timeline = new JoinLogoTimeline();

	@Test
	void startsInvisible() {
		JoinLogoTimeline.Frame frame = timeline.frameAt(0.0);

		assertEquals(0.0, frame.alpha());
		assertFalse(frame.finished());
	}

	@Test
	void fadesIn() {
		JoinLogoTimeline.Frame frame = timeline.frameAt(JoinLogoTimeline.FADE_IN_END);

		assertEquals(1.0, frame.alpha());
		assertEquals(1.0, frame.scale());
	}

	@Test
	void holdsBeforeItFades() {
		assertEquals(1.0, timeline.frameAt(JoinLogoTimeline.HOLD_END).alpha());
		assertTrue(timeline.frameAt(JoinLogoTimeline.HOLD_END + 0.1).alpha() < 1.0);
	}

	@Test
	void fadesAway() {
		assertEquals(0.0, timeline.frameAt(JoinLogoTimeline.FADE_OUT_END).alpha());
		assertTrue(timeline.frameAt(JoinLogoTimeline.FADE_OUT_END).finished());
	}

	@Test
	void neverBecomesVisibleAfterTheEnd() {
		JoinLogoTimeline.Frame frame = timeline.frameAt(JoinLogoTimeline.FADE_OUT_END + 5.0);

		assertEquals(0.0, frame.alpha());
		assertTrue(frame.finished());
	}

	@Test
	void ignoresTimeBeforeTheStart() {
		JoinLogoTimeline.Frame frame = timeline.frameAt(-5.0);

		assertEquals(0.0, frame.alpha());
		assertFalse(frame.finished());
	}
}

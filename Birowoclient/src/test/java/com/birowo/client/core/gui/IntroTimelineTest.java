package com.birowo.client.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests of the timing of the introduction animation.
 */
class IntroTimelineTest {
	private final IntroTimeline timeline = new IntroTimeline();

	@Test
	void startsEmpty() {
		IntroTimeline.Frame frame = timeline.frameAt(0.0);

		assertEquals(0.0, frame.slashProgress());
		assertEquals(0.0, frame.markAlpha());
		assertEquals(0, frame.lettersTyped());
		assertEquals(1.0, frame.alpha());
		assertFalse(frame.finished());
	}

	@Test
	void slashBurnsThrough() {
		IntroTimeline.Frame middle = timeline.frameAt(IntroTimeline.SLASH_END / 2.0);

		assertTrue(middle.slashProgress() > 0.0);
		assertTrue(middle.slashProgress() < 1.0);

		assertEquals(1.0, timeline.frameAt(IntroTimeline.SLASH_END).slashProgress());
	}

	@Test
	void markAppearsAfterTheSlash() {
		assertEquals(0.0, timeline.frameAt(IntroTimeline.MARK_START).markAlpha());
		assertEquals(1.0, timeline.frameAt(IntroTimeline.MARK_END).markAlpha());
	}

	@Test
	void nameIsTypedLetterByLetter() {
		IntroTimeline.Frame frame = timeline.frameAt(IntroTimeline.TYPE_START);

		assertEquals(0, frame.lettersTyped());

		IntroTimeline.Frame last = timeline.frameAt(IntroTimeline.TYPE_END);

		assertEquals(IntroTimeline.LETTERS, last.lettersTyped());
		assertEquals(0.0, last.letterReveal());
	}

	@Test
	void nameIsNeverTypedPastItsLastLetter() {
		IntroTimeline.Frame frame = timeline.frameAt(IntroTimeline.TYPE_END + 10.0);

		assertEquals(IntroTimeline.LETTERS, frame.lettersTyped());
		assertTrue(frame.letterReveal() >= 0.0);
		assertTrue(frame.letterReveal() < 1.0);
	}

	@Test
	void holdsBeforeItFades() {
		assertEquals(1.0, timeline.frameAt(IntroTimeline.HOLD_END).alpha());
		assertEquals(0.0, timeline.frameAt(IntroTimeline.FADE_END).alpha());
		assertTrue(timeline.frameAt(IntroTimeline.HOLD_END + 0.1).alpha() < 1.0);
	}

	@Test
	void finishesAtTheEnd() {
		assertFalse(timeline.frameAt(IntroTimeline.FADE_END - 0.01).finished());
		assertTrue(timeline.frameAt(IntroTimeline.FADE_END).finished());
		assertTrue(timeline.frameAt(IntroTimeline.FADE_END + 5.0).finished());
	}

	@Test
	void ignoresTimeBeforeTheStart() {
		IntroTimeline.Frame frame = timeline.frameAt(-5.0);

		assertEquals(0.0, frame.slashProgress());
		assertEquals(0, frame.lettersTyped());
		assertEquals(1.0, frame.alpha());
	}
}

package net.yapbam.utils;

import static org.junit.Assert.*;

import net.yapbam.util.TextMatcher;

import org.junit.Test;

public class TextMatcherTest {
	@Test
	public void testEquals() {
		TextMatcher matcher = new TextMatcher(TextMatcher.Kind.EQUALS, "�t�".toUpperCase(), true, true);
		assertFalse(matcher.matches("Et�"));
		assertFalse(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertFalse(matcher.matches("�t�".toUpperCase()+"s"));

		matcher = new TextMatcher(TextMatcher.Kind.EQUALS, "�t�".toUpperCase(), true, false);
		assertFalse(matcher.matches("Et�"));
		assertFalse(matcher.matches("�t�"));
		assertTrue(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertFalse(matcher.matches("�t�".toUpperCase()+"s"));

		matcher = new TextMatcher(TextMatcher.Kind.EQUALS, "�t�".toUpperCase(), false, false);
		assertTrue(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertTrue(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertFalse(matcher.matches("�t�".toUpperCase()+"s"));

		matcher = new TextMatcher(TextMatcher.Kind.EQUALS, "�t�".toUpperCase(), false, true);
		assertFalse(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertFalse(matcher.matches("�t�".toUpperCase()+"s"));
	}

	@Test
	public void testContains() {
		TextMatcher matcher = new TextMatcher(TextMatcher.Kind.CONTAINS, "�t�".toUpperCase(), true, true);
		assertFalse(matcher.matches("Et�"));
		assertFalse(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertTrue(matcher.matches("x�t�".toUpperCase()));
		assertTrue(matcher.matches("�t�".toUpperCase()+"s"));

		matcher = new TextMatcher(TextMatcher.Kind.CONTAINS, "�t�".toUpperCase(), true, false);
		assertFalse(matcher.matches("Et�"));
		assertFalse(matcher.matches("�t�"));
		assertTrue(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertTrue(matcher.matches("x"+"�t�".toUpperCase()+"s"));

		matcher = new TextMatcher(TextMatcher.Kind.CONTAINS, "�t�".toUpperCase(), false, false);
		assertTrue(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertTrue(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertTrue(matcher.matches("�t�".toUpperCase()+"s"));

		matcher = new TextMatcher(TextMatcher.Kind.CONTAINS, "�t�".toUpperCase(), false, true);
		assertFalse(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertTrue(matcher.matches("x�t�".toUpperCase()));
		assertTrue(matcher.matches("�t�".toUpperCase()+"s"));
	}

	@Test
	public void testRegular() {
		TextMatcher matcher = new TextMatcher(TextMatcher.Kind.REGULAR, "^�t.*$", true, true);
		assertFalse(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertFalse(matcher.matches("�t�".toUpperCase()));
		assertFalse(matcher.matches("�tre"));

		matcher = new TextMatcher(TextMatcher.Kind.REGULAR, "^�t.*$", true, false);
		assertFalse(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertFalse(matcher.matches("�t�".toUpperCase()));
		assertTrue(matcher.matches("�tre"));

		matcher = new TextMatcher(TextMatcher.Kind.REGULAR, "^�t.*$", false, false);
		assertTrue(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertTrue(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertTrue(matcher.matches("�tre"));
		assertFalse(matcher.matches("h�tre"));

		matcher = new TextMatcher(TextMatcher.Kind.REGULAR, "^�t.*$", false, true);
		assertFalse(matcher.matches("Et�"));
		assertTrue(matcher.matches("�t�"));
		assertFalse(matcher.matches("ETE"));
		assertTrue(matcher.matches("�t�".toUpperCase()));
		assertFalse(matcher.matches("�tre"));
	}

}

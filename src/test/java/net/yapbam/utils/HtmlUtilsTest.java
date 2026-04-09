package net.yapbam.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.yapbam.util.HtmlUtils;

class HtmlUtilsTest {

	@Test
	void test() {
		assertEquals ("This is a <a href=\"http://www.google.com\">link to Google</a>.", HtmlUtils.toHtml("This is a [link to Google[http://www.google.com]]."));
	}

	@Test
	void testNoLinkButEscaped() {
		assertEquals ("A very simple &lt;test&gt;", HtmlUtils.toHtml("A very simple <test>"));
	}

	@Test
	void testNoText() {
		assertEquals ("Try <a href=\"http://www.google.com\">http://www.google.com</a>", HtmlUtils.toHtml("Try [[http://www.google.com]]"));
	}

	@Test
	void testFake() {
		assertEquals ("see dfd[test] or [[]] or [xxx[]] or [yyy[zzz]]", HtmlUtils.toHtml("see dfd[test] or [[]] or [xxx[]] or [yyy[zzz]]"));
	}

	@Test
	void testFollowing() {
		assertEquals ("<a href=\"http://x.com\">x</a><a href=\"http://y.com\">y</a>", HtmlUtils.toHtml("[x[http://x.com]][y[http://y.com]]"));
	}

	@Test
	void testComplex() {
		assertEquals ("<a href=\"http://x.com\">x</a>.[[]]<a href=\"http://y.com\">y</a>", HtmlUtils.toHtml("[x[http://x.com]].[[]][y[http://y.com]]"));
	}
	
	@Test
	void testConvertLines() {
		assertEquals("a is &lt; b<BR>ok", HtmlUtils.linesToHtml(false, "a is < b","ok"));
		assertEquals("<HTML>a<BR>b</HTML>", HtmlUtils.linesToHtml(true, "a","b"));
	}
}

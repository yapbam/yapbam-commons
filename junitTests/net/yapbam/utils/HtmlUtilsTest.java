package net.yapbam.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import net.yapbam.util.HtmlUtils;

public class HtmlUtilsTest {

	@Test
	public void test() {
		assertEquals ("This is a <a href=\"http://www.google.com\">link to Google</a>", HtmlUtils.toHtml("This is a [link to Google[http://www.google.com]]"));
		assertEquals ("Try <a href=\"http://www.google.com\">http://www.google.com</a>", HtmlUtils.toHtml("Try [[http://www.google.com]]"));
		assertEquals ("A very simple &lt;test&gt;", HtmlUtils.toHtml("A very simple <test>"));
		assertEquals ("see dfd[test] or [[]] or [xxx[]] or [yyy[zzz]]", HtmlUtils.toHtml("see dfd[test] or [[]] or [xxx[]] or [yyy[zzz]]"));
	}

}

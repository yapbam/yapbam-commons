package net.yapbam.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import net.yapbam.util.Mangler;

public class ManglerTest {
	@Test
	public void test() {
		String original = "RXT 1.6 16V";
		String escapedChars = " ";
		Mangler m = new Mangler(escapedChars, '_');
		String mangled = m.mangle(original);
		String unmangled = m.unmangle(mangled);
		assertEquals(original, unmangled);
		for (int i = 0; i < mangled.length(); i++) {
			if (escapedChars.contains(mangled.substring(i, i+1))) {
				fail("mangled contains "+mangled.substring(i, i+1));
			}
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEscapeInEscaped() {
		new Mangler("_", '_');
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEscapedInEscaped() {
		new Mangler("+1", '_');
	}
}

package net.yapbam.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.yapbam.util.Mangler;

class ManglerTest {
	@Test
	void test() {
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
	
	@Test
	void testEscapeInEscaped() {
		assertThrows(IllegalArgumentException.class, () -> new Mangler("_", '_'));
	}
	
	@Test
	void testEscapedInEscaped() {
		assertThrows(IllegalArgumentException.class, () -> new Mangler("+1", '_'));
	}
}

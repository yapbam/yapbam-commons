package net.yapbam.currency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CurrencyNamesTest {
	private static Locale locale;
	
	@BeforeAll
	static void setUpBeforeClass() {
		locale = Locale.getDefault();
		Locale.setDefault(Locale.FRANCE);
	}

	@AfterAll
	static void tearDownAfterClass() {
		Locale.setDefault(locale);
	}

	@Test
	void test() {
		assertEquals("XXXX", CurrencyNames.get("XXXX"));
		assertEquals("Euro", CurrencyNames.get("EUR"));
	}
}

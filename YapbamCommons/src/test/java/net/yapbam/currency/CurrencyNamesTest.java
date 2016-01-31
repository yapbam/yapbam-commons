package net.yapbam.currency;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CurrencyNamesTest {
	private static Locale locale;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		locale = Locale.getDefault();
		Locale.setDefault(Locale.FRANCE);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Locale.setDefault(locale);
	}

	@Test
	public void test() {
		assertEquals("XXXX", CurrencyNames.get("XXXX"));
		assertEquals("Euro", CurrencyNames.get("EUR"));
	}
}

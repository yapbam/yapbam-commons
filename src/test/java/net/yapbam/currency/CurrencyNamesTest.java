package net.yapbam.currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
		// Warning: this test uses the currencyNames.properties file from src/test/resources not the one from src/main/resources
		try (MockedStatic<CurrencyNames> mockedCurrencyNames = mockStatic(CurrencyNames.class, CALLS_REAL_METHODS)) {
			mockedCurrencyNames.when(() -> CurrencyNames.getJavaDisplayName(anyString())).thenReturn(null);
			
			assertEquals("XXXX", CurrencyNames.get("XXXX"), "CurrencyNames.get on unknown currency code should return the currency code");

			mockedCurrencyNames.when(() -> CurrencyNames.getJavaDisplayName("BOTH")).thenReturn("BothFromJava");
			assertEquals("Les deux", CurrencyNames.get("BOTH"), "CurrencyNames.get should return language specific resource wording when it exists");

			assertEquals("Default", CurrencyNames.get("DEFAULT"), "CurrencyNames.get should return default resource wording when it is the only that exists exists");

			mockedCurrencyNames.when(() -> CurrencyNames.getJavaDisplayName("DEFAULT")).thenReturn("DefaultFromJava");
			assertEquals("DefaultFromJava", CurrencyNames.get("DEFAULT"), "CurrencyNames.get should return java wording before default resource wording");
		}
	}

	@Test
	void testGetJavaDisplayName() {
		// Test with a known currency code
		String displayName = CurrencyNames.getJavaDisplayName("EUR");
		assertNotNull(displayName, "Java display name should not be null for EUR");
		assertFalse(displayName.isEmpty(), "Java display name should not be empty for EUR currency code");
		
		// Test with an unknown currency code
		String unknownDisplayName = CurrencyNames.getJavaDisplayName("UNKNOWN");
		assertNull(unknownDisplayName, "Java display name should be null for UNKNOWN currency code");
	}
}

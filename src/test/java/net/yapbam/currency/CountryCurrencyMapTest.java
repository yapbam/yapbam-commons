package net.yapbam.currency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CountryCurrencyMapTest {
	@Test
	void test() {
		CountryCurrencyMap map = CountryCurrencyMap.INSTANCE;
		
		String[] isoCountries = Locale.getISOCountries();
		Set<String> countries = map.getCountries();
		for (String isoCountry : isoCountries) {
			assertTrue(countries.contains(isoCountry));
			Currency c = Currency.getInstance(new Locale.Builder().setRegion(isoCountry).build());
			if (c==null) {
				assertNull(map.getCurrency(isoCountry));
			} else {
				assertEquals(c.getCurrencyCode(), map.getCurrency(isoCountry));
			}
		}
	}

	@Test
	void testNoChangeAllowed1() {
		Set<String> countries = CountryCurrencyMap.INSTANCE.getCountries();
		assertThrows (UnsupportedOperationException.class, countries::clear);
	}

	@Test
	void testNoChangeAllowed2() {
		Set<String> currencies = CountryCurrencyMap.INSTANCE.getCurrencies();
		assertThrows (UnsupportedOperationException.class, currencies::clear);
	}

	@Test
	void testNoChangeAllowed3() {
		CountryCurrencyMap map = CountryCurrencyMap.INSTANCE;
		String currency = map.getCurrencies().iterator().next();
		Set<String> countries = map.getCountries(currency);
		assertThrows (UnsupportedOperationException.class, countries::clear);
	}
}

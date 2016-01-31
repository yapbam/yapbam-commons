package net.yapbam.currency;

import static org.junit.Assert.*;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import net.yapbam.currency.CountryCurrencyMap;

import org.junit.Test;

public class CountryCurrencyMapTest {
	@Test
	public void test() {
		CountryCurrencyMap map = CountryCurrencyMap.INSTANCE;
		
		String[] isoCountries = Locale.getISOCountries();
		Set<String> countries = map.getCountries();
		for (String isoCountry : isoCountries) {
			assertTrue(countries.contains(isoCountry));
			Currency c = Currency.getInstance(new Locale("",isoCountry));
			if (c==null) {
				assertNull(map.getCurrency(isoCountry));
			} else {
				assertEquals(c.getCurrencyCode(), map.getCurrency(isoCountry));
			}
		}
	}

	@Test (expected=UnsupportedOperationException.class)
	public void testNoChangeAllowed1() {
		CountryCurrencyMap.INSTANCE.getCountries().clear();
	}

	@Test (expected=UnsupportedOperationException.class)
	public void testNoChangeAllowed2() {
		CountryCurrencyMap.INSTANCE.getCurrencies().clear();
	}

	@Test (expected=UnsupportedOperationException.class)
	public void testNoChangeAllowed3() {
		CountryCurrencyMap map = CountryCurrencyMap.INSTANCE;
		String currency = map.getCurrencies().iterator().next();
		map.getCountries(currency).clear();
	}
}

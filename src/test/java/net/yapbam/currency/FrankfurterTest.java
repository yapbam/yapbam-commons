package net.yapbam.currency;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.yapbam.remote.Cache;
import net.yapbam.remote.MemoryCache;

class FrankfurterTest {
	private static String path;

	private static class FrankfurterTestConverter extends FrankfurterCurrencyConverter {
		public FrankfurterTestConverter(Cache cache) {
			super(Proxy.NO_PROXY, cache);
		}

		@Override
		protected URL getSourceURL() {
			return getClass().getResource(path);
		}
	}

	@Test
	void test() throws IOException, ParseException {
		path = "frankfurter.json";
		Cache cache = new MemoryCache();
		AbstractCurrencyConverter cvt = new FrankfurterTestConverter(cache);
		assertTrue(cvt.getRefreshTimeStamp()<0);
		assertTrue(cvt.getTimeStamp()<0);
		assertFalse(cvt.isSynchronized());
		assertEquals(0, cvt.getCurrencies().length);
		SpyObserver observer = new SpyObserver();
		cvt.addObserver(observer);
		cvt.update();
		assertTrue(observer.wasCalled());
		assertTrue(cvt.getRefreshTimeStamp()>0);
		assertTrue(cvt.getTimeStamp()>0);
		assertTrue(cvt.isSynchronized());
		assertTrue(cvt.isAvailable("AED"));
		assertTrue(cvt.isAvailable("AFN"));
		assertTrue(cvt.isAvailable("ALL"));
		assertEquals(1.0, cvt.convert(1.0, "AED", "AED"), 0.0);
		assertEquals(4.3272, cvt.convert(1.0, "EUR", "AED"), 0.0001);
		assertEquals(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-04-15").getTime(), cvt.getTimeStamp());
		path = "bad_frankfurter.json";
		AbstractCurrencyConverter x = new FrankfurterTestConverter(cache);
		Set<String> currencies = new HashSet<String>(Arrays.asList(x
				.getCurrencies()));
		assertEquals(new HashSet<>(Arrays.asList("AED", "AFN", "ALL", "AMD", "EUR")), currencies);
		assertEquals(1.0/95.8, x.convert(1.0, "ALL", "EUR"), 0.0001);
	}

	@Test
	void testBad1() {
		path = "bad_frankfurter.json";
		AbstractCurrencyConverter cvt = new FrankfurterTestConverter(new MemoryCache());
		assertThrows(ParseException.class, cvt::update);
	}

	@Test
	void testBadArgs() throws IOException, ParseException {
		path = "frankfurter.json";
		AbstractCurrencyConverter cvt = new FrankfurterTestConverter(new MemoryCache());
		cvt.update();
		assertThrows(IllegalArgumentException.class, () -> cvt.convert(1.0, "XXX", "USD"));
		assertThrows(IllegalArgumentException.class, () -> cvt.convert(1.0, "USD", "XXX"));
	}

	@Test
	void testUnknown() {
		path = "unknown.json";
		AbstractCurrencyConverter cvt = new FrankfurterTestConverter(new MemoryCache());
		assertThrows(IOException.class, cvt::update);
	}
}

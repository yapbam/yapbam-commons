package net.yapbam.currency;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.yapbam.remote.Cache;
import net.yapbam.remote.MemoryCache;

class YahooTest {
	private static String path;
	
	@SuppressWarnings("deprecation")
	private static class YahooTestConverter extends YahooCurrencyConverter {
		public YahooTestConverter(Cache cache) {
			super(Proxy.NO_PROXY, cache);
		}

		@Override
		protected URL getSourceURL() {
			return getClass().getResource(path);
		}
	}

	@Test
	void test() throws IOException, ParseException {
		path = "yahoo.xml";
		Cache cache = new MemoryCache();
		AbstractCurrencyConverter cvt = new YahooTestConverter(cache);
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
		assertFalse(cvt.isAvailable("ARG"));
		assertTrue(cvt.isAvailable("USD"));
		assertTrue(cvt.isAvailable("VND"));
		assertTrue(cvt.isAvailable("EUR"));
		assertEquals(1.0, cvt.convert(1.0, "USD", "USD"), 0.0);
		assertEquals(0.730903, cvt.convert(1.0, "USD", "EUR"), 0.0001);
		assertEquals(1387592587000L, cvt.getTimeStamp());
		path = "bad_yahoo.xml";
		AbstractCurrencyConverter x = new YahooTestConverter(cache);
		Set<String> currencies = new HashSet<String>(Arrays.asList(x.getCurrencies()));
		assertEquals(3, currencies.size());
		assertTrue(currencies.contains("USD"));
		assertTrue(currencies.contains("EUR"));
		assertTrue(currencies.contains("VND"));
		assertEquals(0.730903, x.convert(1.0, "USD", "EUR"), 0.0001);
	}

	@Test
	void testBad1() {
		path = "bad_yahoo.xml";
		AbstractCurrencyConverter cvt = new YahooTestConverter(new MemoryCache());
		assertThrows(ParseException.class, cvt::update);
	}

	@Test
	void testBadArgs() throws IOException, ParseException {
		path = "yahoo.xml";
		AbstractCurrencyConverter cvt = new YahooTestConverter(new MemoryCache());
		cvt.update();
		assertThrows(IllegalArgumentException.class, () -> cvt.convert(1.0, "XXX", "USD"));
		assertThrows(IllegalArgumentException.class, () -> cvt.convert(1.0, "USD", "XXX"));
	}
	
	@Test
	void testUnknown() {
		AbstractCurrencyConverter cvt = new YahooTestConverter(new MemoryCache());
		path = "unknown.xml";
		assertThrows(IOException.class, cvt::update);
	}
}

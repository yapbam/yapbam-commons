package net.yapbam.currency;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.yapbam.remote.Cache;
import net.yapbam.remote.MemoryCache;

import org.junit.Test;

public class YahooTest {
	private static String path;
	
	private static class YahooTestConverter extends YahooCurrencyConverter {
		public YahooTestConverter(Cache cache) throws IOException, ParseException {
			super(Proxy.NO_PROXY, cache);
		}

		@Override
		protected URL getSourceURL() {
			return getClass().getResource(path);
		}
	}

	@Test
	public void test() throws IOException, ParseException {
		path = "yahoo.xml";
		Cache cache = new MemoryCache();
		YahooCurrencyConverter cvt = new YahooTestConverter(cache);
		cvt.update();
		assertFalse(cvt.isAvailable("ARG"));
		assertTrue(cvt.isAvailable("USD"));
		assertTrue(cvt.isAvailable("VND"));
		assertTrue(cvt.isAvailable("EUR"));
		assertEquals(1.0, cvt.convert(1.0, "USD", "USD"), 0.0);
		assertEquals(0.730903, cvt.convert(1.0, "USD", "EUR"), 0.0001);
		assertEquals(1387557000000L, cvt.getTimeStamp());
		path = "bad_yahoo.xml";
		YahooCurrencyConverter x = new YahooTestConverter(cache);
		Set<String> currencies = new HashSet<String>(Arrays.asList(x.getCurrencies()));
		assertEquals(3, currencies.size());
		assertTrue(currencies.contains("USD"));
		assertTrue(currencies.contains("EUR"));
		assertTrue(currencies.contains("VND"));
		assertEquals(0.730903, x.convert(1.0, "USD", "EUR"), 0.0001);
	}

	@Test (expected = ParseException.class)
	public void testBad1() throws IOException, ParseException {
		path = "bad_yahoo.xml";
		new YahooTestConverter(new MemoryCache()).update();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadArgs1() throws IOException, ParseException {
		path = "yahoo.xml";
		YahooCurrencyConverter cvt = new YahooTestConverter(new MemoryCache());
		cvt.update();
		cvt.convert(1.0, "XXX", "USD");
	}


	@Test (expected = IllegalArgumentException.class)
	public void testBadArgs2() throws IOException, ParseException {
		path = "yahoo.xml";
		YahooCurrencyConverter cvt = new YahooTestConverter(new MemoryCache());
		cvt.update();
		cvt.convert(1.0, "USD", "XXX");
	}
	
	@Test (expected = IOException.class)
	public void testUnknown() throws IOException, ParseException {
		path = "unknown.xml";
		new YahooTestConverter(new MemoryCache()).update();
	}
}

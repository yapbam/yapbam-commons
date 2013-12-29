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

public class ECBTest {
	private static String path;

	private static class ECBTestConverter extends ECBCurrencyConverter {
		public ECBTestConverter(Cache cache) throws IOException, ParseException {
			super(Proxy.NO_PROXY, cache);
		}

		@Override
		protected URL getSourceURL() {
			return getClass().getResource(path);
		}
	}

	@Test
	public void test() throws IOException, ParseException {
		path = "ecb.xml";
		Cache cache = new MemoryCache();
		AbstractCurrencyConverter cvt = new ECBTestConverter(cache);
		assertTrue(cvt.getRefreshTimeStamp()<0);
		assertTrue(cvt.getTimeStamp()<0);
		assertFalse(cvt.isSynchronized());
		assertEquals(0, cvt.getCurrencies().length);
		cvt.update();
		assertTrue(cvt.getRefreshTimeStamp()>0);
		assertTrue(cvt.getTimeStamp()>0);
		assertTrue(cvt.isSynchronized());
		assertTrue(cvt.isAvailable("USD"));
		assertTrue(cvt.isAvailable("JPY"));
		assertTrue(cvt.isAvailable("EUR"));
		assertEquals(1.0, cvt.convert(1.0, "USD", "USD"), 0.0);
		assertEquals(1.3655, cvt.convert(1.0, "EUR", "USD"), 0.0001);
		assertEquals(1387545300000L, cvt.getTimeStamp());
		path = "bad_ecb.xml";
		AbstractCurrencyConverter x = new ECBTestConverter(cache);
		Set<String> currencies = new HashSet<String>(Arrays.asList(x
				.getCurrencies()));
		assertEquals(3, currencies.size());
		assertTrue(currencies.contains("USD"));
		assertTrue(currencies.contains("EUR"));
		assertTrue(currencies.contains("JPY"));
		assertEquals(1.0/1.3655, x.convert(1.0, "USD", "EUR"), 0.0001);
	}

	@Test(expected = ParseException.class)
	public void testBad1() throws IOException, ParseException {
		path = "bad_ecb.xml";
		new ECBTestConverter(new MemoryCache()).update();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArgs1() throws IOException, ParseException {
		path = "ecb.xml";
		AbstractCurrencyConverter cvt = new ECBTestConverter(new MemoryCache());
		cvt.update();
		cvt.convert(1.0, "XXX", "USD");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArgs2() throws IOException, ParseException {
		path = "yahoo.xml";
		AbstractCurrencyConverter cvt = new ECBTestConverter(new MemoryCache());
		cvt.update();
		cvt.convert(1.0, "USD", "XXX");
	}

	@Test(expected = IOException.class)
	public void testUnknown() throws IOException, ParseException {
		path = "unknown.xml";
		new ECBTestConverter(new MemoryCache()).update();
	}
}

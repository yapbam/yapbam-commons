package net.yapbam.currency;

import java.net.*;
import java.io.*;

import org.xml.sax.*;

import java.text.*;
import java.util.*;

/**
 * Currency converter based on the European Central Bank's (ECB) foreign exchange rates.
 * <br><br>The published ECB rates contain exchange rates for approx. 35 of the world's major currencies.
 * <br>They are updated daily at 14:15 CET. These rates use EUR as reference currency and are specified with a
 * precision of 1/10000 of the currency unit (one hundredth cent).
 * <br>See: http://www.ecb.int/stats/exchange/eurofxref/html/index.en.html
 * <br>
 * <br>This converter is compatible with Java Desktop and Android.
 *  
 * @version 1.2 2013-12-16
 * @author Jean-Marc Astesana (based on an original code from <b>Thomas Knierim</br>)
 */
public class ECBCurrencyConverter extends AbstractXMLCurrencyConverter {
	private static final class ECBHandler extends CurrencyHandler {
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if ("Cube".equals(qName)) { //$NON-NLS-1$
				String date = attributes.getValue("time"); //$NON-NLS-1$
				if (date != null) {
					getData().setCurrencyRate("EUR", 10000L); //$NON-NLS-1$
					String[] ids = TimeZone.getAvailableIDs();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.US); //$NON-NLS-1$
					try {
						// A previous version used CET as time zone. Unfortunately this time zone was broken in Android 2.x
						// See http://code.google.com/p/android/issues/detail?id=14963
						getData().setReferenceDate(df.parse(date + " 13:15 GMT")); //$NON-NLS-1$
					} catch (ParseException e) {
						System.out.println (java.util.Arrays.asList(ids));
						throw new SAXException("Cannot parse reference date: " + date); //$NON-NLS-1$
					}
				}
				String currency = attributes.getValue("currency"); //$NON-NLS-1$
				String rate = attributes.getValue("rate"); //$NON-NLS-1$
				if (currency != null && rate != null) {
					try {
						getData().setCurrencyRate(currency, stringToLong(rate));
					} catch (Exception e) {
						throw new SAXException("Cannot parse exchange rate: " + rate + ". " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
	}

	private static final String ECB_RATES_URL = "http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml"; //$NON-NLS-1$
	
	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	public ECBCurrencyConverter(Proxy proxy, Cache cache) throws IOException, ParseException {
		super(proxy, cache);
	}

	@Override
	protected URL getSourceURL() {
		try {
			return new URL(ECB_RATES_URL);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected CurrencyHandler getXMLHandler() {
		return new ECBHandler();
	}
}

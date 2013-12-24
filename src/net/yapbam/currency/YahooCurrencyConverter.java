package net.yapbam.currency;

import java.net.*;
import java.io.*;

import net.yapbam.remote.Cache;

import org.xml.sax.*;

import java.text.*;

/**
 * Currency converter based on Yahoo's foreign exchange rates.
 * <br>
 * <br>This converter is compatible with Java Desktop and Android.
 *  
 * @version 1.0 2013-12-17
 * @author Jean-Marc Astesana
 */
public class YahooCurrencyConverter extends AbstractXMLCurrencyConverter {
	private static final String YAHOO_RATES_URL = "http://finance.yahoo.com/webservice/v1/symbols/allcurrencies/quote?format=xml"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	private static final String FIELD_TAG = "field"; //$NON-NLS-1$
	
	private enum Field {
		CURRENCY, RATE, TIME_STAMP
	}
	
	private static final class YahooHandler extends CurrencyHandler {
		private String currency;
		private long rate;
		private StringBuilder buffer;
		private Field field;
		private long maxTStamp;
		
		private YahooHandler() {
			this.buffer = new StringBuilder();
			maxTStamp = 0;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (FIELD_TAG.equals(qName) && "name".equals(attributes.getValue(NAME_ATTRIBUTE))) {
				this.field = Field.CURRENCY;
			} else if (FIELD_TAG.equals(qName) && "price".equals(attributes.getValue(NAME_ATTRIBUTE))) {
				this.field = Field.RATE;
			} else if (FIELD_TAG.equals(qName) && "ts".equals(attributes.getValue(NAME_ATTRIBUTE))) {
				this.field = Field.TIME_STAMP;
			} else {
				this.field = null;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if ("resource".equals(qName) && (currency!=null) && (rate!=0)) { //$NON-NLS-1$
				if (CountryCurrencyMap.INSTANCE.getCountries(currency)!=null) {
					// We ignore currencies that are used in no country.
					// They are technical "currencies" like XDR (Special drawing rights) or obsolete currencies
					getData().setCurrencyRate(currency, rate);
				}
			} else if (FIELD_TAG.equals(qName) && (field!=null)) {
				if (this.field.equals(Field.CURRENCY)) {
					int index = this.buffer.indexOf("/");
					if ((index>=0) && (this.buffer.length()==index+4)) {
						currency = this.buffer.substring(index+1);
					} else {
						currency = null;
					}
				} else if (this.field.equals(Field.RATE)) {
					this.rate = stringToLong(this.buffer.toString());
				} else if (this.field.equals(Field.TIME_STAMP) && (currency!=null) && (rate!=0)) {
					long tstamp = Long.parseLong(this.buffer.toString())*1000;
					if (tstamp>this.maxTStamp) {
						this.maxTStamp = tstamp;
					}
				}
				this.field = null;
				this.buffer.delete(0, this.buffer.length());
			}
		}
		
		public void characters(char[] buffer, int start, int length) {
			if (this.field!=null) {
				this.buffer.append(buffer, start, length);
			}
		}

		@Override
		public void endDocument() throws SAXException {
			getData().setReferenceDate(maxTStamp);
			super.endDocument();
		}
	}

	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	public YahooCurrencyConverter(Proxy proxy, Cache cache) throws IOException, ParseException {
		super(proxy, cache);
	}

	@Override
	protected URL getSourceURL() {
		try {
			return new URL(YAHOO_RATES_URL);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected CurrencyHandler getXMLHandler() {
		return new YahooHandler();
	}

	@Override
	protected boolean isDataExpired() {
		if (getTimeStamp() < 0) {
			return true;
		}
		long now = System.currentTimeMillis();
		// If we connect to server since less than one minute ... do nothing
		// This could happen if server doesn't refresh its rates since the last time we
		// updated the cache file (and more than the "standard" cache expiration time defined below)
		if (now - getLastRefreshTimeStamp() < 60000) {
			return false;
		}
		// Data is expired if older than 1 hour
		return now-getTimeStamp()>3600000;
	}
}

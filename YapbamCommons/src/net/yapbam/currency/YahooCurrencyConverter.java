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
				getData().setCurrencyRate(currency, rate);
			} else if ("field".equals(qName) && (field!=null)) {
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
}

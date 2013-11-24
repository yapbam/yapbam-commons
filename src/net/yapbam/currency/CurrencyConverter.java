/*
 * This class is a modified version of Thomas Knierim original Currency Converter.
 * Copyright (c) 2007 Thomas Knierim
 * http://www.thomasknierim.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.yapbam.currency;

import java.net.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.text.*;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Currency converter based on the European Central Bank's (ECB) foreign exchange rates.
 * <br><br>The published ECB rates contain exchange rates for approx. 35 of the world's major currencies.
 * <br>They are updated daily at 14:15 CET. These rates use EUR as reference currency and are specified with a
 * precision of 1/10000 of the currency unit (one hundredth cent).
 * <br>See: http://www.ecb.int/stats/exchange/eurofxref/html/index.en.html
 * <br>
 * <br>The <b>convert()</b> methods perform currency conversions using either double values or 64-bit long integer values.
 * <br>Long values should be preferred in order to avoid problems associated with floating point arithmetics.
 * <br><br>A local cache file is used for storing exchange rates to reduce network latency and allow offline mode.
 * <br>
 * <br>This converter is compatible with Java Desktop and Android.
 *  
 * @version 1.1 2013-05-02
 * @author Jean-Marc Astesana (based on an orginal code from <b>Thomas Knierim</br>)
 */
public class CurrencyConverter {
	private static final String ECB_RATES_URL = "http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml"; //$NON-NLS-1$
	
	/** A Cache.
	 * <br>This converter use cache in order to preserve ECB resources, and to be able to work with no Internet connection.
	 * <br>To improve the cache robustness, the cache may have (this is not mandatory) two levels:<ol>
	 * <li>A temporary cache that is used to store the data red from Internet.</li>
	 * <li>A persistent cache that saved the temporary after it was validated by a successful parsing (see commit method).</li></ol>
	 * @author Jean-Marc Astesana
	 */
	public interface Cache {
		/** Gets a writer to the temporary cache.
		 * @return A writer.
		 * @throws IOException if an error occurs while creating the writer. 
		 */
		public Writer getWriter() throws IOException;
		
		/** Gets a reader to the cache.
		 * @param tmp true if the temporary cache is required
		 * @return A reader.
		 * @throws IOException if an error occurs while creating the reader. 
		 */
		public Reader getReader(boolean tmp) throws IOException;
		
		/** Commits the temporary cache.
		 * <br>This method is called once temporary cache has been successfully red.
		 */
		public void commit();
	}
	
	private Proxy proxy = Proxy.NO_PROXY;
	private Cache cache;
	private Map<String, Long> fxRates = new HashMap<String, Long>(40);
	private Date referenceDate = null;
	private long lastTryCacheRefresh;
	private boolean isSynchronized;

	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	public CurrencyConverter(Proxy proxy, Cache cache) throws IOException, ParseException {
		this.proxy = proxy;
		this.cache = cache==null?new MemoryCache():cache;
		try {
			// Try to read the cache file
			parse(false);
			try {
				update();
			} catch (Exception e) {
				// Don't throw any exception if update fails as the instance is already initialized with the cache
				// isSynchronized method will return false, indicating that this instance is not synchronized with Internet
			}
		} catch (Exception e) {
			// Cache parsing failed, maybe cache file is not present or is corrupted. 
			// We will call update without try/catch clause to throw exceptions if data can't be red.
			this.update();
		}
	}

	/** Logs a message.
	 * <br>This method is used by the class to log events.
	 * <br>By default, it does nothing. You could override this method is subclasses in order to log messages at the appropriate place. 
	 * @param message
	 */
	protected void log(String message) {
		// Default implementation does nothing
	}
	
	/**
	 * Converts a double precision floating point value from one currency to
	 * another. Example: convert(29.95, "USD", "EUR") - converts $29.95 US Dollars
	 * to Euro.
	 * 
	 * @param amount
	 *          Amount of money (in source currency) to be converted.
	 * @param fromCurrency
	 *          Three letter ISO 4217 currency code of source currency.
	 * @param toCurrency
	 *          Three letter ISO 4217 currency code of target currency.
	 * @return Amount in target currency
	 * @throws IllegalArgumentException
	 *           If a wrong (non-existing) currency argument was supplied.
	 */
	public double convert(double amount, String fromCurrency, String toCurrency) throws IllegalArgumentException {
		if (checkCurrencyArgs(fromCurrency, toCurrency)) {
			amount *= fxRates.get(toCurrency);
			amount /= fxRates.get(fromCurrency);
		}
		return amount;
	}

	/**
	 * Converts a long value from one currency to another. Internally long values
	 * represent monetary amounts in 1/10000 of the currency unit, e.g. the long
	 * value 975573l represents 97.5573 (precision = four digits after comma).
	 * Using long values instead of floating point numbers prevents imprecision /
	 * calculation errors resulting from floating point arithmetics.
	 * 
	 * @param amount
	 *          Amount of money (in source currency) to be converted.
	 * @param fromCurrency
	 *          Three letter ISO 4217 currency code of source currency.
	 * @param toCurrency
	 *          Three letter ISO 4217 currency code of target currency.
	 * @return Amount in target currency
	 * @throws IllegalArgumentException
	 *           If a wrong (non-existing) currency argument was supplied.
	 */
	public long convert(long amount, String fromCurrency, String toCurrency) throws IllegalArgumentException {
		if (checkCurrencyArgs(fromCurrency, toCurrency)) {
			amount *= fxRates.get(toCurrency);
			amount /= fxRates.get(fromCurrency);
		}
		return amount;
	}

	/**
	 * Check whether currency arguments are valid and not equal.
	 * 
	 * @param fromCurrency
	 *          ISO 4217 source currency code.
	 * @param toCurrency
	 *          ISO 4217 target currency code.
	 * @return true if both currency arguments are not equal.
	 * @throws IllegalArgumentException
	 *           If a wrong (non-existing) currency argument was supplied.
	 */
	private boolean checkCurrencyArgs(String fromCurrency, String toCurrency) throws IllegalArgumentException {
		if (!fxRates.containsKey(fromCurrency)) {
			throw new IllegalArgumentException(fromCurrency + " currency is not available."); //$NON-NLS-1$
		}
		if (!fxRates.containsKey(toCurrency)) {
			throw new IllegalArgumentException(toCurrency + " currency is not available."); //$NON-NLS-1$
		}
		return (!fromCurrency.equals(toCurrency));
	}

	/**
	 * Check whether the exchange rate for a given currency is available.
	 * 
	 * @param currency
	 *          Three letter ISO 4217 currency code of source currency.
	 * @return True if exchange rate exists, false otherwise.
	 */
	public boolean isAvailable(String currency) {
		return (fxRates.containsKey(currency));
	}

	/**
	 * Returns currencies for which exchange rates are available.
	 * 
	 * @return String array with ISO 4217 currency codes.
	 */
	public String[] getCurrencies() {
		String[] currencies = fxRates.keySet().toArray(new String[fxRates.size()]);
		return currencies;
	}

	/**
	 * Gets the reference date for the exchange rates as a Java Date. The time part
	 * is always 14:15 Central European Time (CET).
	 * 
	 * @return Date for which currency exchange rates are valid, or null if the
	 *         data structure has not yet been initialized.
	 * 
	 */
	public Date getReferenceDate() {
		return referenceDate;
	}

	/** Tests whether this converter is synchronized with ECB.
	 * <br>When an error occurs while connecting to ECB, the converter is be created from cache data (if any exists).
	 * This allows offline usage of the converter.
	 * <br>Be aware that, in order to preserve ECB resources, update method does not call ECB if cache is not so old (see update method's comment).
	 * In such a case, this method returns true.
	 * @return true if the rates are up to date
	 * @see #update()
	 */
	public boolean isSynchronized() {
		return this.isSynchronized;
	}

	/**
	 * Makes the cache uptodate.
	 * <br>If it is not, downloads again cache file and parse data into internal data structure.
	 * @return true if the ECB was called. In order to preserve ECB resources, ECB is not called if cache is not so old (ECB refresh its rates never more
	 * than 1 time per day, we don't call ECB again if data is younger than 24 hours. There's also special handling of week-ends). In such a case, this method returns false.
	 * @throws IOException If cache file cannot be read/written or if URL cannot be opened.
	 * @throws ParseException If an error occurs while parsing the XML cache file.
	 */
	public boolean update() throws IOException, ParseException {
		boolean connect = cacheIsExpired();
		if (connect) {
			forceUpdate();
		}
		this.isSynchronized = true;
		return connect;
	}
	
	/**
	 * Forces the cache to be refreshed.
	 * <br>Always downloads again cache file and parse data into internal data structure.
	 * @throws IOException If cache file cannot be read/written or if URL cannot be opened.
	 * @throws ParseException If an error occurs while parsing the XML cache file.
	 */
	public void forceUpdate() throws IOException, ParseException {
		long start = System.currentTimeMillis();
		refreshCacheFile();
		log("refresh cache: "+Long.toString(System.currentTimeMillis()-start)+"ms");
		start = System.currentTimeMillis();
		parse(true);
		log("parse: "+Long.toString(System.currentTimeMillis()-start)+"ms");
		start = System.currentTimeMillis();
		cache.commit();
		log("commit: "+Long.toString(System.currentTimeMillis()-start)+"ms");
	}

	/**
	 * Checks whether XML cache file needs to be updated. The cache file is up to
	 * date for 24 hours after the reference date (plus a certain tolerance). On
	 * weekends, it is 72 hours because no rates are published during weekends.
	 * 
	 * @return true if cache file needs to be updated, false otherwise.
	 */
	private boolean cacheIsExpired() {
		if (referenceDate == null) {
			return true;
		}
		// If we connect to ECB since less than one minute ... do nothing
		// This could happen if ECB doesn't refresh its rates since the last time we
		// updated the cache file (and more than the "standard" cache expiration time - see below)
		if (System.currentTimeMillis() - lastTryCacheRefresh < 60000) {
			return false;
		}
		
		final int tolerance = 12;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		long hoursOld = (cal.getTimeInMillis() - referenceDate.getTime()) / (1000 * 60 * 60);
		cal.setTime(referenceDate);
		// hypothetical: rates are never published on Saturdays and Sunday
		int hoursValid = 24 + tolerance;
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
			hoursValid = 72;
		} else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			hoursValid = 48; 
		}

		return hoursOld > hoursValid;
	}

	/**
	 * (Re-) download the XML cache file and store it in a temporary location.
	 * 
	 * @throws IOException
	 *           If (1) URL cannot be opened, or (2) if cache file cannot be
	 *           opened, or (3) if a read/write error occurs.
	 */
	private void refreshCacheFile() throws IOException {
		String lastError = null;
		lastTryCacheRefresh = System.currentTimeMillis();
		try {
			log ("Connecting to ECB");
			HttpURLConnection ct = (HttpURLConnection) new URL(ECB_RATES_URL).openConnection(proxy);
			int errorCode = ct.getResponseCode();
			if (errorCode == HttpURLConnection.HTTP_OK) {
				InputStream in = ct.getInputStream();
				try {
					Writer out = cache.getWriter();
					try {
						for (int c=in.read() ; c!=-1; c=in.read()) {
							out.write(c);
						}
					} catch (IOException e) {
						lastError = "Read/Write Error: " + e.getMessage(); //$NON-NLS-1$
					} finally {
						out.flush();
						out.close();
					}
				} finally {
					in.close();
				}
			} else {
				throw new IOException("Http Error " + errorCode); //$NON-NLS-1$
			}
		} catch (IOException e) {
			lastError = "Connection/Open Error: " + e.getMessage(); //$NON-NLS-1$
		}
		if (lastError != null) {
			throw new IOException(lastError);
		}
	}

	/**
	 * Convert a numeric string to a long value with a precision of four digits
	 * after the decimal point without rounding. E.g. "123.456789" becomes
	 * 1234567l.
	 * 
	 * @param str
	 *          Positive numeric string expression.
	 * @return Value representing 1/10000th of a currency unit.
	 * @throws NumberFormatException
	 *           If "str" argument is not numeric.
	 */
	private long stringToLong(String str) throws NumberFormatException {
		int decimalPoint = str.indexOf('.');
		String wholePart = ""; //$NON-NLS-1$
		String fractionPart = ""; //$NON-NLS-1$
		if (decimalPoint > -1) {
			if (decimalPoint > 0) {
				wholePart = str.substring(0, decimalPoint);
			}
			fractionPart = str.substring(decimalPoint + 1);
			String padString = "0000"; //$NON-NLS-1$
			int padLength = 4 - fractionPart.length();
			if (padLength > 0) {
				fractionPart += padString.substring(0, padLength);
			} else if (padLength < 0) {
				fractionPart = fractionPart.substring(0, 4);
			}
		} else {
			wholePart = str;
			fractionPart = "0000"; //$NON-NLS-1$
		}
		return (Long.parseLong(wholePart + fractionPart));
	}

	/**
	 * Parses XML cache file and create internal data structures containing exchange rates.
	 * @param tmp true to parse the tmp cache, false to parse the official cache
	 * @throws ParseException If XML file cannot be parsed.
	 * @throws IOException if connection to ECB or writing cache file fails.
	 * @see Cache
	 */
	private void parse(boolean tmp) throws ParseException, IOException {
		fxRates.clear();
		fxRates.put("EUR", 10000L); //$NON-NLS-1$
		DefaultHandler handler = new DefaultHandler() {
			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				if ("Cube".equals(qName)) { //$NON-NLS-1$
					String date = attributes.getValue("time"); //$NON-NLS-1$
					if (date != null) {
						String[] ids = TimeZone.getAvailableIDs();
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.US); //$NON-NLS-1$
						try {
							// A previous version used CET as time zone. Unfortunately this time zone was broken in Android 2.x
							// See http://code.google.com/p/android/issues/detail?id=14963
							referenceDate = df.parse(date + " 13:15 GMT"); //$NON-NLS-1$
						} catch (ParseException e) {
							System.out.println (java.util.Arrays.asList(ids));
							throw new SAXException("Cannot parse reference date: " + date); //$NON-NLS-1$
						}
					}
					String currency = attributes.getValue("currency"); //$NON-NLS-1$
					String rate = attributes.getValue("rate"); //$NON-NLS-1$
					if (currency != null && rate != null) {
						try {
							fxRates.put(currency, stringToLong(rate));
						} catch (Exception e) {
							throw new SAXException("Cannot parse exchange rate: " + rate + ". " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		};
		try {
			XMLReader saxReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			saxReader.setContentHandler(handler);
			saxReader.setErrorHandler(handler);
			Reader input = cache.getReader(tmp);
			try {
				saxReader.parse(new InputSource(input));
			} finally {
				input.close();
			}
		} catch (SAXException e) {
			throw new ParseException(e.toString(), 0);
		} catch (ParserConfigurationException e) {
			throw new ParseException(e.toString(), 0);
		}
	}
}

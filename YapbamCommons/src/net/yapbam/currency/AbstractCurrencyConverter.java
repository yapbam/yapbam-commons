/*
 * This class was inspired by Thomas Knierim's original Currency Converter.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.*;
import java.util.*;

/**
 * Currency converter based on an Internet foreign exchange rates source.
 * <br>
 * <br>The <b>convert()</b> methods perform currency conversions using either double values or 64-bit long integer values.
 * <br>Long values should be preferred in order to avoid problems associated with floating point arithmetics.
 * <br><br>A local cache file is used for storing exchange rates to reduce network latency and allow offline mode.
 * <br>
 * <br>This converter is compatible with Java Desktop and Android.
 *  
 * @version 1.0 2013-12-16
 * @author Jean-Marc Astesana (based on an original code from <b>Thomas Knierim</br>)
 */
public abstract class AbstractCurrencyConverter {
	private static final String CURRENCY_IS_NOT_AVAILABLE = "{0} currency is not available."; //$NON-NLS-1$

	/** A Cache.
	 * <br>This converter use cache in order to preserve web server resources, and to be able to work with no Internet connection.
	 * <br>To improve the cache robustness, the cache may have (this is not mandatory) two levels:<ol>
	 * <li>A temporary cache that is used to store the data red from Internet.</li>
	 * <li>A persistent cache that saved the temporary after it was validated by a successful parsing (see commit method).</li></ol>
	 * @author Jean-Marc Astesana
	 */
	public interface Cache {
		public boolean isEmpty();
		
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
		 * <br>This method is called once temporary cache has been successfully red and parsed.
		 */
		public void commit();
	}
	
	private Logger logger;
	private Proxy proxy;
	private Cache cache;
	private Map<String, Long> fxRates;
	private Date referenceDate;
	private long lastTryCacheRefresh;
	private boolean isSynchronized;

	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	protected AbstractCurrencyConverter(Proxy proxy, Cache cache) throws IOException, ParseException {
		this.proxy = proxy;
		this.fxRates = new HashMap<String, Long>();
		this.cache = cache==null?new MemoryCache():cache;
		this.referenceDate = null;
		boolean cacheUnavailable = this.cache.isEmpty();
		if (!cacheUnavailable) {
			getLogger().trace("cache is available");
			try {
				// Try to read the cache file
				parse(cache, false);
			} catch (Exception e) {
				// Cache parsing failed, maybe cache file is not present or is corrupted. 
				// We will call update without try/catch clause to throw exceptions if data can't be red.
				getLogger().warn("Parse failed", e);
				cacheUnavailable = true;
			}
		} else {
			getLogger().trace("cache is unavailable");
		}
		try {
			// If cache was not read update it.
			this.update();
		} catch (IOException e) {
			processException(cacheUnavailable, e);
		} catch (ParseException e) {
			processException(cacheUnavailable, e);
		}
	}

	private  <T extends Exception> void processException(boolean cacheUnavailable, T e) throws T {
		if (cacheUnavailable) {
			// Don't throw any exception if update fails as the instance is already initialized with the cache
			// isSynchronized method will return false, indicating that this instance is not synchronized with Internet
			getLogger().warn("Update failed", e);
		} else {
			throw e;
		}
	}

	/** Gets a logger.
	 * <br>This logger is used by the class to log events.
	 */
	protected Logger getLogger() {
		if (logger==null) {
			logger = LoggerFactory.getLogger(getClass());
		}
		return logger;
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
	public double convert(double amount, String fromCurrency, String toCurrency) {
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
	public long convert(long amount, String fromCurrency, String toCurrency) {
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
	private boolean checkCurrencyArgs(String fromCurrency, String toCurrency) {
		if (!fxRates.containsKey(fromCurrency)) {
			throw new IllegalArgumentException(MessageFormat.format(CURRENCY_IS_NOT_AVAILABLE, fromCurrency));
		}
		if (!fxRates.containsKey(toCurrency)) {
			throw new IllegalArgumentException(MessageFormat.format(CURRENCY_IS_NOT_AVAILABLE, toCurrency));
		}
		return !fromCurrency.equals(toCurrency);
	}

	/**
	 * Check whether the exchange rate for a given currency is available.
	 * 
	 * @param currency
	 *          Three letter ISO 4217 currency code of source currency.
	 * @return True if exchange rate exists, false otherwise.
	 */
	public boolean isAvailable(String currency) {
		return fxRates.containsKey(currency);
	}

	/**
	 * Returns currencies for which exchange rates are available.
	 * 
	 * @return String array with ISO 4217 currency codes.
	 */
	public String[] getCurrencies() {
		return fxRates.keySet().toArray(new String[fxRates.size()]);
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

	/** Tests whether this converter is synchronized with web server.
	 * @return true if the rates are up to date
	 * @see #update()
	 */
	public boolean isSynchronized() {
		return this.isSynchronized;
	}

	/**
	 * Makes the cache uptodate.
	 * <br>If it is not, downloads again cache file and parse data into internal data structure.
	 * @return true if the web server was called. In order to preserve server resources, it is not called if cache is not so old (ECB refresh its rates never more
	 * than 1 time per day, we don't call ECB again if data is younger than 24 hours. There's also special handling of week-ends). In such a case, this method returns false.
	 * @throws IOException If cache file cannot be read/written or if URL cannot be opened.
	 * @throws ParseException If an error occurs while parsing the XML cache file.
	 */
	public boolean update() throws IOException, ParseException {
		//TODO Review method comment (remove references to ECB). Probably cacheIsExpired should be overridable.
		boolean connect = isCacheExpired();
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
		getLogger().debug("refresh cache: {}ms",Long.toString(System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		parse(cache, true);
		getLogger().debug("parse: {}ms",Long.toString(System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		cache.commit();
		getLogger().debug("commit: {}ms",Long.toString(System.currentTimeMillis()-start));
	}

	/**
	 * Checks whether XML cache file needs to be updated. The cache file is up to
	 * date for 24 hours after the reference date (plus a certain tolerance). On
	 * weekends, it is 72 hours because no rates are published during weekends.
	 * 
	 * @return true if cache file needs to be updated, false otherwise.
	 */
	private boolean isCacheExpired() {
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
	
	protected abstract URL getSourceURL(); 

	/**
	 * (Re-) download the XML cache file and store it in a temporary location.
	 * 
	 * @throws IOException
	 *           If (1) URL cannot be opened, or (2) if cache file cannot be
	 *           opened, or (3) if a read/write error occurs.
	 */
	private void refreshCacheFile() throws IOException {
		lastTryCacheRefresh = System.currentTimeMillis();
		getLogger().debug("Connecting to ECB");
		HttpURLConnection ct = (HttpURLConnection) getSourceURL().openConnection(proxy);
		int errorCode = ct.getResponseCode();
		if (errorCode == HttpURLConnection.HTTP_OK) {
			InputStream in = ct.getInputStream();
			try {
				Writer out = cache.getWriter();
				try {
					for (int c=in.read() ; c!=-1; c=in.read()) {
						out.write(c);
					}
				} finally {
					out.flush();
					out.close();
				}
			} finally {
				in.close();
			}
		} else {
			throw new IOException(MessageFormat.format("Http Error {1} when opening {0}", getSourceURL(), errorCode)); //$NON-NLS-1$
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
	public static long stringToLong(String str) {
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
		return Long.parseLong(wholePart + fractionPart);
	}
	
	/**
	 * Parses cache file and create internal data structures containing exchange rates.
	 * @param tmp true to parse the tmp cache, false to parse the official cache
	 * @throws ParseException If XML file cannot be parsed.
	 * @throws IOException if connection to the URL or writing to cache file fails.
	 * @see Cache
	 */
	protected abstract void parse(Cache cache, boolean tmp) throws ParseException, IOException;
	
	protected void setReferenceDate(Date date) {
		this.referenceDate = date;
	}
	
	protected void clearRates() {
		fxRates.clear();
	}

	protected void setCurrencyRate(String isoCode, long rate) {
		fxRates.put(isoCode, rate);
	}
}

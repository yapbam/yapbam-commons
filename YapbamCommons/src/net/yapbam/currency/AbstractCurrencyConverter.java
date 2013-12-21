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

import net.yapbam.remote.AbstractRemoteResource;
import net.yapbam.remote.Cache;
import net.yapbam.remote.MemoryCache;

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
public abstract class AbstractCurrencyConverter extends AbstractRemoteResource<CurrencyData> {

	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	protected AbstractCurrencyConverter(Proxy proxy, Cache cache) throws IOException, ParseException {
		super (proxy, cache);
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
		return getData().convert(amount, fromCurrency, toCurrency);
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
		return getData().convert(amount, fromCurrency, toCurrency);
	}

	/**
	 * Check whether the exchange rate for a given currency is available.
	 * 
	 * @param currency
	 *          Three letter ISO 4217 currency code of source currency.
	 * @return True if exchange rate exists, false otherwise.
	 */
	public boolean isAvailable(String currency) {
		return getData().isAvailable(currency);
	}

	/**
	 * Returns currencies for which exchange rates are available.
	 * 
	 * @return String array with ISO 4217 currency codes.
	 */
	public String[] getCurrencies() {
		return getData().getCurrencies();
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
	 * @return 
	 * @throws ParseException If XML file cannot be parsed.
	 * @throws IOException if connection to the URL or writing to cache file fails.
	 * @see Cache
	 */
	protected abstract CurrencyData parse(Cache cache, boolean tmp) throws ParseException, IOException;
}

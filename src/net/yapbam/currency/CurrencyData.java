package net.yapbam.currency;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.yapbam.remote.RemoteData;

public class CurrencyData implements RemoteData{
	private static final String CURRENCY_IS_NOT_AVAILABLE = "{0} currency is not available."; //$NON-NLS-1$

	private long timeStamp;
	private Map<String, Long> rates;
	private boolean locked;
	
	public CurrencyData() {
		super();
		this.timeStamp = -1;
		this.rates = new HashMap<String, Long>();
	}

	public void setReferenceDate(long date) {
		if (locked) {
			throw new UnsupportedOperationException();
		}
		this.timeStamp = date;
	}

	public void setCurrencyRate(String isoCode, long rate) {
		rates.put(isoCode, rate);
	}
	
	public void lock() {
		this.rates = Collections.unmodifiableMap(this.rates);
		this.locked = true;
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
		if (!rates.containsKey(fromCurrency)) {
			throw new IllegalArgumentException(MessageFormat.format(CURRENCY_IS_NOT_AVAILABLE, fromCurrency));
		}
		if (!rates.containsKey(toCurrency)) {
			throw new IllegalArgumentException(MessageFormat.format(CURRENCY_IS_NOT_AVAILABLE, toCurrency));
		}
		return !fromCurrency.equals(toCurrency);
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
			amount *= rates.get(toCurrency);
			amount /= rates.get(fromCurrency);
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
			amount *= rates.get(toCurrency);
			amount /= rates.get(fromCurrency);
		}
		return amount;
	}

	/**
	 * Check whether the exchange rate for a given currency is available.
	 * 
	 * @param currency
	 *          Three letter ISO 4217 currency code of source currency.
	 * @return True if exchange rate exists, false otherwise.
	 */
	public boolean isAvailable(String currency) {
		return rates.containsKey(currency);
	}

	/**
	 * Returns currencies for which exchange rates are available.
	 * 
	 * @return String array with ISO 4217 currency codes.
	 */
	public String[] getCurrencies() {
		return rates.keySet().toArray(new String[rates.size()]);
	}

	@Override
	public long getTimeStamp() {
		return this.timeStamp;
	}
}

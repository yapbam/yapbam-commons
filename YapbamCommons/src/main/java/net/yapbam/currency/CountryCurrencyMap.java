package net.yapbam.currency;

import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

/** A map between countries and currencies.
 * <br>This map is based on java internal data.
 * <br>This means that its content is related to the java version used, not to an "always uptodate" Internet source.
 * @author Jean-Marc Astesana (License GPL)
 */
public class CountryCurrencyMap {
	public static final CountryCurrencyMap INSTANCE = new CountryCurrencyMap();
	
	/* The following information is related to an online source of data.
	 * After thinking about it, it seems a better solution to use internal java data.
	 * The pro are: The data is guaranteed to be available (it is part of the JVM).
	 * The cons are; The data is not necessary upto date.
	private static final int CURRENCY_COL_INDEX = 14;
	private static final int COUNTRY_COL_INDEX = 2;
	private static final String TABLE_URL = "https://raw.github.com/datasets/country-codes/master/data/country-codes.csv";
	 */

	private Map<String,String> countryToCurrency;
	private Map<String,Set<String>> currencyToCountries;

	private CountryCurrencyMap() {
		this.countryToCurrency = new HashMap<String, String>();
		this.currencyToCountries = new HashMap<String, Set<String>>();
		String[] isoCountries = Locale.getISOCountries();
		for (String isoCountry : isoCountries) {
			Currency currency = null;
			try {
				currency = Currency.getInstance(new Locale("", isoCountry));
			} catch (IllegalArgumentException e) {
				// Google is not able to copy API with no mistake. That funny boys find nothing more stupid
				// than return non ISO 3166 countries in Locale.getIsoCountries(). So that strange code is
				// just to have no error on Android.
				LoggerFactory.getLogger(getClass()).warn("Fucking country returned by Locale.getISOCountries(): {}",isoCountry);
			}
			if (currency==null) {
				this.countryToCurrency.put(isoCountry, null);
			} else {
				String code = currency.getCurrencyCode();
				this.countryToCurrency.put(isoCountry, code);
				Set<String> countries = currencyToCountries.get(code);
				if (countries==null) {
					countries = new TreeSet<String>();
					currencyToCountries.put(code, countries);
				}
				countries.add(isoCountry);
			}
		}
	}

	/** Gets the currency used in a country.
	 * @param country The ISO 3166 code of a country (example: FR).
	 * @return The ISO 4217 code of the currency, or null if the country is unknown or has no currency.
	 */
	public String getCurrency(String country) {
		return this.countryToCurrency.get(country);
	}
	
	/** Gets the countries that use a currency.
	 * @param currencyCode The ISO 4217 code of the currency (example: EUR).
	 * @return An unmodifiable set of ISO 3166 country codes, or null if the currency is unknown.
	 */
	public Set<String> getCountries(String currencyCode) {
		Set<String> set = this.currencyToCountries.get(currencyCode);
		return set==null ? set : Collections.unmodifiableSet(set);
	}

	/** Gets the known countries.
	 * @return An unmodifiable set of ISO 3166 country codes.
	 */
	public Set<String> getCountries() {
		return Collections.unmodifiableSet(this.countryToCurrency.keySet());
	}
	
	/** Gets the known currencies.
	 * @return An unmodifiable set of ISO 4217 currency codes.
	 */
	public Set<String> getCurrencies() {
		return Collections.unmodifiableSet(this.currencyToCountries.keySet());
	}
}

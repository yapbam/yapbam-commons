package net.yapbam.currency;

import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CountryCurrencyMap {
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

	public CountryCurrencyMap() {
		this.countryToCurrency = new HashMap<String, String>();
		this.currencyToCountries = new HashMap<String, Set<String>>();
		String[] isoCountries = Locale.getISOCountries();
		for (String isoCountry : isoCountries) {
			Currency currency = Currency.getInstance(new Locale("", isoCountry));
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

	public String getCurrency(String country) {
		return this.countryToCurrency.get(country);
	}
	
	public Set<String> getCountries(String currencyCode) {
		return Collections.unmodifiableSet(this.currencyToCountries.get(currencyCode));
	}

	public Set<String> getCountries() {
		return Collections.unmodifiableSet(this.countryToCurrency.keySet());
	}
	
	public Set<String> getCurrencies() {
		return Collections.unmodifiableSet(this.currencyToCountries.keySet());
	}
}

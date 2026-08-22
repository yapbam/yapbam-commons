package net.yapbam.currency;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;

import net.yapbam.util.TextUtils;

public class CurrencyNames {
	private static final String BUNDLE_NAME = "/net/yapbam/currency/currencyNames"; //$NON-NLS-1$
	private static final String RESOURCE_SUFFIX = ".properties";

	private static Properties defaultResourceBundle;
	private static Properties resourceBundle;
	private static Locale resourceBundleLocale;

	private CurrencyNames() {
	}

	/** Gets the wording of a currency.
	 * <br>The wording is first searched in the current locale bundle. If not found, it is searched in the java Currency class. If not found, it is searched in the default bundle. If not found, the key itself is returned.
	 * @param key The ISO-4217 currency code.
	 * @return The currency name or the key itself if the name is unknown.
	 */
	public static String get(String key) {
		reset();
		// Search in the current locale bundle
		String wording = (String) resourceBundle.get(key);
		// If not found search in java Currency class
		wording = wording == null ? getJavaDisplayName(key) : wording;
		// If not found, search in the default bundle
		wording = wording == null ? (String) defaultResourceBundle.get(key) : wording;
		// If found nowhere, return the key itself
		return wording==null ? key : wording;
	}

	static String getJavaDisplayName(String key) {
		try {
			Currency currency = Currency.getInstance(key);
			Locale locale = Locale.getDefault();
			return TextUtils.capitalizeFirst(currency.getDisplayName(locale), locale);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static void reset() {
		// Load default resource bundle if not already loaded
		if (defaultResourceBundle == null) {
			defaultResourceBundle = new Properties();
			try {
				tryLoading(defaultResourceBundle, BUNDLE_NAME + RESOURCE_SUFFIX);
			} catch (IOException e) {
				throw new MissingResourceException("", "", BUNDLE_NAME);
			}
		}
		if (!Locale.getDefault().equals(resourceBundleLocale)) {
			Properties properties = new Properties();
			String lang = Locale.getDefault().getLanguage();
			try {
				tryLoading(properties, BUNDLE_NAME+"_"+lang+RESOURCE_SUFFIX);
			} catch (IOException e) {
				// Ignore, get will fallback to default
			}
			resourceBundle = properties;
			resourceBundleLocale = Locale.getDefault();
		}
	}
	
	private static void tryLoading(Properties properties, String name) throws IOException {
		InputStream stream = CurrencyNames.class.getResourceAsStream(name);
		if (stream==null) {
			throw new IOException("Resource not found: " + name);
		}
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.ISO_8859_1)) {
			properties.load(reader);
		} finally {
			stream.close();
		}
	}
}

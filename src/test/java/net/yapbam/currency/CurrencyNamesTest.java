package net.yapbam.currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yapbam.remote.MemoryCache;
import net.yapbam.util.TextUtils;

class CurrencyNamesTest {
	private static Locale locale;
	
	@BeforeAll
	static void setUpBeforeClass() {
		locale = Locale.getDefault();
		Locale.setDefault(Locale.FRANCE);
	}

	@AfterAll
	static void tearDownAfterClass() {
		Locale.setDefault(locale);
	}

	@Test
	void test() {
		// Warning: this test uses the currencyNames.properties file from src/test/resources not the one from src/main/resources
		try (MockedStatic<CurrencyNames> mockedCurrencyNames = mockStatic(CurrencyNames.class, CALLS_REAL_METHODS)) {
			mockedCurrencyNames.when(() -> CurrencyNames.getJavaDisplayName(anyString())).thenReturn(null);
			
			assertEquals("XXXX", CurrencyNames.get("XXXX"), "CurrencyNames.get on unknown currency code should return the currency code");

			mockedCurrencyNames.when(() -> CurrencyNames.getJavaDisplayName("BOTH")).thenReturn("BothFromJava");
			assertEquals("Les deux", CurrencyNames.get("BOTH"), "CurrencyNames.get should return language specific resource wording when it exists");

			assertEquals("Default", CurrencyNames.get("DEFAULT"), "CurrencyNames.get should return default resource wording when it is the only that exists exists");

			mockedCurrencyNames.when(() -> CurrencyNames.getJavaDisplayName("DEFAULT")).thenReturn("DefaultFromJava");
			assertEquals("DefaultFromJava", CurrencyNames.get("DEFAULT"), "CurrencyNames.get should return java wording before default resource wording");
		}
	}

	@Test
	void testGetJavaDisplayName() {
		// Test with a known currency code
		String displayName = CurrencyNames.getJavaDisplayName("EUR");
		assertNotNull(displayName, "Java display name should not be null for EUR");
		assertFalse(displayName.isEmpty(), "Java display name should not be empty for EUR currency code");
		
		// Test with an unknown currency code
		String unknownDisplayName = CurrencyNames.getJavaDisplayName("UNKNOWN");
		assertNull(unknownDisplayName, "Java display name should be null for UNKNOWN currency code");
	}

	@Test
	void checkUsage() throws IOException, ParseException {
		Set<String> usefull = new HashSet<>();
		Proxy proxy = Proxy.NO_PROXY;
		usefull.addAll(getUsedCurrencies(new ECBCurrencyConverter(proxy, new MemoryCache())));
		usefull.addAll(getUsedCurrencies(new FrankfurterCurrencyConverter(proxy, new MemoryCache())));
		assertDoesNotThrow(() -> checkUsageForLanguage(usefull, null));
	}

	private void checkUsageForLanguage(Set<String> usefull, String language) throws IOException {
		Logger logger = LoggerFactory.getLogger(CurrencyNamesTest.class);
		Properties properties = getKnownCodes(language);
		for (String code : properties.stringPropertyNames()) {
			if (!usefull.contains(code)) {
				logger.warn("Useless currency code {} for {} language -> {}", code, language==null?"default":language, properties.getProperty(code));
			}
		}
		Map<String, List<String>> map = new HashMap<>();
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			map.computeIfAbsent((String) entry.getValue(), k -> new ArrayList<>()).add((String) entry.getKey());
		}
		map.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
			List<String> canBeRemoved = e.getValue().stream().filter(code -> !usefull.contains(code)).collect(Collectors.toList());
			if (canBeRemoved.size()>=e.getValue().size()-1) {
				logger.info("Duplicate currency name '{}' for codes: {}. {}", e.getKey(), e.getValue(), uselessWording(canBeRemoved));
			} else {
				logger.warn("Duplicate currency name '{}' for codes: {}. {}", e.getKey(), e.getValue(), uselessWording(canBeRemoved));
			}
			Locale currentLocale = language == null ? Locale.US : Locale.forLanguageTag(language);
			logger.info("Java wordings: {}", getJavaWordings(e.getValue(), currentLocale));
		});
	}

	private String uselessWording(List<String> uselessCodes) {
		String count;
		if (uselessCodes.isEmpty()) {
			count = "None of them";
		} else {
			count = uselessCodes.toString();
		}
		return count + " " + (uselessCodes.size() == 1 ? "is" : "are") + " useless and can be removed";
	}

	private Map<String, String> getJavaWordings(List<String> codes, Locale locale) {
		return codes.stream().collect(Collectors.toMap(code -> code, code -> getJavaDisplayName(code, locale)));
	}
	private String getJavaDisplayName(String key, Locale locale) {
		try {
			Currency currency = Currency.getInstance(key);
			return TextUtils.capitalizeFirst(currency.getDisplayName(locale), locale);
		} catch (IllegalArgumentException e) {
			return "?";
		}
	}

	private Collection<String> getUsedCurrencies(AbstractCurrencyConverter converter) throws IOException, ParseException {
		converter.update();
		return Arrays.asList(converter.getCurrencies());
	}

	private Properties getKnownCodes(String language) throws IOException {
		String id = language == null ? "" : "_" + language;
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/net/yapbam/currency/currencyNames"+id+".properties"), StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(reader);
			return properties;
		}
	}
}

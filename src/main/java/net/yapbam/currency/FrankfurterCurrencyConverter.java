package net.yapbam.currency;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.yapbam.remote.Cache;
import net.yapbam.remote.MemoryCache;

public class FrankfurterCurrencyConverter extends AbstractCurrencyConverter {
    private static final String RATES_URL = "https://api.frankfurter.dev/v2/rates?base=EUR"; //$NON-NLS-1$


    public FrankfurterCurrencyConverter(Proxy proxy, Cache cache) {
        super(proxy, cache);
    }

    @Override
    protected URL getSourceURL() {
        try {
            return new URL(RATES_URL);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected CurrencyData parse(Cache cache, boolean tmp) throws ParseException, IOException {
        if (!tmp && cache.isEmpty()) {
            return new CurrencyData();
        }
        try (Reader reader = new InputStreamReader(cache.getInputStream(tmp), StandardCharsets.UTF_8)) {
            Object obj = new JSONParser().parse(reader);
            if (!(obj instanceof JSONArray)) {
                throw new ParseException("Unexpected response format (should be a json array)", 0);
            }
            CurrencyData data = new CurrencyData();
            data.setCurrencyRate("EUR", 10000L); //$NON-NLS-1$
            long timestamp = 0;
            for (Object item : (JSONArray) obj) {
                timestamp = Math.max(timestamp, parseQuote(data, item));
            }
            data.setReferenceDate(timestamp);
            return data;
        } catch (org.json.simple.parser.ParseException e) {
            throw new ParseException(e.getMessage(), e.getPosition());
        }
    }

    /**
     * Parses a quote item and returns the date as milliseconds since epoch.
     * @param item The JSON object containing the quote data
     * @return The date as milliseconds since epoch
     * @throws ParseException if the date cannot be parsed
     */
    private long parseQuote(CurrencyData data, Object item) throws ParseException {
        if (!(item instanceof JSONObject)) {
            throw new ParseException("Expected JSON object for quote item", 0);
        }
        
        JSONObject jsonItem = (JSONObject) item;
        Object dateObj = jsonItem.get("date");
        
        if (dateObj == null) {
            throw new ParseException("Missing date attribute in quote item", 0);
        }
        
        String dateStr = dateObj.toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date date = dateFormat.parse(dateStr);
        
        // Parse the quote and rate attributes
        Object quoteObj = jsonItem.get("quote");
        if (quoteObj == null) {
            throw new ParseException("Missing quote attribute in quote item", 0);
        }
        Object rateObj = jsonItem.get("rate");
        if (rateObj == null) {
            throw new ParseException("Missing rate attribute in quote item", 0);
        }
        
        String currency = quoteObj.toString();
        try {
            double rate = Double.parseDouble(rateObj.toString());
            long rateAsLong = Math.round(rate * 10000L);
            data.setCurrencyRate(currency, rateAsLong);
        } catch (NumberFormatException e) {
            throw new ParseException("Cannot parse rate: " + rateObj, 0);
        }
        
        return date.getTime();
    }

    public static void main(String[] args) throws ParseException, IOException {
        AbstractCurrencyConverter converter = new FrankfurterCurrencyConverter(Proxy.NO_PROXY, new MemoryCache());
        converter.update();
        // System.out.println("TimeStamp: "+converter.getTimeStamp());
        // System.out.println("Currencies: "+Arrays.asList(converter.getCurrencies()));
        // System.out.println("1 euro= "+converter.convert(100, "EUR", "USD")/100.0+" USD");
    }
}

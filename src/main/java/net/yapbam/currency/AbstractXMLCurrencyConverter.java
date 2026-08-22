package net.yapbam.currency;

import java.net.*;
import java.io.*;

import net.yapbam.remote.Cache;

import org.xml.sax.*;

import java.text.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Currency converter based on a XML Internet foreign exchange rates source.
 * <br>
 * <br>This converter is compatible with Java Desktop and Android.
 *  
 * @version 1.0 2013-12-16
 * @author Jean-Marc Astesana
 */
public abstract class AbstractXMLCurrencyConverter extends AbstractCurrencyConverter {
	private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 */
	protected AbstractXMLCurrencyConverter(Proxy proxy, Cache cache) {
		super(proxy, cache);
	}
	
	@Override
	protected CurrencyData parse(Cache cache, boolean tmp) throws ParseException, IOException {
		if (!tmp && cache.isEmpty()) {
			return new CurrencyData();
		} else {
			return parseXML(cache, tmp);
		}
	}

	/**
	 * Parses XML cache file and create internal data structures containing exchange rates.
	 * @param cache The cache in which to read data
	 * @param tmp true to parse the tmp cache, false to parse the official cache
	 * @return The CurrencyData
	 * @throws ParseException If XML file cannot be parsed.
	 * @throws IOException if connection to ECB or writing cache file fails.
	 * @see Cache
	 */
	protected CurrencyData parseXML(Cache cache, boolean tmp) throws ParseException, IOException {
		CurrencyHandler handler = getXMLHandler();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Prevent XXE attack by disabling DOCTYPE declarations
			factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
			XMLReader saxReader = factory.newSAXParser().getXMLReader();
			saxReader.setContentHandler(handler);
			saxReader.setErrorHandler(handler);
			try (InputStream input = cache.getInputStream(tmp)) {
				saxReader.parse(new InputSource(input));
			}
		} catch (SAXException e) {
			ParseException x = new ParseException(e.toString(), 0);
			x.initCause(e);
			throw x;
		} catch (ParserConfigurationException e) {
			throw new ParseException(e.toString(), 0);
		}
		return handler.getData();
	}

	protected abstract CurrencyHandler getXMLHandler();
}

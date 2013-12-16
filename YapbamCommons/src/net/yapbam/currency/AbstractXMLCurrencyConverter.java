package net.yapbam.currency;

import java.net.*;
import java.io.*;

import net.yapbam.currency.AbstractCurrencyConverter.Cache;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

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

	/**
	 * Constructor.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	protected AbstractXMLCurrencyConverter(Proxy proxy, Cache cache) throws IOException, ParseException {
		super(proxy, cache);
	}
	
	@Override
	protected void parse(Cache cache, boolean tmp) throws ParseException, IOException {
		clearRates();
		parseXML(cache, tmp);
	}

	/**
	 * Parses XML cache file and create internal data structures containing exchange rates.
	 * @param tmp true to parse the tmp cache, false to parse the official cache
	 * @throws ParseException If XML file cannot be parsed.
	 * @throws IOException if connection to ECB or writing cache file fails.
	 * @see Cache
	 */
	protected void parseXML(Cache cache, boolean tmp) throws ParseException, IOException {
		DefaultHandler handler = getXMLHandler();
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

	protected abstract DefaultHandler getXMLHandler();
}

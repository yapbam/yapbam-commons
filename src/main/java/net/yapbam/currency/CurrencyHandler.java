package net.yapbam.currency;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class CurrencyHandler extends DefaultHandler {
	private CurrencyData data;
	
	protected CurrencyHandler() {
	}
	
	@Override
	public void startDocument() throws SAXException {
		this.data = new CurrencyData();
	}

	@Override
	public void endDocument() throws SAXException {
		this.data.lock();
	}
 
	public CurrencyData getData() {
		return this.data;
	}
}

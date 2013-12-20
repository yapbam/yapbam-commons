package net.yapbam.currency;

import org.xml.sax.helpers.DefaultHandler;

public class CurrencyHandler extends DefaultHandler {
	private CurrencyData data;
	
	public CurrencyHandler() {
		this.data = new CurrencyData();
	}
	
	public CurrencyData getData() {
		return this.data;
	}
}

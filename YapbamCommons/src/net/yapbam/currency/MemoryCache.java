package net.yapbam.currency;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import net.yapbam.currency.CurrencyConverter.Cache;

/** A fake cache class that stores data into memory.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
public class MemoryCache implements Cache {
	private CharArrayWriter writer;
	private char[] charArrays;

	@Override
	public Writer getWriter() throws IOException {
		writer = new CharArrayWriter();
		return writer;
	}

	@Override
	public Reader getReader(boolean tmp) throws IOException {
		if (charArrays==null) {
			charArrays = writer==null?new char[0]:writer.toCharArray();
		}
		return new CharArrayReader(charArrays);
	}

	@Override
	public void commit() {
		// Force re-creation of the charArrays field
		this.charArrays = null;
	}
}

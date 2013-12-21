package net.yapbam.remote;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** A fake cache class that stores data into memory.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
public class MemoryCache implements Cache {
	private CharArrayWriter writer;
	private char[] charArrays;

	@Override
	public Writer getWriter() throws IOException {
		this.writer = new CharArrayWriter();
		return writer;
	}

	@Override
	public Reader getReader(boolean tmp) throws IOException {
		if (tmp) {
			if (writer==null) {
				throw new FileNotFoundException();
			} else {
				return new CharArrayReader(writer.toCharArray());
			}
		} else {
			if (charArrays==null) {
				throw new FileNotFoundException();
			} else {
				return new CharArrayReader(charArrays);
			}
		}
	}

	@Override
	public void commit() {
		// Force re-creation of the charArrays field
		this.charArrays = writer.toCharArray();
	}

	@Override
	public boolean isEmpty() {
		return (charArrays == null) && (writer==null);
	}
}

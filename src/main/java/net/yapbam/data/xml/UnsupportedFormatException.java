package net.yapbam.data.xml;

import java.io.IOException;

import org.xml.sax.SAXException;

/** Signals that we tried to parse a stream that contains unsupported format.
 * <br>This exception is thrown when pointing to an empty file or an xml file that not contains
 * Yapbam data, or when the file not contains valid xml data.
 */
public class UnsupportedFormatException extends IOException {
	private static final long serialVersionUID = 1L;
	
	UnsupportedFormatException(String message) {
	}
	
	UnsupportedFormatException(SAXException e) {
		super(e);
	}

	public UnsupportedFormatException(Throwable e) {
		super(e);
	}
}

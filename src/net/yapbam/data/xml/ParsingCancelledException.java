package net.yapbam.data.xml;

import org.xml.sax.SAXParseException;

/** Indicates that the parsing was cancelled.
 */
class ParsingCancelledException extends SAXParseException {
	private static final long serialVersionUID = 1L;

	ParsingCancelledException() {
		super("Parsing was cancelled", null, null, -1, -1); //$NON-NLS-1$
	}
}

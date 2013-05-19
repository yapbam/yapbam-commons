package net.yapbam.data.xml;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

class SaxUnsupportedFileVersion extends SAXParseException {
	private static final long serialVersionUID = 1L;
	private int version;

	SaxUnsupportedFileVersion(Locator locator, int version) {
		super ("File is of version "+version+". Expected "+Serializer.CURRENT_VERSION+" or less", locator);
		this.version = version;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}
}

package net.yapbam.data.xml;

import org.xml.sax.helpers.DefaultHandler;

public abstract class DelegateHandler extends DefaultHandler {
	protected abstract String getRootTag();
	protected boolean isEndTag(String tag) {
		return getRootTag().equals(tag);
	}
}

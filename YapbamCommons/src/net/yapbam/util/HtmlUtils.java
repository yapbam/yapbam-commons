package net.yapbam.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

/** Some utility methods on html Strings.
 * 
 * @author Jean-Marc Astesana
 * License GPL v3
 */
public abstract class HtmlUtils {
	private static final Pattern p = Pattern.compile("\\[([^\\[]*)\\[([^\\]]+)\\]\\]");
	public static final String START_TAG = "<HTML>";
	public static final String END_TAG = "</HTML>";
	public static final String NEW_LINE_TAG = "<BR>";

	private HtmlUtils() {
		// To prevent instantiation
		super();
	}
	
	/** Removes the &lt;html&gt; and &lt;/html&gt; tag respectively at the beginning and the end of a string.
	 * @param text The string to process
	 * @return the string without the html tags, or the trimmed string if it doesn't not contains the tags.
	 */
	public static String removeHtmlTags (String text) {
		text = text.trim();
		String upper = text.toUpperCase();
		if (upper.startsWith(START_TAG) && upper.endsWith(END_TAG)) {
			text = text.substring(START_TAG.length());
			text = text.substring(0, text.length()-END_TAG.length());
		}
		return text;
	}
	
	/** Converts a simple link encoded string to html.
	 * @param content The encoded content.<br>Links are encoded with the following syntax [<i>text</i>[<i>url</i>]].
	 * <br>If <i>text</i> is omitted, <i>url</i> is used as text.
	 * <br>Examples:<ul>
	 * <li>This is a [link to Google[http://www.google.com]] -> This is a &lt;a href="http://www.google.com"&gt;link to Google&lt;/a&gt;</li>
	 * <li>Try [[http://www.google.com]] -> Try &lt;a href="http://www.google.com"&gt;http://www.google.com&lt;/a&gt;</li></li>
	 * </ul>
	 * @return the html text corresponding to the encoded content. The returned string does not contains "&lt;html&gt;&lt;/html&gt;" tags around the html generated content. 
	 */
	public static String toHtml(String content) {
		Matcher m = p.matcher(content);
		StringBuilder sb = new StringBuilder();
		int previous = 0;
		while (m.find()) {
			if (previous!=m.start()) {
				sb.append(StringEscapeUtils.escapeHtml3(content.substring(previous, m.start())));
			}
			sb.append(getHTMLLink(m.group(1), m.group(2)));
			previous = m.end()+1;
		}
		if (previous<content.length()) {
			sb.append(StringEscapeUtils.escapeHtml3(content.substring(previous)));
		}
		return sb.toString();
	}

	private static String getHTMLLink(String name, String url) {
		return "<a href=\"" + url + "\">" + StringEscapeUtils.escapeHtml3(((name.length() == 0) ? url : name)) + "</a>";
	}

}

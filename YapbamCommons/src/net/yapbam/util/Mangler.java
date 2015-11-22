package net.yapbam.util;

/**
 * This class is used to encode a string so as to escape a number of predefined characters.
 * <br>
 * Encoding is performed according to a simple principle that does not allows the escape of totally arbitrary characters.
 * However, it has hitherto proved sufficient.<BR><br>
 * The principle is as follows:
 * <BR>
 * Forbidden characters are coded on two characters, the first character (called escape) indicates that the next character was escaped.
 * The following code is specific to each escaped character; '0' for the encoding of character, '1' for the first escaped character ... and so on.
 * <BR><br>
 * It follows from this:<ul>
 * <li>escaped string never contains any of the escaped char</li>
 * <li>the following limitations:<UL>
 * <LI>The escape character must not be in escaped characters </LI>
 * <LI> Prohibited characters must be different from '0' to 'n' (n is the number of characters allowed.</LI>
 * </UL></li>
 * </ul>
 */
public class Mangler {
	/** Escape character */
	private char[] escapeChars;
	/** The first encoding character */
	private static final int BASE_CODE = '0';

	/**
	 * Constructor
	 * @param reservedChars the characters to escape.
	 * @param mangleChar the escape character.
	 * @exception java.lang.IllegalArgumentException if <UL>
	 * 	<LI>the escape character is included in reservedChars</LI>
	 * 	<LI>A char between '0' and 'n' (n is the length of reservedChars - 1) is included in reservedChars</LI>
	 * </UL>
	 */
	public Mangler(String reservedChars, char mangleChar) {
		// reservedChars ne contient pas mangleChars ?
		if (reservedChars.indexOf(mangleChar) >= 0) {
			throw new IllegalArgumentException();
		}
		int nb = reservedChars.length();
		// Le code qu'on va ecrire derriere mangleChar ne fait pas partie
		// des caracteres interdits ?
		char code = BASE_CODE;
		for (int i = 0; i <= nb; i++) {
			if (reservedChars.indexOf(code) >= 0) {
				throw new IllegalArgumentException();
			}
			code++;
		}
		escapeChars = new char[nb + 1];
		escapeChars[0] = mangleChar;
		for (int i = 0; i < nb; i++) {
			escapeChars[i + 1] = reservedChars.charAt(i);
		}
	}

	/**
	 * Escapes a string.
	 * @param s the string to be escaped.
	 * @return a new string if argument contains some escaped characters of the argument if it does not. 
	 */
	public String mangle(String s) {
		StringBuilder buf = null;
		int len = s.length();
		for (int i = 0; i < len; i++) {
			int found = -1;
			char car = s.charAt(i);
			for (int j = 0; j < escapeChars.length; j++) {
				if (escapeChars[j] == car) {
					found = j;
				}
			}
			if (found < 0) {
				if (buf != null) {
					buf.append(car);
				}
			} else {
				if (buf == null) {
					buf = new StringBuilder(s.substring(0, i));
				}
				buf.append(escapeChars[0]);
				buf.append((char) (BASE_CODE + found));
			}
		}
		if (buf != null) {
			// If we escaped some characters, add the escape char at the end of the returned string in order to
			// quickly test if it was escaped
			buf.append(escapeChars[0]);
			return buf.toString();
		} else {
			return s;
		}
	}

	/** Inverse operation of mangle.
	 * @param s a string previously escaped by {@link #mangle(String)}.
	 * @return the string originally passed to {@link #mangle(String)}.
	 */
	public String unmangle(String s) {
		int len = s.length();
		if (len==0 || s.charAt(len - 1) != escapeChars[0]) {
			// La variable n'est pas codee.
			return s;
		} else {
			StringBuilder buf = new StringBuilder();
			boolean mangleMode = false;
			for (int i = 0; i < len; i++) {
				char car = s.charAt(i);
				if (mangleMode) {
					mangleMode = false;
					buf.append(escapeChars[car - BASE_CODE]);
				} else {
					if (car == escapeChars[0]) {
						mangleMode = true;
					} else {
						buf.append(car);
					}
				}
			}
			return buf.toString();
		}
	}
}
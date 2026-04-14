package net.yapbam.util;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Utility class for text manipulation operations.
 */
public final class TextUtils {
    private TextUtils() {
    }

    /**
     * Capitalizes the first character (or grapheme cluster) of a string according to the specified locale.
     * 
     * <p>This method uses {@link BreakIterator} to properly identify the first grapheme cluster,
     * which ensures correct handling of Unicode characters that may be composed of multiple
     * code points (such as accented characters, combining marks, or complex scripts).</p>
     * 
     * <p>Only the first character/cluster is capitalized; the rest of the string remains unchanged.
     * This behavior is different from {@link String#toUpperCase()} which would convert the entire string.</p>
     * 
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>{@code capitalizeFirst("hello", Locale.US)} returns {@code "Hello"}</li>
     *   <li>{@code capitalizeFirst("istanbul", new Locale("tr", "TR"))} returns {@code "İstanbul"} (Turkish dotted I)</li>
     *   <li>{@code capitalizeFirst("été", Locale.FRENCH)} returns {@code "Été"}</li>
     *   <li>{@code capitalizeFirst("ßeta", Locale.GERMAN)} returns {@code "SSeta"}</li>
     * </ul>
     * 
     * @param text the string to capitalize, may be {@code null}
     * @param locale the locale to use for capitalization rules, must not be {@code null}
     * @return the string with the first character capitalized, or {@code null} if the input was {@code null}
     * @throws NullPointerException if {@code locale} is {@code null}
     * @see BreakIterator#getCharacterInstance(Locale)
     * @see String#toUpperCase(Locale)
     * @since 1.10.0
     */
    public static String capitalizeFirst(String text, Locale locale) {
        if (text == null || text.isEmpty()) return text;

        BreakIterator boundary = BreakIterator.getCharacterInstance(locale);
        boundary.setText(text);

        int start = boundary.first();
        int end   = boundary.next();

        // Extrait le premier "graphème" (peut être plusieurs chars en Unicode)
        String firstChar = text.substring(start, end);
        String rest      = text.substring(end);

        return firstChar.toUpperCase(locale) + rest;
    }
}

package net.yapbam.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import net.yapbam.util.TextUtils;

import org.junit.jupiter.api.Test;

class TextUtilsTest {

    @Test
    void testCapitalizeForLocale_NullInput() {
        assertNull(TextUtils.capitalizeFirst(null, Locale.US));
    }

    @Test
    void testCapitalizeForLocale_EmptyString() {
        assertEquals("", TextUtils.capitalizeFirst("", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_SingleCharacter() {
        assertEquals("A", TextUtils.capitalizeFirst("a", Locale.US));
        assertEquals("Z", TextUtils.capitalizeFirst("z", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_LowercaseWord() {
        assertEquals("Hello", TextUtils.capitalizeFirst("hello", Locale.US));
        assertEquals("World", TextUtils.capitalizeFirst("world", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_AlreadyCapitalized() {
        assertEquals("Hello", TextUtils.capitalizeFirst("Hello", Locale.US));
        assertEquals("World", TextUtils.capitalizeFirst("World", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_AllUppercase() {
        assertEquals("HELLO", TextUtils.capitalizeFirst("HELLO", Locale.US));
        assertEquals("WORLD", TextUtils.capitalizeFirst("WORLD", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_MixedCase() {
        assertEquals("HELLO", TextUtils.capitalizeFirst("hELLO", Locale.US));
        assertEquals("WORLD", TextUtils.capitalizeFirst("wORLD", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_WithSpaces() {
        assertEquals("H ello world", TextUtils.capitalizeFirst("h ello world", Locale.US));
        assertEquals("Hello world", TextUtils.capitalizeFirst("Hello world", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_WithNumbers() {
        assertEquals("123abc", TextUtils.capitalizeFirst("123abc", Locale.US));
        assertEquals("A123", TextUtils.capitalizeFirst("a123", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_WithSpecialCharacters() {
        assertEquals("!hello", TextUtils.capitalizeFirst("!hello", Locale.US));
        assertEquals("@world", TextUtils.capitalizeFirst("@world", Locale.US));
    }

    @Test
    void testCapitalizeForLocale_FrenchLocale() {
        assertEquals("Été", TextUtils.capitalizeFirst("été", Locale.FRENCH));
        assertEquals("Àbientôt", TextUtils.capitalizeFirst("àbientôt", Locale.FRENCH));
    }

    @Test
    void testCapitalizeForLocale_TurkishLocale() {
        // Turkish has special casing rules for 'i' and 'I'
        Locale turkish = Locale.forLanguageTag("tr-TR");
        assertEquals("İstanbul", TextUtils.capitalizeFirst("istanbul", turkish));
        assertEquals("İstanbul", TextUtils.capitalizeFirst("İstanbul", turkish));
    }

    @Test
    void testCapitalizeForLocale_GermanLocale() {
        // Test German umlauts
        assertEquals("Äpfel", TextUtils.capitalizeFirst("äpfel", Locale.GERMAN));
        assertEquals("Österreich", TextUtils.capitalizeFirst("österreich", Locale.GERMAN));
        assertEquals("Übung", TextUtils.capitalizeFirst("übung", Locale.GERMAN));
        assertEquals("SSeta", TextUtils.capitalizeFirst("ßeta", Locale.GERMAN));
    }

    @Test
    void testCapitalizeForLocale_UnicodeCharacters() {
        // Test with Unicode characters that may be composed of multiple code points
        Locale spanish = Locale.forLanguageTag("es-ES");
        assertEquals("Ñandú", TextUtils.capitalizeFirst("ñandú", spanish));
        assertEquals("Ça", TextUtils.capitalizeFirst("ça", Locale.FRENCH));
    }

    @Test
    void testCapitalizeForLocale_WithAccents() {
        assertEquals("Café", TextUtils.capitalizeFirst("café", Locale.FRENCH));
        assertEquals("Naïve", TextUtils.capitalizeFirst("naïve", Locale.FRENCH));
        assertEquals("Résumé", TextUtils.capitalizeFirst("résumé", Locale.FRENCH));
    }

    @Test
    void testCapitalizeForLocale_LongString() {
        String longString = "this is a very long string to test that the method works correctly with longer inputs";
        String expected = "This is a very long string to test that the method works correctly with longer inputs";
        assertEquals(expected, TextUtils.capitalizeFirst(longString, Locale.US));
    }

    @Test
    void testCapitalizeForLocale_OnlyFirstCharChanges() {
        // Verify that only the first character is changed
        String input = "hello WORLD";
        String result = TextUtils.capitalizeFirst(input, Locale.US);
        assertEquals("Hello WORLD", result);
        // The rest of the string should remain unchanged
        assertEquals(" WORLD", result.substring(5));
    }

    @Test
    void testCapitalizeForLocale_WithEmoji() {
        // Test with emoji (should not affect the capitalization of following text)
        assertEquals("😀smile", TextUtils.capitalizeFirst("😀smile", Locale.US));
    }
}

package tools.cevi.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits text into words and folds them, the same way for a Search Term and for the text of an
 * entry, so that both sides of a comparison are always in the same form (UC-010 BR-003, OQ-004).
 */
final class Words {

    // UC-010 BR-003 (OQ-004): any character that is neither a letter nor a digit ends a word.
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private Words() {
    }

    /** The folded words of {@code text}, in order. */
    static List<String> of(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text == null ? "" : text);
        while (matcher.find()) {
            words.add(fold(matcher.group()));
        }
        return words;
    }

    /** The position in {@code text} of the first word that begins with one of {@code prefixes}. */
    static int firstOccurrence(String text, List<String> prefixes) {
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            if (startsWithAny(fold(matcher.group()), prefixes)) {
                return matcher.start();
            }
        }
        return -1;
    }

    static boolean startsWithAny(String word, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (word.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // UC-010 BR-003: case and accents are ignored — Poděbrady matches podebrady, Zürich zurich.
    static String fold(String word) {
        String decomposed = Normalizer.normalize(word, Normalizer.Form.NFD);
        String bare = MARKS.matcher(decomposed).replaceAll("");
        // Letters whose stroke is part of the letter, not a separable mark (OQ-004).
        return bare.toLowerCase(Locale.ROOT)
                .replace('ł', 'l')
                .replace('ø', 'o')
                .replace('đ', 'd');
    }
}

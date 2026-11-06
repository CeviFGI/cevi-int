package tools.cevi.search;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A Search Term as the visitor entered it, reduced to what the search may use (UC-010).
 *
 * @param text  the term as it is shown again in the search field, at most {@link #MAX_LENGTH}
 *              characters
 * @param words the folded words every match must contain, in the order typed
 */
public record SearchTerm(String text, List<String> words) {

    // UC-010 A4: a longer Search Term is cut to its first 100 characters (NFR-047).
    public static final int MAX_LENGTH = 100;

    // UC-010 A3: fewer than two letters or digits start no search; spaces and punctuation do not count.
    public static final int MIN_LENGTH = 2;

    public static SearchTerm of(String raw) {
        String text = raw == null ? "" : cut(raw.strip());
        return new SearchTerm(text, Words.of(text));
    }

    /** Whether the term is long enough to search for (A3). */
    public boolean searchable() {
        return Words.letterCount(text) >= MIN_LENGTH;
    }

    /** UC-010 BR-005: the address of the Search Results Page for this term. */
    public String address() {
        return "/suche?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static String cut(String text) {
        if (text.length() <= MAX_LENGTH) {
            return text;
        }
        // Never split a surrogate pair, or the shortened term shown in the field ends in garbage.
        int end = Character.isHighSurrogate(text.charAt(MAX_LENGTH - 1)) ? MAX_LENGTH - 1 : MAX_LENGTH;
        return text.substring(0, end).strip();
    }
}

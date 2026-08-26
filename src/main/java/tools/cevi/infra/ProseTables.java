package tools.cevi.infra;

import java.util.regex.Pattern;

/**
 * Wraps every table in a stored description in a scroll container (NFR-013).
 *
 * <p>An administrator writing about costs reaches for a table, and a table with five columns is
 * wider than a phone. Without a box of its own it takes the whole page sideways — the one layout
 * defect NFR-013 names explicitly, and the one a desktop review never sees. {@code prose.css} has
 * carried the {@code .prose-table} rule since phase 2 on the assumption that the template applied
 * the wrapper; nothing did.
 *
 * <p>It is done here rather than in {@link HtmlSanitizer} because it is presentation, and a
 * presentation concern in the security boundary is how a security boundary starts accumulating
 * reasons to change. Doing it on the way out also means it applies to descriptions that were
 * already stored, which a change at storage time would not.
 *
 * <p><strong>This is not a security control.</strong> It reads a value that was reduced to the
 * allow-list when it was stored (NFR-010) and hands it on unchanged apart from the wrapper.
 */
public final class ProseTables {

    private static final Pattern OPENING = Pattern.compile("(?i)<table\\b");
    private static final Pattern CLOSING = Pattern.compile("(?i)</table\\s*>");

    private ProseTables() {
    }

    /**
     * @param html a stored, already sanitised description; may be {@code null}
     * @return the same markup with each {@code <table>} inside a {@code <div class="prose-table">}
     */
    public static String wrapped(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        // One wrapper opens per <table and one closes per </table>, in the order they appear, so
        // the result stays balanced even for the nested table the allow-list permits. The tag
        // itself is carried over as $0 rather than rewritten, so nothing but the wrapper changes.
        String opened = OPENING.matcher(html).replaceAll("<div class=\"prose-table\">$0");
        return CLOSING.matcher(opened).replaceAll("$0</div>");
    }
}

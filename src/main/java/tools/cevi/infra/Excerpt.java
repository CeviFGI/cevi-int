package tools.cevi.infra;

import java.util.regex.Pattern;

/**
 * Shortens a stored description to a plain-text excerpt for a list (BR-040, BR-044).
 *
 * <p><strong>This is a presentation helper, never a security control.</strong> It reads a value
 * that was already reduced to the allow-list when it was stored (NFR-010), and its result is
 * rendered escaped — the lists interpolate it with {@code {...}}, not with {@code .raw}. Nothing
 * here may become a reason to relax the sanitiser: if this class ever saw unsanitised input, the
 * defect would be that the input reached storage, not that this class let it through.
 */
public final class Excerpt {

    /** Roughly three lines in a card at the body size. */
    public static final int DEFAULT_LENGTH = 160;

    /**
     * Tags that separate words. Removing {@code <p>a</p><p>b</p>} without them would read "ab",
     * while removing inline tags with a space would break "wor<b>d</b>" into "wor d".
     */
    private static final Pattern BLOCK_TAG = Pattern.compile(
            "(?i)</?(p|div|br|hr|li|ul|ol|tr|td|th|table|thead|tbody|tfoot|caption|h[1-6]|blockquote|pre)\\b[^>]*>");

    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private Excerpt() {
    }

    public static String from(String html) {
        return from(html, DEFAULT_LENGTH);
    }

    /**
     * @param html      stored description, already sanitised; may be {@code null}
     * @param maxLength the length above which the text is cut
     * @return plain text of at most {@code maxLength} characters plus an ellipsis, cut on a word
     *         boundary; an empty string when there is nothing to show
     */
    public static String from(String html, int maxLength) {
        if (html == null || html.isBlank()) {
            return "";
        }

        String text = BLOCK_TAG.matcher(html).replaceAll(" ");
        text = ANY_TAG.matcher(text).replaceAll("");
        text = decodeEntities(text);
        text = WHITESPACE.matcher(text).replaceAll(" ").trim();

        if (text.length() <= maxLength) {
            return text;
        }

        String cut = text.substring(0, maxLength);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > maxLength / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut.stripTrailing() + "…";
    }

    /**
     * The handful of references the sanitiser's output actually contains. {@code &amp;} is decoded
     * last, so that a stored "&amp;lt;" turns into the text "&lt;" rather than into a tag.
     */
    private static String decodeEntities(String text) {
        return text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&amp;", "&");
    }
}

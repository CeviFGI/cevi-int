package tools.cevi.infra;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The visible text of a stored description, without its formatting, for a place that cannot show
 * markup — the notes of a Calendar Entry (UC-012 BR-002).
 *
 * <p><strong>Not a security control.</strong> It reads a value already reduced to the allow-list
 * when it was stored (NFR-010) and produces text for a consumer that never interprets HTML. Its
 * result must not be rendered unescaped in a page.
 */
public final class PlainText {

    /** Elements that end a line of text; removing them without a break would run two paragraphs together. */
    private static final Pattern BLOCK_TAG = Pattern.compile(
            "(?i)</?(p|div|br|hr|li|ul|ol|tr|table|thead|tbody|tfoot|caption|h[1-6]|blockquote|pre)\\b[^>]*>");

    /** Cells of one row stay on one line, separated like words. */
    private static final Pattern CELL_TAG = Pattern.compile("(?i)</?(td|th)\\b[^>]*>");

    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern HORIZONTAL_SPACE = Pattern.compile("[\\h\\x0B\\f\\r]+");

    /**
     * Every character reference the sanitiser writes: it encodes not only {@code <} and {@code &}
     * but also characters such as {@code @}, {@code +} or {@code =} as numeric references.
     */
    private static final Pattern REFERENCE = Pattern.compile("&(#\\d{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z]{2,6});");

    private PlainText() {
    }

    /**
     * @param html stored description, already sanitised; may be {@code null}
     * @return its text, one line per paragraph, list item or table row, without empty lines and
     *         with whitespace inside a line collapsed; an empty string when there is no text
     */
    // UC-012 BR-002: the visible text of the Description, without formatting
    public static String of(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = BLOCK_TAG.matcher(html).replaceAll("\n");
        text = CELL_TAG.matcher(text).replaceAll(" ");
        text = ANY_TAG.matcher(text).replaceAll("");
        text = decode(text);
        return Arrays.stream(text.split("\n"))
                .map(line -> HORIZONTAL_SPACE.matcher(line).replaceAll(" ").strip())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Decodes character references in one pass, so that a stored {@code &amp;lt;} reads as the text
     * {@code &lt;} and never turns into a tag. An unknown or invalid reference is kept as written.
     */
    public static String decode(String text) {
        Matcher matcher = REFERENCE.matcher(text);
        StringBuilder decoded = new StringBuilder(text.length());
        while (matcher.find()) {
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(resolve(matcher.group(1), matcher.group())));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    private static String resolve(String reference, String original) {
        if (reference.charAt(0) == '#') {
            boolean hex = reference.length() > 1 && (reference.charAt(1) == 'x' || reference.charAt(1) == 'X');
            int codePoint = Integer.parseInt(reference.substring(hex ? 2 : 1), hex ? 16 : 10);
            return Character.isValidCodePoint(codePoint) && codePoint != 0 ? Character.toString(codePoint) : original;
        }
        return switch (reference.toLowerCase(Locale.ROOT)) {
            case "amp" -> "&";
            case "lt" -> "<";
            case "gt" -> ">";
            case "quot" -> "\"";
            case "apos" -> "'";
            case "nbsp" -> " ";
            default -> original;
        };
    }
}

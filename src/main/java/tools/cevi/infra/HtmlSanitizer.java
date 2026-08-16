package tools.cevi.infra;

import java.util.Locale;

import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Reduces the HTML coming from the rich-text editor to the allow-list documented in
 * {@code docs/entity_model.md}.
 * <p>
 * Descriptions are rendered unescaped on the public pages, so this reduction — not the editor in
 * the browser — is the boundary. It runs on the value that is about to be stored, which means the
 * stored value is already safe no matter how the request reached the endpoint.
 */
public final class HtmlSanitizer {

    /**
     * Rejects {@code data:} URLs. Applied to {@code a href} because the policy has to allow the
     * {@code data} scheme for inline pictures, and a {@code data:} link is a navigation target
     * rather than an embedded resource.
     */
    private static final AttributePolicy NO_DATA_URL = (element, attribute, value) ->
            value.trim().toLowerCase(Locale.ROOT).startsWith("data:") ? null : value;

    /** Restricts inline images to the {@code data:image/…} form the editor produces when pasting. */
    private static final AttributePolicy IMAGE_SOURCE = (element, attribute, value) -> {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("data:")) {
            return normalized.startsWith("data:image/") ? value : null;
        }
        return value;
    };

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            // "data" is needed for pictures the editor embeds inline; NO_DATA_URL keeps it out of links.
            .allowUrlProtocols("http", "https", "mailto", "data")
            .allowElements("p", "br", "hr", "div", "span", "blockquote", "pre", "code",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "b", "strong", "i", "em", "u", "s", "strike", "sub", "sup", "font",
                    "ul", "ol", "li",
                    "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption",
                    "a", "img")
            .allowAttributes("href").matching(NO_DATA_URL).onElements("a")
            .allowAttributes("title").onElements("a")
            .allowAttributes("target").matching(true, "_blank").onElements("a")
            .allowAttributes("src").matching(IMAGE_SOURCE).onElements("img")
            .allowAttributes("alt", "title", "width", "height").onElements("img")
            .allowAttributes("color", "face", "size").onElements("font")
            .allowAttributes("colspan", "rowspan").onElements("td", "th")
            .allowAttributes("align").onElements("p", "div", "td", "th", "table")
            // Keeps the colours and font sizes the editor writes as inline styles, reduced to a
            // safe set of CSS properties by the sanitiser's own CSS schema.
            .allowStyling()
            .requireRelNofollowOnLinks()
            .toFactory();

    private HtmlSanitizer() {
    }

    /**
     * @param html the submitted markup, may be {@code null}
     * @return the markup reduced to the allow-list, or {@code null} if {@code html} was {@code null}
     */
    public static String sanitize(String html) {
        return html == null ? null : POLICY.sanitize(html);
    }
}

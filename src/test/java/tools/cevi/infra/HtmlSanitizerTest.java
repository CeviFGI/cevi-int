package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class HtmlSanitizerTest {

    @Test
    void null_stays_null() {
        assertThat(HtmlSanitizer.sanitize(null), is(nullValue()));
    }

    @Test
    void keeps_the_formatting_the_editor_produces() {
        String sanitized = HtmlSanitizer.sanitize(
                "<p><strong>Kosten:</strong> 900 USD<br><em>inkl.</em> Reise</p>");

        assertThat(sanitized, containsString("<strong>Kosten:</strong>"));
        assertThat(sanitized, containsString("<em>inkl.</em>"));
        assertThat(sanitized, containsString("<br"));
    }

    @Test
    void keeps_lists_tables_and_colours() {
        String sanitized = HtmlSanitizer.sanitize(
                "<ul><li>eins</li></ul><table><tr><td colspan=\"2\">x</td></tr></table>"
                        + "<span style=\"color: rgb(255, 0, 0);\">rot</span>");

        assertThat(sanitized, containsString("<li>eins</li>"));
        assertThat(sanitized, containsString("colspan=\"2\""));
        assertThat(sanitized, containsString("color"));
    }

    @Test
    void removes_script_elements() {
        String sanitized = HtmlSanitizer.sanitize("<p>hallo</p><script>alert(1)</script>");

        assertThat(sanitized, containsString("hallo"));
        assertThat(sanitized, not(containsString("<script")));
        assertThat(sanitized, not(containsString("alert(1)")));
    }

    @Test
    void removes_event_handler_attributes() {
        String sanitized = HtmlSanitizer.sanitize("<img src=\"https://example.org/x.png\" onerror=\"alert(1)\">");

        assertThat(sanitized, not(containsString("onerror")));
        assertThat(sanitized, containsString("https://example.org/x.png"));
    }

    @Test
    void removes_iframes_and_objects() {
        String sanitized = HtmlSanitizer.sanitize(
                "<iframe src=\"https://example.org\"></iframe><object data=\"x\"></object>");

        assertThat(sanitized, not(containsString("<iframe")));
        assertThat(sanitized, not(containsString("<object")));
    }

    @Test
    void keeps_web_links_but_drops_script_links() {
        assertThat(HtmlSanitizer.sanitize("<a href=\"https://cevi.ch\">Cevi</a>"),
                containsString("https://cevi.ch"));
        assertThat(HtmlSanitizer.sanitize("<a href=\"javascript:alert(1)\">x</a>"),
                not(containsString("javascript")));
        // A data: link is a navigation target, unlike an embedded picture, so it stays out.
        assertThat(HtmlSanitizer.sanitize("<a href=\"data:text/html,<script>alert(1)</script>\">x</a>"),
                not(containsString("data:text/html")));
    }

    @Test
    void keeps_pictures_the_editor_embeds_inline() {
        String pixel = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

        assertThat(HtmlSanitizer.sanitize("<img src=\"" + pixel + "\">"), containsString("data:image/gif"));
        assertThat(HtmlSanitizer.sanitize("<img src=\"data:text/html,<script>alert(1)</script>\">"),
                not(containsString("data:text/html")));
    }

    @Test
    void plain_text_survives_unchanged() {
        assertThat(HtmlSanitizer.sanitize("Generalversammlung des YMCA"),
                equalTo("Generalversammlung des YMCA"));
    }
}

package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** UC-012 BR-002: the visible text of a Description, without formatting, for the Calendar Entry. */
class PlainTextTest {

    /** BR-002: a missing or empty Description, or one without visible text, yields no text. */
    @Test
    void nothing_yields_nothing() {
        assertEquals("", PlainText.of(null));
        assertEquals("", PlainText.of(""));
        assertEquals("", PlainText.of("<p> </p><br>"));
    }

    /** BR-002: formatting and links are removed, each paragraph stays a line, white space collapses. */
    @Test
    void formatting_is_removed_but_paragraphs_stay_lines() {
        assertEquals("Zwei Wochen in Prag.\nKosten: 300 CHF",
                PlainText.of("<p>Zwei <strong>Wochen</strong> in <a href=\"https://x.test\">Prag</a>.</p>"
                        + "<p>Kosten:   300&nbsp;CHF</p>"));
    }

    /** BR-002: headings, list items and table rows become lines; the cells of a row stay on it. */
    @Test
    void list_items_and_table_rows_become_lines_and_cells_stay_on_their_row() {
        assertEquals("Programm\nAnreise\nLager\nTag Ort",
                PlainText.of("<h2>Programm</h2><ul><li>Anreise</li><li>Lager</li></ul>"
                        + "<table><tr><td>Tag</td><td>Ort</td></tr></table>"));
    }

    /** What the sanitiser writes as a numeric reference reads as the character again. */
    @Test
    void every_character_reference_the_sanitiser_writes_is_decoded() {
        assertEquals("info@cevi.ch 1+1=2 \"zitiert\" 'so' <b> & ä",
                PlainText.of("info&#64;cevi.ch 1&#43;1&#61;2 &#34;zitiert&#34; &#39;so&#39; &lt;b&gt; &amp; &#xe4;"));
    }

    /** BR-002: a reference the visitor typed as text is decoded once only and stays text. */
    @Test
    void a_double_encoded_reference_stays_text() {
        assertEquals("&lt;script&gt;", PlainText.of("&amp;lt;script&amp;gt;"));
    }

    /** BR-002: an unknown name or a code point outside Unicode is kept as written. */
    @Test
    void an_unknown_or_invalid_reference_is_kept_as_written() {
        assertEquals("&bogus; &#0; &#1114112;", PlainText.decode("&bogus; &#0; &#1114112;"));
    }

    /** The sanitiser's actual output, not a guess about it. */
    @Test
    void reads_a_sanitised_description() {
        String stored = HtmlSanitizer.sanitize("<p>Mail an info@cevi.ch, Kosten 100+50 CHF</p><script>x()</script>");

        assertEquals("Mail an info@cevi.ch, Kosten 100+50 CHF", PlainText.of(stored));
    }
}

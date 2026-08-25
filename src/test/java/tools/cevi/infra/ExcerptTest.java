package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcerptTest {

    @Test
    void nothing_to_shorten_yields_nothing() {
        assertEquals("", Excerpt.from(null));
        assertEquals("", Excerpt.from(""));
        assertEquals("", Excerpt.from("   "));
    }

    @Test
    void plain_text_shorter_than_the_limit_is_returned_unchanged() {
        assertEquals("Zwei Wochen in Prag.", Excerpt.from("Zwei Wochen in Prag."));
    }

    @Test
    void markup_is_removed() {
        assertEquals("Zwei Wochen in Prag.",
                Excerpt.from("<p>Zwei <strong>Wochen</strong> in <a href=\"https://x.test\">Prag</a>.</p>"));
    }

    /** Without this, two paragraphs would run into each other as one word. */
    @Test
    void block_elements_separate_words() {
        assertEquals("Erster Absatz Zweiter Absatz",
                Excerpt.from("<p>Erster Absatz</p><p>Zweiter Absatz</p>"));
        assertEquals("Eins Zwei", Excerpt.from("Eins<br>Zwei"));
    }

    /** ...and the opposite mistake: an inline element must not split a word. */
    @Test
    void inline_elements_do_not_split_a_word() {
        assertEquals("Wort", Excerpt.from("Wor<b>t</b>"));
    }

    @Test
    void character_references_are_decoded() {
        assertEquals("Kosten & Reise", Excerpt.from("<p>Kosten &amp; Reise</p>"));
        assertEquals("\"Euro Camp\"", Excerpt.from("&quot;Euro Camp&quot;"));
        assertEquals("a b", Excerpt.from("a&nbsp;b"));
    }

    /**
     * A stored "&amp;lt;" is the text "&lt;", not the start of a tag — decoding the ampersand last
     * is what keeps the two apart.
     */
    @Test
    void a_double_encoded_reference_does_not_become_markup() {
        assertEquals("&lt;script&gt;", Excerpt.from("&amp;lt;script&amp;gt;"));
    }

    @Test
    void whitespace_is_collapsed() {
        assertEquals("Eins Zwei", Excerpt.from("Eins   \n\t Zwei"));
    }

    @Test
    void a_long_text_is_cut_on_a_word_boundary() {
        String text = "Zwei Wochen mit rund vierhundert jungen Menschen aus ganz Europa, mit "
                + "Workshops zu Nachhaltigkeit, internationalen Abenden und Ausfluegen in die "
                + "Umgebung der Stadt.";

        String excerpt = Excerpt.from(text, 60);

        assertTrue(excerpt.endsWith("…"), excerpt);
        assertTrue(excerpt.length() <= 61, "cut plus ellipsis, was " + excerpt.length());
        assertTrue(text.startsWith(excerpt.substring(0, excerpt.length() - 1)),
                "the excerpt should be a prefix of the text");
        assertTrue(excerpt.charAt(excerpt.length() - 2) != ' ', "no space before the ellipsis");
    }

    @Test
    void a_text_exactly_at_the_limit_is_not_cut() {
        String text = "a".repeat(40);

        assertEquals(text, Excerpt.from(text, 40));
    }

    /** A single word longer than the limit has no boundary to cut on, so it is cut hard. */
    @Test
    void a_word_longer_than_the_limit_is_cut_anyway() {
        String excerpt = Excerpt.from("a".repeat(100), 20);

        assertEquals("a".repeat(20) + "…", excerpt);
    }
}

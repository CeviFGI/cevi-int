package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-001 BR-004 and A3 (UC-003 BR-004 uses the same helper): the excerpt a list shows instead of
 * the whole description.
 */
class ExcerptTest {

    @Test
    void nothing_to_shorten_yields_nothing() {
        assertEquals("", Excerpt.from(null));
        assertEquals("", Excerpt.from(""));
        assertEquals("", Excerpt.from("   "));
    }

    /** UC-001 A3: a description shorter than the excerpt is kept whole, without an ellipsis. */
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

    /** UC-001 BR-004: the cut falls between two words, never inside one. */
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
        // The 60th character falls inside "Europa"; the excerpt stops after the word before it.
        assertEquals("Zwei Wochen mit rund vierhundert jungen Menschen aus ganz…", excerpt);
    }

    @Test
    void a_text_exactly_at_the_limit_is_not_cut() {
        String text = "a".repeat(40);

        assertEquals(text, Excerpt.from(text, 40));
    }

    /** UC-001 BR-004: without a word boundary to cut on, the excerpt is cut at the limit and still ends with an ellipsis. */
    @Test
    void a_word_longer_than_the_limit_is_cut_anyway() {
        String excerpt = Excerpt.from("a".repeat(100), 20);

        assertEquals("a".repeat(20) + "…", excerpt);
    }

    /**
     * UC-001 BR-004: a word boundary only in the first half of the excerpt length is not usable —
     * cutting there would leave too little text — so the excerpt is cut at the limit.
     */
    @Test
    void a_boundary_only_in_the_first_half_is_not_used() {
        String excerpt = Excerpt.from("Link https://example.org/" + "a".repeat(100), 40);

        assertEquals(("Link https://example.org/" + "a".repeat(100)).substring(0, 40) + "…", excerpt);
    }
}

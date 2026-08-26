package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProseTablesTest {

    @Test
    void a_description_without_a_table_is_handed_on_unchanged() {
        String html = "<p>Kosten: <strong>900 USD</strong></p><ul><li>Reise</li></ul>";

        assertEquals(html, ProseTables.wrapped(html));
    }

    @Test
    void a_table_gets_a_scroll_container_of_its_own() {
        assertEquals("<div class=\"prose-table\"><table><tr><td>1</td></tr></table></div>",
                ProseTables.wrapped("<table><tr><td>1</td></tr></table>"));
    }

    @Test
    void a_table_carrying_attributes_is_wrapped_with_them_intact() {
        assertEquals("<div class=\"prose-table\"><table align=\"left\"><tr><td>1</td></tr></table></div>",
                ProseTables.wrapped("<table align=\"left\"><tr><td>1</td></tr></table>"));
    }

    @Test
    void every_table_in_a_description_gets_one() {
        String wrapped = ProseTables.wrapped("<table><tr><td>a</td></tr></table><p>und</p><table><tr><td>b</td></tr></table>");

        assertEquals(2, wrapped.split("prose-table", -1).length - 1);
        assertEquals(2, wrapped.split("</div>", -1).length - 1);
    }

    /**
     * The allow-list permits a table inside a cell. One wrapper opens per opening tag and one
     * closes per closing tag, in the order they appear, so the nesting stays balanced.
     */
    @Test
    void a_nested_table_leaves_the_markup_balanced() {
        String wrapped = ProseTables.wrapped(
                "<table><tr><td><table><tr><td>x</td></tr></table></td></tr></table>");

        assertEquals("<div class=\"prose-table\"><table><tr><td>"
                        + "<div class=\"prose-table\"><table><tr><td>x</td></tr></table></div>"
                        + "</td></tr></table></div>",
                wrapped);
    }

    @Test
    void the_editor_writes_upper_case_tags_too() {
        String wrapped = ProseTables.wrapped("<TABLE><TR><TD>1</TD></TR></TABLE >");

        assertTrue(wrapped.startsWith("<div class=\"prose-table\"><TABLE>"), wrapped);
        assertTrue(wrapped.endsWith("</TABLE ></div>"), wrapped);
    }

    @Test
    void an_absent_description_stays_absent() {
        assertNull(ProseTables.wrapped(null));
        assertEquals("", ProseTables.wrapped(""));
    }
}

package infra;

import org.junit.jupiter.api.Test;
import java.util.Set;
import tools.cevi.infra.Slug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/** The readable form of a derived Short Name: UC-002 BR-005, BR-006 and UC-004 BR-007. */
public class SlugTest {
    /** UC-002 BR-005, UC-004 BR-007: lowercase Latin letters, digits and single hyphens. */
    @Test
    public void slug_with_umlaute() {
        Slug slug = Slug.of("Jugendreise \"Journey for Justice\" - mit Umlaut ä und Satzzeichen !");
        assertThat(slug.toString(), equalTo("jugendreise-journey-for-justice-mit-umlaut-a-und-satzzeichen"));
    }

    /** UC-002 BR-005, UC-004 BR-007: a free name is taken as derived, a taken one gets a counter until it is free. */
    @Test
    public void unique_appends_a_counter_until_the_name_is_free() {
        Set<String> taken = Set.of("horyzon-genf", "horyzon-genf0");
        assertThat(Slug.unique("Horyzon Genf", name -> false), equalTo("horyzon-genf"));
        assertThat(Slug.unique("Horyzon Genf", taken::contains), equalTo("horyzon-genf1"));
    }

    /** UC-002 BR-006, UC-004 BR-007: cut to 252 characters, so that a counter still fits into 255. */
    @Test
    public void unique_cuts_the_derived_name_to_252_characters() {
        String derived = Slug.unique("a".repeat(300), name -> false);
        assertThat(derived.length(), equalTo(252));
        String base = "a".repeat(252);
        Set<String> taken = new java.util.HashSet<>(Set.of(base));
        for (int i = 0; i < 1000; i++) {
            taken.add(base + i);
        }
        // From four digits on the name gives way to the counter, so the whole stays within 255.
        assertThat(Slug.unique("a".repeat(300), taken::contains), equalTo("a".repeat(251) + "1000"));
    }
}

package infra;

import org.junit.jupiter.api.Test;
import tools.cevi.infra.Slug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SlugTest {
    @Test
    public void slug_with_umlaute() {
        Slug slug = Slug.of("Jugendreise \"Journey for Justice\" - mit Umlaut ä und Satzzeichen !");
        assertThat(slug.toString(), equalTo("jugendreise-journey-for-justice-mit-umlaut-a-und-satzzeichen"));
    }
}

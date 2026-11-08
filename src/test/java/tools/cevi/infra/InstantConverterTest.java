package tools.cevi.infra;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

/** UC-013 BR-002: a moment is stored as text of fixed width, so the text sorts like the moments. */
class InstantConverterTest {

    private final InstantConverter converter = new InstantConverter();

    @Test
    void a_moment_is_written_with_milliseconds_always_present() {
        assertThat(converter.convertToDatabaseColumn(Instant.parse("2026-11-08T09:05:03Z")),
                equalTo("2026-11-08T09:05:03.000Z"));
    }

    @Test
    void the_text_reads_back_as_the_same_moment() {
        Instant moment = Instant.parse("2026-11-08T09:05:03.250Z");

        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(moment)), equalTo(moment));
    }

    @Test
    void the_text_sorts_like_the_moments() {
        String earlier = converter.convertToDatabaseColumn(Instant.parse("2026-11-08T09:05:03Z"));
        String later = converter.convertToDatabaseColumn(Instant.parse("2026-11-08T09:05:03.250Z"));

        assertThat(earlier.compareTo(later), lessThan(0));
    }

    @Test
    void no_moment_is_no_text() {
        assertThat(converter.convertToDatabaseColumn(null), nullValue());
        assertThat(converter.convertToEntityAttribute(null), nullValue());
        assertThat(converter.convertToEntityAttribute(""), nullValue());
    }
}

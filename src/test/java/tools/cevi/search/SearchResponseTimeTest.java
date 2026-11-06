package tools.cevi.search;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.cevi.event.Event;
import tools.cevi.voluntary.VoluntaryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * UC-010, NFR-046: a search answers within 300 ms at the 95th percentile with 500 Upcoming Events
 * and 200 Voluntary Service Offers stored — as Search Results Page and as Live Results.
 *
 * <p>Measured in the test JVM against the SQLite file, not on the production host the requirement
 * names; it holds the implementation to the figure, and a regression that multiplies the work per
 * entry shows up here first.
 *
 * <p>The volume is stored once for the class, not per test: both measurements only read it. The
 * fixtures' per-test clean-up deletes by id, so it never touches these rows; {@link #removeThem()}
 * does.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchResponseTimeTest {

    private static final String PREFIX = "CLEANUP Last ";
    private static final int EVENTS = 500;
    private static final int OFFERS = 200;
    private static final int RUNS = 40;
    private static final long P95_LIMIT_MS = 300;

    /** Roughly the length of a real description, with the markup the editor produces. */
    private static final String DESCRIPTION = "<p>" + "Ein internationales Treffen mit Workshops, Andachten und Ausflügen "
            .repeat(12) + "<a href=\"https://example.org/info\">Weitere Informationen</a></p>"
            + "<ul><li>Kosten: 300 Euro</li><li>Alter: 18 bis 30</li></ul>";

    /**
     * {@code @BeforeAll} runs without a request context, so the transaction is the
     * {@code requiringNew()} kind rather than {@code begin()}/{@code commit()}, which is request-scoped.
     */
    @BeforeAll
    void storeTheVolumeNfr046Names() {
        QuarkusTransaction.requiringNew().run(SearchResponseTimeTest::storeTheVolume);
    }

    private static void storeTheVolume() {
        for (int i = 0; i < EVENTS; i++) {
            Event event = new Event();
            event.title = PREFIX + "Anlass " + i;
            event.slug = "cleanup-last-anlass-" + i;
            event.date = i + ". Oktober";
            event.location = "Ort " + i;
            event.displayDate = LocalDate.now().plusDays(1 + i % 300);
            event.description = DESCRIPTION + (i % 50 == 0 ? "<p>Zqlast</p>" : "");
            event.persist();
        }
        for (int i = 0; i < OFFERS; i++) {
            VoluntaryService offer = new VoluntaryService();
            offer.organization = PREFIX + "Verein " + i;
            offer.organizationLink = "https://example.org/" + i;
            offer.location = "Land " + i;
            offer.description = DESCRIPTION + (i % 20 == 0 ? "<p>Zqlast</p>" : "");
            offer.persist();
        }
    }

    @AfterAll
    void removeThem() {
        QuarkusTransaction.requiringNew().run(() -> {
            Event.delete("title like ?1", PREFIX + "%");
            VoluntaryService.delete("organization like ?1", PREFIX + "%");
        });
    }

    private static long p95(String path, String term) {
        for (int i = 0; i < 5; i++) {
            request(path, term);
        }
        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            request(path, term);
            durations.add((System.nanoTime() - start) / 1_000_000);
        }
        durations.sort(null);
        return durations.get((int) Math.ceil(RUNS * 0.95) - 1);
    }

    private static void request(String path, String term) {
        given().queryParam("q", term).when().get(path).then().statusCode(HttpStatus.SC_OK);
    }

    /** NFR-046: the Search Results Page, for a rare word and for one that matches everything. */
    @Test
    void the_search_results_page_answers_within_300_ms() {
        assertThat(p95("/suche", "zqlast"), lessThanOrEqualTo(P95_LIMIT_MS));
        assertThat(p95("/suche", "treffen workshops"), lessThanOrEqualTo(P95_LIMIT_MS));
    }

    /** NFR-046: the Live Results. */
    @Test
    void the_live_results_answer_within_300_ms() {
        assertThat(p95("/suche/live", "zqlast"), lessThanOrEqualTo(P95_LIMIT_MS));
        assertThat(p95("/suche/live", "treffen"), lessThanOrEqualTo(P95_LIMIT_MS));
    }
}

package tools.cevi.event;

import java.net.URL;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.EventFixture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Event descriptions are rendered unescaped, which is what makes the formatting work — and what
 * would turn a submitted script into stored script running for every visitor. The reduction happens
 * before storing, so these tests check the stored value and the rendered page.
 */
@QuarkusTest
class EventXssTest {

    @TestHTTPEndpoint(EventResource.class)
    @TestHTTPResource
    URL eventEndpoint;

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void script_in_description_is_not_stored() {
        String title = "CLEANUP script_in_description_is_not_stored";

        submitDescription(title, "script_in_description",
                "<p>Programm</p><script>alert('xss')</script><img src=x onerror=alert('xss')>");

        Event stored = Event.findBySlug("script_in_description");
        EventFixture.trackForCleanup(stored.id);

        assertThat(stored.description, not(containsString("<script")));
        assertThat(stored.description, not(containsString("onerror")));
        assertThat(stored.description, containsString("Programm"));

        given().when().get(eventEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(not(containsString("alert('xss')")));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void script_link_in_description_is_not_stored() {
        String title = "CLEANUP script_link_in_description_is_not_stored";

        submitDescription(title, "script_link_in_description",
                "<a href=\"javascript:alert('xss')\">Weitere Informationen</a>");

        Event stored = Event.findBySlug("script_link_in_description");
        EventFixture.trackForCleanup(stored.id);

        assertThat(stored.description, not(containsString("javascript:")));
        assertThat(stored.description, containsString("Weitere Informationen"));
    }

    /**
     * The list no longer renders the description, it renders an excerpt of it — a second path from
     * a stored description to a page, and one that strips tags itself. Stripping is not escaping,
     * so the excerpt has to arrive escaped as well.
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void the_excerpt_in_the_list_is_escaped() {
        String title = "CLEANUP the_excerpt_in_the_list_is_escaped";

        submitDescription(title, "excerpt_is_escaped",
                "<p>Programm &lt;script&gt;alert('xss')&lt;/script&gt; und mehr</p>");

        Event stored = Event.findBySlug("excerpt_is_escaped");
        EventFixture.trackForCleanup(stored.id);

        given().when().get(eventEndpoint).then()
                .statusCode(HttpStatus.SC_OK)
                // The excerpt decodes the references, so the rendering has to encode them again
                .body(not(containsString("<script>alert('xss')</script>")))
                .body(containsString("Programm"));
    }

    private void submitDescription(String title, String slug, String description) {
        Csrf.given()
                .formParam("title", title)
                .formParam("slug", slug)
                .formParam("date", "17.02.2023")
                .formParam("displayDate", "2030-01-01")
                .formParam("location", "Bern")
                .formParam("description", description)
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}

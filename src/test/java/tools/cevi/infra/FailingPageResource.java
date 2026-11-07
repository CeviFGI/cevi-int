package tools.cevi.infra;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A page that cannot be produced, for UC-007 A3. It lives under {@code src/test} and therefore only
 * exists in the test profile: no production page fails on purpose, and the error page has to be
 * reachable without breaking the database or a template under the other tests.
 */
@Path("/test-only/failing-page")
public class FailingPageResource {

    /** Text that must never reach the visitor (FR-028): the page names no technical internals. */
    static final String INTERNAL_DETAIL = "UC-007 A3 internal detail jdbc:sqlite:/secret/path";

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String fail() {
        throw new IllegalStateException(INTERNAL_DETAIL);
    }
}

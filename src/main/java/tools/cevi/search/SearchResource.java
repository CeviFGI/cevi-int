package tools.cevi.search;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import tools.cevi.event.Event;
import tools.cevi.voluntary.VoluntaryService;

/**
 * UC-010: the Search Results Page and the Live Results.
 *
 * <p>UC-010 BR-005: the Search Term travels in the address ({@code /suche?q=...}) and is never
 * stored or logged — this class writes no log line, and the request log names only the path
 * ({@code LoggingFilter}, {@code AppExceptionMapper}). Both endpoints are plain GETs that change
 * nothing.
 */
@Path("suche")
public class SearchResource {

    /** UC-010 A1: at most five events and five offers below the search field. */
    static final int LIVE_LIMIT = 5;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance results(SearchTerm term, List<Hit> events, List<Hit> offers, int count);
        public static native TemplateInstance live(SearchTerm term, List<Hit> events, List<Hit> offers, int count);
    }

    /** UC-010 steps 2–3, A2–A6: works without any script (BR-006). */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance results(@QueryParam("q") String q) {
        SearchTerm term = SearchTerm.of(q);
        if (!term.searchable()) {
            return Templates.results(term, List.of(), List.of(), 0);
        }
        List<Hit> events = events(term, Integer.MAX_VALUE);
        List<Hit> offers = offers(term, Integer.MAX_VALUE);
        return Templates.results(term, events, offers, events.size() + offers.size());
    }

    /** UC-010 A1: a fragment the search script places below the field. */
    @GET
    @Path("live")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance live(@QueryParam("q") String q) {
        SearchTerm term = SearchTerm.of(q);
        if (!term.searchable()) {
            return Templates.live(term, List.of(), List.of(), 0);
        }
        List<Hit> events = events(term, LIVE_LIMIT);
        List<Hit> offers = offers(term, LIVE_LIMIT);
        return Templates.live(term, events, offers, events.size() + offers.size());
    }

    // UC-010 BR-001: only Upcoming Events, as on the Event List, whoever is signed in.
    private static List<Hit> events(SearchTerm term, int limit) {
        return Search.events(term, Event.upcomingEvents(), limit);
    }

    // UC-010 BR-001: every Voluntary Service Offer, as on the Offer List.
    private static List<Hit> offers(SearchTerm term, int limit) {
        return Search.offers(term, VoluntaryService.listAll(), limit);
    }
}

package tools.cevi.feed;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import tools.cevi.event.Event;
import tools.cevi.infra.PublicAddress;
import tools.cevi.voluntary.VoluntaryService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The News Feed (UC-013): every Voluntary Service Offer and every Upcoming Event as an RSS 2.0
 * Feed Item, the most recent Last Change first. A plain {@code GET} that changes nothing and records
 * nothing beyond the request log every page view gets (UC-013 BR-006).
 */
@Path(FeedResource.PATH)
public class FeedResource {

    static final String PATH = "/feed.xml";
    static final String MEDIA_TYPE = "application/rss+xml;charset=UTF-8";

    // UC-013 BR-002: at most 50 Feed Items
    static final int MAX_ITEMS = 50;

    private static final String EVENT_PAGE = "/anlaesse/detail";
    private static final String OFFER_PAGE = "/volontariat/detail";

    @CheckedTemplate
    public static class Templates {
        private Templates() {
        }

        public static native TemplateInstance feed(String siteAddress, String feedAddress, List<FeedItem> items);
    }

    // UC-013 BR-006: no counter and no record of who fetched the News Feed
    @GET
    @Produces(MEDIA_TYPE)
    public TemplateInstance feed(@Context UriInfo uriInfo) {
        // UC-013 BR-001: addresses from the origin of the request, as for sharing without script
        String siteAddress = uriInfo.getBaseUriBuilder().replacePath("/").replaceQuery(null).build().toString();
        String feedAddress = uriInfo.getBaseUriBuilder().replacePath(PATH).replaceQuery(null).build().toString();

        // UC-013 BR-002: each list already cut to the limit, so merging them reads at most twice that
        Stream<FeedItem> events = Event.recentlyChangedUpcoming(MAX_ITEMS).stream()
                .map(e -> FeedItem.of(e, PublicAddress.of(uriInfo, EVENT_PAGE, e.slug).address(), feedAddress));
        Stream<FeedItem> offers = VoluntaryService.recentlyChanged(MAX_ITEMS).stream()
                .map(o -> FeedItem.of(o, PublicAddress.of(uriInfo, OFFER_PAGE, o.slug).address(), feedAddress));
        List<FeedItem> items = Stream.concat(events, offers)
                .sorted(Comparator.comparing(FeedItem::lastChange).reversed())
                .limit(MAX_ITEMS)
                .toList();
        return Templates.feed(siteAddress, feedAddress, items);
    }
}

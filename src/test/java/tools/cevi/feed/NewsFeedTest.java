package tools.cevi.feed;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import tools.cevi.infra.LoggingFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.cevi.event.Event;
import tools.cevi.fixture.Csrf;
import tools.cevi.fixture.EventFixture;
import tools.cevi.fixture.VoluntaryFixture;
import tools.cevi.infra.InstantConverter;
import tools.cevi.voluntary.VoluntaryService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * UC-013 Subscribe to the News Feed — what the News Feed lists and how a change reaches it, read the
 * way a feed reader reads it: as XML.
 */
@QuarkusTest
class NewsFeedTest {

    private static final String FEED = "/feed.xml";
    private static final String EVENTS = "events";
    private static final String OFFERS = "voluntary_services";

    @TestHTTPResource("/")
    URL root;

    @TestHTTPResource("/anlaesse")
    URL eventEndpoint;

    @TestHTTPResource("/volontariat")
    URL offerEndpoint;

    private final List<ExtLogRecord> records = new CopyOnWriteArrayList<>();
    private final Logger requestLog = Logger.getLogger(LoggingFilter.class.getName());
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
            records.add((ExtLogRecord) logRecord);
        }

        @Override
        public void flush() {
            // nothing buffered
        }

        @Override
        public void close() {
            // nothing to release
        }
    };

    @BeforeEach
    void captureRequestLog() {
        requestLog.addHandler(capture);
    }

    @AfterEach
    void releaseRequestLog() {
        requestLog.removeHandler(capture);
    }

    private String origin() {
        return root.getProtocol() + "://" + root.getHost() + ":" + root.getPort();
    }

    /** One item of the News Feed as a feed reader sees it. */
    private record Item(String title, String link, String guid, String category, String pubDate, String description) {
    }

    private static Document feedDocument() throws Exception {
        String body = given().when().get(FEED).then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(startsWith("application/rss+xml"))
                .extract().body().asString();
        return parse(body);
    }

    private static Document parse(String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
    }

    private static List<Item> items() throws Exception {
        NodeList nodes = feedDocument().getElementsByTagName("item");
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);
            items.add(new Item(text(item, "title"), text(item, "link"), text(item, "guid"),
                    text(item, "category"), text(item, "pubDate"), text(item, "description")));
        }
        return items;
    }

    private static String text(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent();
    }

    private static Optional<Item> itemTitled(List<Item> items, String title) {
        return items.stream().filter(i -> i.title().equals(title)).findFirst();
    }

    private static int indexOf(List<Item> items, String title) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).title().equals(title)) {
                return i;
            }
        }
        return -1;
    }

    /** Moves the creation and the Last Change of an entry back, as if it had been recorded an hour ago. */
    private static void recordedAnHourAgo(String table, long id) {
        String anHourAgo = new InstantConverter().convertToDatabaseColumn(Instant.now().minus(1, ChronoUnit.HOURS));
        String update = switch (table) {
            case EVENTS -> "UPDATE events SET created = :moment, lastChange = :moment WHERE id = :id";
            case OFFERS -> "UPDATE voluntary_services SET created = :moment, lastChange = :moment WHERE id = :id";
            default -> throw new IllegalArgumentException(table);
        };
        QuarkusTransaction.requiringNew().run(() -> Event.getEntityManager()
                .createNativeQuery(update)
                .setParameter("moment", anHourAgo)
                .setParameter("id", id)
                .executeUpdate());
    }

    /** The number of rows in every table of the database, by table name. */
    @SuppressWarnings("unchecked")
    private static Map<String, Long> rowsPerTable() {
        return QuarkusTransaction.requiringNew().call(() -> {
            var em = Event.getEntityManager();
            List<String> tables = em.createNativeQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").getResultList();
            Map<String, Long> rows = new TreeMap<>();
            for (String table : tables) {
                Number count = (Number) em.createNativeQuery("SELECT COUNT(*) FROM \"" + table + "\"").getSingleResult();
                rows.put(table, count.longValue());
            }
            return rows;
        });
    }

    private static Event event(long id) {
        return QuarkusTransaction.requiringNew().call(() -> Event.findById((int) id));
    }

    private static VoluntaryService offer(long id) {
        return QuarkusTransaction.requiringNew().call(() -> VoluntaryService.findById((int) id));
    }

    private void saveEvent(Event event, String title) {
        Csrf.given()
                .formParam("id", event.id)
                .formParam("title", title)
                .formParam("slug", event.slug)
                .formParam("date", event.date)
                .formParam("displayDate", event.displayDate.toString())
                .formParam("firstDay", "")
                .formParam("lastDay", "")
                .formParam("location", event.location)
                .formParam("description", event.description)
                .when().post(eventEndpoint)
                .then().statusCode(HttpStatus.SC_OK);
    }

    /** Step 2, BR-001: the News Feed is an RSS 2.0 document about the platform, in German. */
    @Test
    void the_news_feed_is_an_rss_document_of_the_platform() throws Exception {
        Document feed = feedDocument();

        Element rss = feed.getDocumentElement();
        assertThat(rss.getTagName(), equalTo("rss"));
        assertThat(rss.getAttribute("version"), equalTo("2.0"));
        Element channel = (Element) rss.getElementsByTagName("channel").item(0);
        assertThat(text(channel, "title"), equalTo("Cevi International"));
        assertThat(text(channel, "link"), equalTo(origin() + "/"));
        assertThat(text(channel, "language"), equalTo("de-CH"));
        Element self = (Element) channel.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "link").item(0);
        assertThat(self.getAttribute("href"), equalTo(origin() + FEED));
        assertThat(self.getAttribute("rel"), equalTo("self"));
    }

    /** Step 4, BR-002, BR-003: an Upcoming Event is a Feed Item with its details and its Event Page. */
    @Test
    void an_upcoming_event_is_listed_with_its_details() throws Exception {
        long id = EventFixture.createEvent("CLEANUP UC-013 event", LocalDate.now().plusDays(10),
                "1. – 3. Mai", "Prag", "<p>Programm &amp; Kosten</p>");
        Event event = event(id);

        Item item = itemTitled(items(), "CLEANUP UC-013 event").orElseThrow();

        assertThat(item.link(), equalTo(origin() + "/anlaesse/detail?slug=" + event.slug));
        assertThat(item.category(), equalTo("Anlass"));
        assertThat(item.description(), equalTo("<p>1. – 3. Mai · Prag</p><p>Programm &amp; Kosten</p>"));
        assertThat(item.pubDate(), equalTo(DateTimeFormatter.RFC_1123_DATE_TIME.format(event.lastChange.atZone(ZoneOffset.UTC))));
        assertThat(item.guid(), equalTo(origin() + FEED + "#anlass-" + id + "-" + event.lastChange.toEpochMilli()));
    }

    /** Step 4, BR-003: an offer is named by its Organisation and location and links to its Offer Page. */
    @Test
    void an_offer_is_listed_under_its_organisation_and_location() throws Exception {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-013 Organisation", "<p>Ein Jahr</p>",
                "https://example.org");
        VoluntaryService offer = offer(id);

        Item item = itemTitled(items(), "CLEANUP UC-013 Organisation, some location").orElseThrow();

        assertThat(item.link(), equalTo(origin() + "/volontariat/detail?slug=" + offer.slug));
        assertThat(item.category(), equalTo("Volontariat"));
        assertThat(item.description(), equalTo("<p>Ein Jahr</p>"));
        assertThat(item.guid(), equalTo(origin() + FEED + "#volontariat-" + id + "-" + offer.lastChange.toEpochMilli()));
    }

    /** BR-002: an event whose Display Date has passed is no longer listed. */
    @Test
    void a_past_event_is_not_listed() throws Exception {
        EventFixture.createEvent("CLEANUP UC-013 past", LocalDate.now().minusDays(1));
        EventFixture.createEvent("CLEANUP UC-013 today", LocalDate.now());

        List<Item> items = items();

        assertThat(itemTitled(items, "CLEANUP UC-013 past").isPresent(), is(false));
        assertThat(itemTitled(items, "CLEANUP UC-013 today").isPresent(), is(true));
    }

    /** BR-002: the most recent Last Change first, across events and offers. */
    @Test
    void the_most_recent_last_change_comes_first() throws Exception {
        long older = EventFixture.createEvent("CLEANUP UC-013 older");
        recordedAnHourAgo(EVENTS, older);
        VoluntaryFixture.createVoluntaryService("CLEANUP UC-013 newer");

        List<Item> items = items();

        assertThat(indexOf(items, "CLEANUP UC-013 newer, some location"), lessThan(indexOf(items, "CLEANUP UC-013 older")));
    }

    /** BR-002: at most 50 Feed Items, even when more entries exist. */
    @Test
    void the_news_feed_lists_at_most_fifty_items() throws Exception {
        for (int i = 0; i <= FeedResource.MAX_ITEMS; i++) {
            VoluntaryFixture.createVoluntaryService("CLEANUP UC-013 many " + i);
        }

        List<Item> items = items();

        assertThat(items.size(), equalTo(FeedResource.MAX_ITEMS));
        // The first offer is the oldest of the 51 and the one left out.
        assertThat(itemTitled(items, "CLEANUP UC-013 many 0, some location").isPresent(), is(false));
        assertThat(itemTitled(items, "CLEANUP UC-013 many 50, some location").isPresent(), is(true));
    }

    /** A3, BR-004, BR-005: a changed event returns as a new Feed Item marked as updated. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_changed_event_returns_as_a_new_item_marked_as_updated() throws Exception {
        long id = EventFixture.createEvent("CLEANUP UC-013 before", LocalDate.now().plusDays(5));
        recordedAnHourAgo(EVENTS, id);
        Event before = event(id);
        String guidBefore = itemTitled(items(), "CLEANUP UC-013 before").orElseThrow().guid();

        saveEvent(before, "CLEANUP UC-013 after");

        Event after = event(id);
        assertThat(after.created, equalTo(before.created));
        assertThat(after.lastChange, greaterThan(before.lastChange));
        List<Item> items = items();
        Item item = itemTitled(items, "Aktualisiert: CLEANUP UC-013 after").orElseThrow();
        assertThat(item.guid(), not(equalTo(guidBefore)));
        // Only the current state is listed.
        assertThat(items.stream().anyMatch(i -> i.guid().equals(guidBefore)), is(false));
        assertThat(itemTitled(items, "CLEANUP UC-013 before").isPresent(), is(false));
    }

    /** BR-004: a save that changes nothing leaves the Last Change, and so the Feed Item, as it is. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_save_without_change_keeps_the_item() throws Exception {
        long id = EventFixture.createEvent("CLEANUP UC-013 unchanged", LocalDate.now().plusDays(5));
        recordedAnHourAgo(EVENTS, id);
        Event before = event(id);

        saveEvent(before, before.title);

        assertThat(event(id).lastChange, equalTo(before.lastChange));
        Item item = itemTitled(items(), "CLEANUP UC-013 unchanged").orElseThrow();
        assertThat(item.guid(), containsString("-" + before.lastChange.toEpochMilli()));
    }

    /** A3, BR-004, BR-005: a changed offer returns marked as updated, too. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void a_changed_offer_returns_marked_as_updated() throws Exception {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-013 offer");
        recordedAnHourAgo(OFFERS, id);
        VoluntaryService before = offer(id);

        Csrf.given()
                .formParam("id", id)
                .formParam("slug", before.slug)
                .formParam("organization", before.organization)
                .formParam("organizationLink", before.organizationLink)
                .formParam("location", "Peru")
                .formParam("description", before.description)
                .when().post(offerEndpoint)
                .then().statusCode(HttpStatus.SC_OK);

        assertThat(offer(id).created, equalTo(before.created));
        assertThat(itemTitled(items(), "Aktualisiert: CLEANUP UC-013 offer, Peru").isPresent(), is(true));
    }

    /** BR-003: markup in a title or a Date Text reaches the reader as text, and the document stays readable. */
    @Test
    void markup_in_plain_text_stays_text() throws Exception {
        EventFixture.createEvent("CLEANUP UC-013 <b>&</b>", LocalDate.now().plusDays(3),
                "<i>Mai</i>", "Bern & Thun", "desc");

        Item item = itemTitled(items(), "CLEANUP UC-013 <b>&</b>").orElseThrow();

        assertThat(item.description(), startsWith("<p>&lt;i&gt;Mai&lt;/i&gt; · Bern &amp; Thun</p>"));
    }

    /** BR-001: every public page names the News Feed to the browser and shows a visible link to it. */
    @Test
    void every_public_page_names_the_news_feed() {
        for (String page : List.of("/", "/anlaesse", "/volontariat", "/fgi", "/kontakt", "/datenschutzinformation")) {
            String body = given().when().get(page).then().statusCode(HttpStatus.SC_OK).extract().body().asString();
            assertThat(page, body, containsString(
                    "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"Cevi International: Anlässe und Volontariate\" href=\"/feed.xml\">"));
            assertThat(page, body, containsString("<a href=\"/feed.xml\" type=\"application/rss+xml\">RSS-Feed</a>"));
        }
    }

    /**
     * A2: with no offer and no Upcoming Event the News Feed is still a readable RSS document, only
     * without Feed Items and without a moment of the last change. Rendered through the News Feed's own
     * template: the shared test database always holds the Demo Seed, so it is never empty over HTTP.
     */
    @Test
    void with_nothing_to_report_the_news_feed_has_no_items() throws Exception {
        String body = FeedResource.Templates.feed(origin() + "/", origin() + FEED, List.of()).render();

        Document feed = parse(body);

        Element channel = (Element) feed.getDocumentElement().getElementsByTagName("channel").item(0);
        assertThat(text(channel, "title"), equalTo("Cevi International"));
        assertThat(feed.getElementsByTagName("item").getLength(), equalTo(0));
        assertThat(feed.getElementsByTagName("lastBuildDate").getLength(), equalTo(0));
    }

    /** BR-002: a removed event or offer is no longer listed from the next fetch on. */
    @Test
    void a_removed_entry_is_no_longer_listed() throws Exception {
        long event = EventFixture.createEvent("CLEANUP UC-013 removed event", LocalDate.now().plusDays(4));
        long offer = VoluntaryFixture.createVoluntaryService("CLEANUP UC-013 removed offer");
        List<Item> before = items();
        assertThat(itemTitled(before, "CLEANUP UC-013 removed event").isPresent(), is(true));
        assertThat(itemTitled(before, "CLEANUP UC-013 removed offer, some location").isPresent(), is(true));

        QuarkusTransaction.requiringNew().run(() -> {
            Event.deleteById((long) event);
            VoluntaryService.deleteById((int) offer);
        });

        List<Item> after = items();
        assertThat(itemTitled(after, "CLEANUP UC-013 removed event").isPresent(), is(false));
        assertThat(itemTitled(after, "CLEANUP UC-013 removed offer, some location").isPresent(), is(false));
    }

    /** BR-004: an offer saved without a change keeps its Last Change, and so its Feed Item. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_offer_saved_without_change_keeps_the_item() throws Exception {
        long id = VoluntaryFixture.createVoluntaryService("CLEANUP UC-013 offer unchanged", "<p>Ein Jahr in Peru</p>",
                "https://example.org");
        recordedAnHourAgo(OFFERS, id);
        VoluntaryService before = offer(id);

        Csrf.given()
                .formParam("id", id)
                .formParam("slug", before.slug)
                .formParam("organization", before.organization)
                .formParam("organizationLink", before.organizationLink)
                .formParam("location", before.location)
                .formParam("description", before.description)
                .when().post(offerEndpoint)
                .then().statusCode(HttpStatus.SC_OK);

        assertThat(offer(id).lastChange, equalTo(before.lastChange));
        Item item = itemTitled(items(), "CLEANUP UC-013 offer unchanged, some location").orElseThrow();
        assertThat(item.guid(), containsString("-" + before.lastChange.toEpochMilli()));
    }

    /**
     * Success postcondition "No data is stored or changed on the platform", BR-006: fetching the News
     * Feed adds no row to any table — no counter of subscribers, no record of who fetched it — and
     * leaves the Last Change of the entries as it was, so their Feed Items keep their identity.
     */
    @Test
    void fetching_the_news_feed_stores_and_changes_nothing() throws Exception {
        long id = EventFixture.createEvent("CLEANUP UC-013 stores nothing", LocalDate.now().plusDays(2));
        recordedAnHourAgo(EVENTS, id);
        Event original = event(id);
        Map<String, Long> rowsBefore = rowsPerTable();
        assertThat(rowsBefore, hasKey("events"));

        String first = itemTitled(items(), "CLEANUP UC-013 stores nothing").orElseThrow().guid();
        String second = itemTitled(items(), "CLEANUP UC-013 stores nothing").orElseThrow().guid();

        assertThat(rowsPerTable(), equalTo(rowsBefore));
        Event after = event(id);
        assertThat(after.created, equalTo(original.created));
        assertThat(after.lastChange, equalTo(original.lastChange));
        assertThat(second, equalTo(first));
    }

    /**
     * BR-006, NFR-031: the fetch is logged like any other page view — at INFO with method and path —
     * and no record at INFO or above names the client's address.
     */
    @Test
    void fetching_the_news_feed_logs_no_client_address() {
        given().when().get(FEED).then().statusCode(HttpStatus.SC_OK);

        List<String> logged = records.stream()
                .filter(r -> r.getLevel().intValue() >= Level.INFO.intValue())
                .map(ExtLogRecord::getFormattedMessage)
                .toList();
        assertThat(logged.contains("Request GET /feed.xml"), is(true));
        assertThat(logged.stream().filter(m -> m.contains("127.0.0.1") || m.contains("from IP")).toList(), empty());
    }
}

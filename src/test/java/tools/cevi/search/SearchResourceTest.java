package tools.cevi.search;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.cevi.contact.ContactFormEntry;
import tools.cevi.event.Event;
import tools.cevi.fixture.EventFixture;
import tools.cevi.infra.Slug;
import tools.cevi.voluntary.VoluntaryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * UC-010 Search Events and Offers — the Search Results Page, the Live Results fragment and the
 * Offer List entry a search leads to, over HTTP against the real database. The behaviour in the
 * browser (typing, keyboard, scrolling) is covered by {@code SearchE2ETest}; the response time of
 * NFR-046 by {@code SearchResponseTimeTest}.
 *
 * <p>Every entry created here carries an invented word beginning with {@code zq}, so the demo data
 * of the test database can never be what a test finds.
 */
@QuarkusTest
class SearchResourceTest {

    private final List<Integer> offers = new ArrayList<>();
    private final List<Integer> messages = new ArrayList<>();

    @AfterEach
    void removeOffersAndMessages() {
        QuarkusTransaction.begin();
        offers.forEach(id -> VoluntaryService.deleteById(id));
        messages.forEach(id -> ContactFormEntry.deleteById(id));
        QuarkusTransaction.commit();
        offers.clear();
        messages.clear();
    }

    // --- Test data ---------------------------------------------------------------------------

    private static Event event(String title, String date, String location, String description, LocalDate displayDate) {
        QuarkusTransaction.begin();
        Event event = new Event();
        event.title = title;
        event.slug = Slug.of(title).toString();
        event.date = date;
        event.location = location;
        event.description = description;
        event.displayDate = displayDate;
        event.persist();
        QuarkusTransaction.commit();
        EventFixture.trackForCleanup(event.id);
        return event;
    }

    private static Event event(String title, String description) {
        return event(title, "irgendwann", "irgendwo", description, LocalDate.now().plusDays(10));
    }

    private VoluntaryService offer(String organization, String location, String description, String link) {
        QuarkusTransaction.begin();
        VoluntaryService offer = new VoluntaryService();
        offer.organization = organization;
        offer.organizationLink = link;
        offer.location = location;
        offer.description = description;
        offer.persist();
        QuarkusTransaction.commit();
        offers.add(offer.id);
        return offer;
    }

    private VoluntaryService offer(String organization, String description) {
        return offer(organization, "irgendwo", description, "https://example.org/");
    }

    private static String search(String query) {
        return given().queryParam("q", query).when().get("/suche").then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    private static String live(String query) {
        return given().queryParam("q", query).when().get("/suche/live").then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    /** The markup of one group of the Search Results Page, or an empty string when it is absent. */
    private static String group(String body, String id) {
        int start = body.indexOf("aria-labelledby=\"" + id + "\"");
        if (start < 0) {
            return "";
        }
        return body.substring(start, body.indexOf("</section>", start));
    }

    private static String eventGroup(String body) {
        return group(body, "treffer-anlaesse");
    }

    private static String offerGroup(String body) {
        return group(body, "treffer-volontariat");
    }

    /** The titles of the hits in a group, in the order they are shown. */
    private static List<String> titles(String group) {
        Matcher matcher = Pattern.compile("<h3 class=\"card__title\"><a href=\"[^\"]*\">([^<]*)</a>").matcher(group);
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            titles.add(matcher.group(1));
        }
        return titles;
    }

    private static List<String> excerpts(String group) {
        Matcher matcher = Pattern.compile("<p class=\"card__excerpt\">([^<]*)</p>").matcher(group);
        List<String> excerpts = new ArrayList<>();
        while (matcher.find()) {
            excerpts.add(matcher.group(1));
        }
        return excerpts;
    }

    private static String searchFieldValue(String body) {
        Matcher matcher = Pattern.compile("<input class=\"site-search__input\"[^>]*value=\"([^\"]*)\"").matcher(body);
        assertThat("the page carries the search field", matcher.find(), is(true));
        return matcher.group(1);
    }

    // --- Main success scenario ---------------------------------------------------------------

    /**
     * Main success scenario, steps 1–3: the Search Results Page shows the Search Term in the
     * search field, the number of matches, and the events and offers in two groups, each entry
     * with its title or Organisation, location, the Date Text of an event, and an excerpt.
     */
    @Test
    void the_search_results_page_shows_matching_events_and_offers_in_two_groups() {
        Event event = event("CLEANUP Zqhaupt Workcamp", "3. bis 9. August", "Accra",
                "<p>Bauen und lernen in Ghana.</p>", LocalDate.now().plusDays(5));
        VoluntaryService offer = offer("Zqhaupt Verein", "Lima", "<p>Ein Jahr in Peru.</p>", "https://example.org/");

        String body = search("zqhaupt");

        assertThat(searchFieldValue(body), is("zqhaupt"));
        assertThat(body, containsString("2 Treffer"));
        String events = eventGroup(body);
        assertThat(titles(events), is(List.of(event.title)));
        assertThat(events, containsString("3. bis 9. August"));
        assertThat(events, containsString("Accra"));
        assertThat(events, containsString("Bauen und lernen in Ghana."));
        String offers = offerGroup(body);
        assertThat(titles(offers), is(List.of(offer.organization)));
        assertThat(offers, containsString("Lima"));
        assertThat(offers, containsString("Ein Jahr in Peru."));
    }

    /** Main success scenario, step 5: an event leads to its Event Page. */
    @Test
    void an_event_hit_leads_to_its_event_page() {
        Event event = event("CLEANUP Zqziel Treffen", "Beschreibung");

        String body = search("zqziel");

        assertThat(eventGroup(body), containsString("href=\"/anlaesse/detail?slug=" + event.slug + "\""));
        given().when().get("/anlaesse/detail?slug=" + event.slug).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString(event.title));
    }

    /**
     * Main success scenario, step 5 and UC-003 A5: an offer leads to its entry in the Offer List — brought into
     * view by the address's fragment, marked as the one chosen, its complete Description open.
     */
    @Test
    void an_offer_hit_leads_to_its_marked_and_opened_entry_in_the_offer_list() {
        String longDescription = "<p>" + "Zqvoll ist der Anfang einer langen Beschreibung. ".repeat(10)
                + "Und hier steht das Ende des Textes.</p>";
        VoluntaryService offer = offer("Zqwahl Verein", longDescription);
        String address = "/volontariat?angebot=" + offer.id + "#angebot-" + offer.id;

        assertThat(offerGroup(search("zqwahl")), containsString("href=\"" + address + "\""));

        String list = given().when().get("/volontariat?angebot=" + offer.id).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertThat(list, containsString("<article class=\"card card--chosen\" id=\"angebot-" + offer.id + "\">"));
        assertThat(list, containsString("Dein Suchtreffer"));
        assertThat(list, containsString("<details class=\"disclosure\" open>"));
        assertThat(list, containsString("Und hier steht das Ende des Textes."));
    }

    /** Step 5: only the chosen offer is marked; the others stay as they are on the Offer List. */
    @Test
    void only_the_chosen_offer_is_marked() {
        VoluntaryService chosen = offer("Zqnur Eins", "Text");
        VoluntaryService other = offer("Zqnur Zwei", "Text");

        String list = given().when().get("/volontariat?angebot=" + chosen.id).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertThat(list, containsString("<article class=\"card\" id=\"angebot-" + other.id + "\">"));
        assertThat(Pattern.compile("card--chosen").matcher(list).results().count(), is(1L));
    }

    // --- BR-008: the excerpt ----------------------------------------------------------------

    /**
     * BR-008: the excerpt is the passage around the first occurrence — at most 160
     * characters, with some text before the match and an ellipsis at each cut end.
     */
    @Test
    void the_excerpt_is_the_passage_around_the_first_occurrence() {
        String before = "Vorher steht ein langer Text ohne das Wort. ".repeat(8);
        String after = " Danach folgt noch mehr Text, der nicht wichtig ist.".repeat(8);
        event("CLEANUP Ausschnitt Mitte", "<p>" + before + "Hier kommt zqmitte vor." + after + "</p>");

        String excerpt = excerpts(eventGroup(search("zqmitte"))).getFirst();

        assertThat(excerpt, startsWith("…"));
        assertThat(excerpt, endsWith("…"));
        assertThat(excerpt, containsString("Hier kommt zqmitte vor."));
        assertThat(excerpt, not(startsWith("…zqmitte")));
        assertThat(excerpt.length(), lessThanOrEqualTo(160 + 2));
    }

    /** BR-008: with several words, the passage is the one around the earliest of them. */
    @Test
    void with_several_words_the_excerpt_shows_the_earliest_occurrence() {
        String filler = " Füllsatz ohne Bedeutung für die Suche.".repeat(10);
        event("CLEANUP Ausschnitt Wörter", "<p>Anfang." + filler + " Zuerst zqfrueh." + filler
                + " Später zqspaet." + filler + "</p>");

        String excerpt = excerpts(eventGroup(search("zqspaet zqfrueh"))).getFirst();

        assertThat(excerpt, containsString("Zuerst zqfrueh."));
        assertThat(excerpt, not(containsString("zqspaet")));
    }

    /**
     * Step 3: when the Search Term occurs only outside the Description, the excerpt is the
     * beginning of the Description, as on the lists.
     */
    @Test
    void without_a_match_in_the_description_the_excerpt_is_its_beginning() {
        String description = "<p>" + "Dieser Text beginnt hier und ist lang genug für einen Schnitt. ".repeat(5) + "</p>";
        event("CLEANUP Zqtitelonly Lager", description);

        String excerpt = excerpts(eventGroup(search("zqtitelonly"))).getFirst();

        assertThat(excerpt, startsWith("Dieser Text beginnt hier"));
        assertThat(excerpt, endsWith("…"));
    }

    // --- Alternative flows -------------------------------------------------------------------

    /** A1: at most five events and five offers, each with title and location, and a link to all results. */
    @Test
    void live_results_show_at_most_five_per_group_and_a_link_to_all_results() {
        for (int i = 1; i <= 6; i++) {
            event("CLEANUP Zqlive Anlass " + i, "Text");
            offer("Zqlive Verein " + i, "Ort " + i, "Text", "https://example.org/");
        }

        String body = live("zqlive");

        assertThat(Pattern.compile("Zqlive Anlass \\d").matcher(body).results().count(), is(5L));
        assertThat(Pattern.compile("Zqlive Verein \\d").matcher(body).results().count(), is(5L));
        assertThat(body, containsString("Ort 1"));
        assertThat(body, containsString("10 Treffer"));
        assertThat(body, containsString("href=\"/suche?q=zqlive\""));
    }

    /** A1 step 4: a Live Result leads where a hit on the Search Results Page leads (step 5). */
    @Test
    void a_live_result_leads_to_the_same_place_as_a_hit() {
        Event event = event("CLEANUP Zqlivelink Anlass", "Text");
        VoluntaryService offer = offer("Zqlivelink Verein", "Text");

        String body = live("zqlivelink");

        assertThat(body, containsString("href=\"/anlaesse/detail?slug=" + event.slug + "\""));
        assertThat(body, containsString("href=\"/volontariat?angebot=" + offer.id + "#angebot-" + offer.id + "\""));
    }

    /** A1 step 3: fewer than two letters or digits give no Live Results; punctuation does not count. */
    @Test
    void live_results_need_two_letters_or_digits() {
        event("CLEANUP Z Einbuchstabe", "Text");

        for (String query : List.of("z   ", "--", "z-")) {
            assertThat(query, live(query), not(containsString("live-results__list")));
        }
    }

    /**
     * A2: nothing matched — the page says so, keeps the Search Term in the field, and offers the
     * way to the Event List, the Offer List and the Contact Form.
     */
    @Test
    void no_match_says_so_and_offers_the_lists_and_the_contact_form() {
        String body = search("zqnirgends");

        assertThat(searchFieldValue(body), is("zqnirgends"));
        assertThat(body, containsString("Nichts gefunden"));
        assertThat(body, containsString("href=\"/anlaesse\""));
        assertThat(body, containsString("href=\"/volontariat\""));
        assertThat(body, containsString("href=\"/kontakt\""));
        assertThat(eventGroup(body), is(""));
        assertThat(offerGroup(body), is(""));
    }

    /**
     * A3: an empty, missing or one-letter Search Term, or one of spaces and punctuation only,
     * starts no search and shows the hint.
     */
    @Test
    void a_search_term_below_two_letters_or_digits_is_not_searched() {
        event("CLEANUP Z kurz", "Text");
        for (String query : List.of("", "z", " z ", "   ", "--", "?!", "z-", "/ . /")) {
            String body = search(query);

            assertThat(query, body, containsString("mindestens zwei Buchstaben oder Ziffern"));
            assertThat(query, eventGroup(body), is(""));
            assertThat(query, body, not(containsString("Nichts gefunden")));
        }
        String withoutParameter = given().when().get("/suche").then()
                .statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertThat(withoutParameter, containsString("mindestens zwei Buchstaben oder Ziffern"));
    }

    /** A3: two letters or digits are enough, even with spaces or punctuation between them. */
    @Test
    void two_letters_or_digits_start_a_search() {
        for (String query : List.of("zq", "z q", "z-9", "ü2")) {
            assertThat(query, search(query), not(containsString("mindestens zwei Buchstaben oder Ziffern")));
        }
    }

    /**
     * A4: a Search Term longer than 100 characters is searched by its first 100 only, and the
     * shortened term is shown in the field. The word beyond the cut would match nothing.
     */
    @Test
    void a_search_term_longer_than_100_characters_is_cut() {
        event("CLEANUP Zqlang Treffen", "Text");
        String head = "zqlang";
        String term = head + " ".repeat(100 - head.length()) + "zqjenseits";

        String body = search(term);

        assertThat(searchFieldValue(body), is("zqlang"));
        assertThat(titles(eventGroup(body)), is(List.of("CLEANUP Zqlang Treffen")));
    }

    /** A4: the field shows exactly the first 100 characters of a long term. */
    @Test
    void the_shortened_search_term_has_100_characters() {
        String term = "zq" + "a".repeat(150);

        assertThat(searchFieldValue(search(term)), is(term.substring(0, 100)));
    }

    /** A5: only events match — the offer group is left out rather than shown empty. */
    @Test
    void a_group_without_matches_is_left_out() {
        event("CLEANUP Zqeinzeln Anlass", "Text");

        String body = search("zqeinzeln");

        assertThat(titles(eventGroup(body)), is(List.of("CLEANUP Zqeinzeln Anlass")));
        assertThat(offerGroup(body), is(""));
        assertThat(body, not(containsString("treffer-volontariat")));
    }

    /** A5: only offers match — the event group is left out. */
    @Test
    void only_offers_matching_leaves_out_the_event_group() {
        offer("Zqnuroffer Verein", "Text");

        String body = search("zqnuroffer");

        assertThat(body, not(containsString("treffer-anlaesse")));
        assertThat(titles(offerGroup(body)), is(List.of("Zqnuroffer Verein")));
    }

    /**
     * A6 and BR-005: the Search Results Page is opened by its address alone and shows the results
     * for that Search Term at that moment.
     */
    @Test
    void the_address_alone_shows_the_results_at_that_moment() {
        assertThat(titles(eventGroup(search("zqspaeter"))), is(List.of()));

        event("CLEANUP Zqspaeter Anlass", "Text");

        assertThat(titles(eventGroup(search("zqspaeter"))), is(List.of("CLEANUP Zqspaeter Anlass")));
    }

    /** A7: the chosen offer was deleted — the Offer List is shown without a marked entry. */
    @Test
    void a_deleted_offer_shows_the_offer_list_without_a_marked_entry() {
        VoluntaryService offer = offer("Zqweg Verein", "Text");
        String address = "/volontariat?angebot=" + offer.id;
        QuarkusTransaction.begin();
        VoluntaryService.deleteById(offer.id);
        QuarkusTransaction.commit();

        given().when().get(address).then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("<h1>Volontariat</h1>"))
                .body(not(containsString("card--chosen")))
                .body(not(containsString("Dein Suchtreffer")));
    }

    /**
     * A8: the chosen event was deleted or its Short Name changed — the visitor learns that the
     * page does not exist and gets the way back to the Event List, as in UC-001 A2.
     */
    @Test
    void a_deleted_event_shows_that_the_page_does_not_exist() {
        Event event = event("CLEANUP Zqgeloescht Anlass", "Text");
        String href = "/anlaesse/detail?slug=" + event.slug;
        assertThat(eventGroup(search("zqgeloescht")), containsString(href));
        QuarkusTransaction.begin();
        Event.deleteById((long) event.id);
        QuarkusTransaction.commit();

        given().when().get(href).then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString("Diese Seite gibt es nicht"))
                .body(containsString("href=\"/anlaesse\""));
    }

    // --- Business rules ----------------------------------------------------------------------

    /** BR-001: an event whose Display Date has passed is not found, as on the Event List. */
    @Test
    void a_past_event_is_not_found() {
        event("CLEANUP Zqvorbei Gestern", "x", "x", "Text", LocalDate.now().minusDays(1));
        event("CLEANUP Zqvorbei Heute", "x", "x", "Text", LocalDate.now());

        assertThat(titles(eventGroup(search("zqvorbei"))), is(List.of("CLEANUP Zqvorbei Heute")));
    }

    /** BR-001: a signed-in Administrator finds the same entries as any visitor. */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void an_administrator_finds_the_same_entries() {
        event("CLEANUP Zqadmin Gestern", "x", "x", "Text", LocalDate.now().minusDays(1));
        event("CLEANUP Zqadmin Morgen", "x", "x", "Text", LocalDate.now().plusDays(1));

        assertThat(titles(eventGroup(search("zqadmin"))), is(List.of("CLEANUP Zqadmin Morgen")));
    }

    /** BR-001: every Voluntary Service Offer can be found, Contact Messages never. */
    @Test
    void contact_messages_are_never_searched() {
        QuarkusTransaction.begin();
        ContactFormEntry entry = new ContactFormEntry();
        entry.message = "Ich interessiere mich für zqnachricht.";
        entry.persist();
        QuarkusTransaction.commit();
        messages.add(entry.getId());

        assertThat(search("zqnachricht"), containsString("Nichts gefunden"));
    }

    /** BR-001: Information Pages are never searched — a word of the FGI page finds nothing. */
    @Test
    void information_pages_are_never_searched() {
        String fgi = given().when().get("/fgi").then().statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertThat(fgi, containsString("Fachgruppe"));

        String body = search("fachgruppe");
        assertThat(eventGroup(body) + offerGroup(body), not(containsString("href=\"/fgi\"")));
    }

    /** BR-002: an event is found by its title, Date Text, location and Description. */
    @Test
    void an_event_is_found_by_every_field_a_visitor_reads() {
        event("CLEANUP Zqtitel Anlass", "x", "x", "Text", LocalDate.now().plusDays(1));
        event("CLEANUP Datum Anlass", "Zqdatum 2026", "x", "Text", LocalDate.now().plusDays(1));
        event("CLEANUP Ort Anlass", "x", "Zqort", "Text", LocalDate.now().plusDays(1));
        event("CLEANUP Text Anlass", "x", "x", "<p>Mit zqtext darin.</p>", LocalDate.now().plusDays(1));

        assertThat(titles(eventGroup(search("zqtitel"))), is(List.of("CLEANUP Zqtitel Anlass")));
        assertThat(titles(eventGroup(search("zqdatum"))), is(List.of("CLEANUP Datum Anlass")));
        assertThat(titles(eventGroup(search("zqort"))), is(List.of("CLEANUP Ort Anlass")));
        assertThat(titles(eventGroup(search("zqtext"))), is(List.of("CLEANUP Text Anlass")));
    }

    /** BR-002: an offer is found by its Organisation, location and Description — not by its link. */
    @Test
    void an_offer_is_found_by_every_field_a_visitor_reads_but_not_by_its_link() {
        offer("Zqorganisation Verein", "x", "Text", "https://example.org/");
        offer("Ort Verein", "Zqofferort", "Text", "https://example.org/");
        offer("Text Verein", "x", "<p>Mit zqoffertext darin.</p>", "https://example.org/");
        offer("Link Verein", "x", "Text", "https://zqofferlink.example.org/");

        assertThat(titles(offerGroup(search("zqorganisation"))), is(List.of("Zqorganisation Verein")));
        assertThat(titles(offerGroup(search("zqofferort"))), is(List.of("Ort Verein")));
        assertThat(titles(offerGroup(search("zqoffertext"))), is(List.of("Text Verein")));
        assertThat(search("zqofferlink"), containsString("Nichts gefunden"));
    }

    /**
     * BR-002: the Description is searched as the text a visitor reads — the visible text of a link
     * and of a table is found, a link address and the markup itself are not.
     */
    @Test
    void the_description_is_searched_as_read_text_not_as_markup() {
        event("CLEANUP Markup Anlass", "<p>Siehe <a href=\"https://zqadresse.example.org/\">zqlinktext</a>.</p>"
                + "<table><tr><td>zqzelle</td></tr></table><p><strong>zqfett</strong>gedruckt</p>");

        assertThat(titles(eventGroup(search("zqlinktext"))), is(List.of("CLEANUP Markup Anlass")));
        assertThat(titles(eventGroup(search("zqzelle"))), is(List.of("CLEANUP Markup Anlass")));
        assertThat(titles(eventGroup(search("zqfettgedruckt"))), is(List.of("CLEANUP Markup Anlass")));
        assertThat(search("zqadresse"), containsString("Nichts gefunden"));
        assertThat(search("strong"), not(containsString("CLEANUP Markup Anlass")));
        assertThat(search("href"), not(containsString("CLEANUP Markup Anlass")));
    }

    /** BR-003: all words must occur, in any field and in any order. */
    @Test
    void every_word_must_occur_in_any_field_and_order() {
        event("CLEANUP Zqalle Camp", "x", "Zqbeide", "<p>Mit zqdritte.</p>", LocalDate.now().plusDays(1));

        assertThat(titles(eventGroup(search("zqdritte zqalle zqbeide"))), is(List.of("CLEANUP Zqalle Camp")));
        assertThat(search("zqalle zqfehlt"), containsString("Nichts gefunden"));
    }

    /** BR-003: a word matches a word of the entry that begins with it — not one that contains it. */
    @Test
    void a_word_matches_from_the_beginning_of_a_word() {
        event("CLEANUP Zqpolen Reise", "<p>Ein Zqfreiwilligeneinsatz.</p>");

        assertThat(titles(eventGroup(search("zqpole"))), is(List.of("CLEANUP Zqpolen Reise")));
        assertThat(titles(eventGroup(search("zqfreiwillig"))), is(List.of("CLEANUP Zqpolen Reise")));
        assertThat(search("qpolen"), containsString("Nichts gefunden"));
        assertThat(search("zqpolenx"), containsString("Nichts gefunden"));
    }

    /** BR-003: upper and lower case are not distinguished. */
    @Test
    void case_is_not_distinguished() {
        event("CLEANUP ZqGross Anlass", "Text");

        assertThat(titles(eventGroup(search("ZQGROSS"))), is(List.of("CLEANUP ZqGross Anlass")));
        assertThat(titles(eventGroup(search("zqgross"))), is(List.of("CLEANUP ZqGross Anlass")));
    }

    /** BR-003: a letter with an accent or umlaut matches the same letter without it, both ways. */
    @Test
    void accents_and_umlauts_match_the_bare_letter() {
        event("CLEANUP Zqakzent Poděbrady", "x", "Zürich", "Text", LocalDate.now().plusDays(1));
        event("CLEANUP Zqohne Zurich", "Text");

        assertThat(titles(eventGroup(search("zqakzent podebrady"))), is(List.of("CLEANUP Zqakzent Poděbrady")));
        assertThat(titles(eventGroup(search("zqakzent zurich"))), is(List.of("CLEANUP Zqakzent Poděbrady")));
        assertThat(titles(eventGroup(search("zqohne zürich"))), is(List.of("CLEANUP Zqohne Zurich")));
    }

    /** BR-003: ł, ø and đ match l, o and d; ß is not folded. */
    @Test
    void letters_with_a_stroke_are_folded_but_sharp_s_is_not() {
        event("CLEANUP Zqstrich Łódź Ørsted Đakovo", "Text");
        event("CLEANUP Zqscharf Straße", "Text");

        assertThat(titles(eventGroup(search("zqstrich lodz orsted dakovo"))),
                is(List.of("CLEANUP Zqstrich Łódź Ørsted Đakovo")));
        assertThat(titles(eventGroup(search("zqscharf straße"))), is(List.of("CLEANUP Zqscharf Straße")));
        assertThat(search("zqscharf strasse"), containsString("Nichts gefunden"));
    }

    /**
     * BR-003: every character other than a letter or a digit separates words, in the
     * entry and in the Search Term alike.
     */
    @Test
    void punctuation_separates_words() {
        event("CLEANUP Zqtrenn YMCA/YWCA Cevi-Lager", "Text");

        assertThat(titles(eventGroup(search("zqtrenn ywca"))), is(List.of("CLEANUP Zqtrenn YMCA/YWCA Cevi-Lager")));
        assertThat(titles(eventGroup(search("zqtrenn lager"))), is(List.of("CLEANUP Zqtrenn YMCA/YWCA Cevi-Lager")));
        assertThat(titles(eventGroup(search("zqtrenn cevi-lager"))), is(List.of("CLEANUP Zqtrenn YMCA/YWCA Cevi-Lager")));
    }

    /**
     * BR-004: events rank by the best field — title, then location or Date Text, then
     * Description — and entries of the same rank follow the Display Date, nearest first.
     */
    @Test
    void events_rank_by_best_field_then_by_display_date() {
        LocalDate today = LocalDate.now();
        event("CLEANUP Rang Beschreibung", "x", "x", "<p>zqrang</p>", today.plusDays(1));
        event("CLEANUP Rang Ort", "x", "Zqrang", "Text", today.plusDays(2));
        event("CLEANUP Zqrang Titel", "x", "x", "Text", today.plusDays(3));
        event("CLEANUP Rang Datum", "Zqrang Woche", "x", "Text", today.plusDays(4));
        event("CLEANUP Zqrang Titel Früher", "x", "x", "Text", today.plusDays(2));

        assertThat(titles(eventGroup(search("zqrang"))), is(List.of(
                "CLEANUP Zqrang Titel Früher", "CLEANUP Zqrang Titel",
                "CLEANUP Rang Ort", "CLEANUP Rang Datum",
                "CLEANUP Rang Beschreibung")));
    }

    /**
     * BR-004: an entry ranks by the best field in which any word occurs, and offers of the same
     * rank follow the Organisation in alphabetical order.
     */
    @Test
    void offers_rank_by_best_field_then_alphabetically() {
        offer("Aaa Verein", "x", "<p>zqorg und zqmehr</p>", "https://example.org/");
        offer("Zqorg Beta", "x", "<p>zqmehr</p>", "https://example.org/");
        offer("Zqorg Älpha", "x", "<p>zqmehr</p>", "https://example.org/");
        offer("Mitte Verein", "Zqorg", "<p>zqmehr</p>", "https://example.org/");

        assertThat(titles(offerGroup(search("zqmehr zqorg"))), is(List.of(
                "Zqorg Älpha", "Zqorg Beta", "Mitte Verein", "Aaa Verein")));
    }

    /**
     * BR-004: events of the same rank and the same Display Date follow the order in which they
     * were recorded, the earlier first — whatever their titles.
     */
    @Test
    void events_of_equal_rank_and_display_date_follow_the_order_recorded() {
        LocalDate day = LocalDate.now().plusDays(5);
        event("CLEANUP Zqgleichtag Zulu", "x", "x", "Text", day);
        event("CLEANUP Zqgleichtag Alpha", "x", "x", "Text", day);
        event("CLEANUP Zqgleichtag Mike", "x", "x", "Text", day);

        assertThat(titles(eventGroup(search("zqgleichtag"))), is(List.of(
                "CLEANUP Zqgleichtag Zulu", "CLEANUP Zqgleichtag Alpha", "CLEANUP Zqgleichtag Mike")));
    }

    /**
     * BR-004: offers of the same rank and the same Organisation follow the order in which they
     * were recorded, the earlier first — whatever their location.
     */
    @Test
    void offers_of_equal_rank_and_organisation_follow_the_order_recorded() {
        offer("Zqgleichorg Verein", "Zürich", "Text", "https://example.org/");
        offer("Zqgleichorg Verein", "Aarau", "Text", "https://example.org/");
        offer("Zqgleichorg Verein", "Malmö", "Text", "https://example.org/");

        String group = offerGroup(search("zqgleichorg"));
        assertThat(group.indexOf("Zürich") < group.indexOf("Aarau"), is(true));
        assertThat(group.indexOf("Aarau") < group.indexOf("Malmö"), is(true));
    }

    /** BR-004: the same Search Term over the same entries always yields the same order. */
    @Test
    void the_order_is_stable() {
        for (int i = 0; i < 5; i++) {
            offer("Zqgleich Verein", "Ort " + i, "Text", "https://example.org/");
        }

        String first = offerGroup(search("zqgleich"));
        for (int i = 0; i < 3; i++) {
            assertThat(offerGroup(search("zqgleich")), is(first));
        }
    }

    /**
     * BR-005: searching changes no data, and neither the Search Term nor who searched reaches the
     * application log; the log names the address without the Search Term.
     */
    @Test
    void searching_changes_nothing_and_logs_no_search_term() {
        event("CLEANUP Zqgeheim Anlass", "Text");
        long events = Event.count();
        long offerCount = VoluntaryService.count();
        long messageCount = ContactFormEntry.count();
        List<String> logged = new ArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                logged.add(logRecord.getMessage() + " " + Arrays.toString(logRecord.getParameters()));
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
        Logger root = Logger.getLogger("");
        root.addHandler(capture);
        try {
            search("zqgeheim");
            live("zqgeheim");
            search("zqgeheim zqnichts");
        } finally {
            root.removeHandler(capture);
        }

        assertThat(Event.count(), is(events));
        assertThat(VoluntaryService.count(), is(offerCount));
        assertThat(ContactFormEntry.count(), is(messageCount));
        assertThat(logged, everyItem(not(containsString("zqgeheim"))));
        assertThat(logged, hasItem(containsString("/suche")));
    }

    /** BR-006: searching is a plain form submission; the page works without the script. */
    @Test
    void the_search_is_a_plain_form_that_needs_no_script() {
        String body = search("zqform");

        assertThat(body, containsString("<form class=\"site-search\" action=\"/suche\" method=\"get\" role=\"search\""));
        assertThat(body, containsString("name=\"q\""));
        assertThat(body, containsString("<button class=\"site-search__button\" type=\"submit\">"));
    }

    /** BR-007: every public page offers the search field, in the same place in the header. */
    @Test
    void every_public_page_offers_the_search_field() {
        Event event = event("CLEANUP Zqueberall Anlass", "Text");
        List<String> pages = List.of("/", "/anlaesse", "/volontariat", "/fgi", "/kontakt",
                "/datenschutzinformation", "/auth/login", "/version", "/suche?q=zq",
                "/anlaesse/detail?slug=" + event.slug);
        for (String path : pages) {
            String body = given().redirects().follow(true).when().get(path).then()
                    .statusCode(HttpStatus.SC_OK).extract().body().asString();
            String header = body.substring(body.indexOf("<header class=\"site-header\">"), body.indexOf("</header>"));

            assertThat(path, header, containsString("<form class=\"site-search\" action=\"/suche\""));
        }
    }

    /** NFR-047: a term made of database syntax is just text — it finds nothing and breaks nothing. */
    @Test
    void database_syntax_in_the_search_term_is_just_text() {
        event("CLEANUP Zqsql Anlass", "Text");

        assertThat(search("zqsql' OR 1=1 --"), containsString("Nichts gefunden"));
        assertThat(search("%"), containsString("mindestens zwei Buchstaben oder Ziffern"));
        // A3: wildcards alone hold no letter or digit; around a word they are no wildcard either.
        assertThat(search("%%"), containsString("mindestens zwei Buchstaben oder Ziffern"));
        assertThat(search("%zqs_l%"), containsString("Nichts gefunden"));
        assertThat(titles(eventGroup(search("zqsql"))), is(List.of("CLEANUP Zqsql Anlass")));
    }

    /** A1 and C-021: the search script is served from the own origin and talks only to it. */
    @Test
    void the_search_script_talks_only_to_its_own_origin() {
        String source = given().when().get("/js/search.js").then()
                .statusCode(HttpStatus.SC_OK).extract().body().asString();

        assertThat(source, not(containsString("http")));
        assertThat(source, not(containsString("sendBeacon")));
        assertThat(source, not(containsString("XMLHttpRequest")));
        assertThat(source, containsString("fetch('/suche/live?q='"));
    }
}

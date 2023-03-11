package tools.cevi.fixture;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import tools.cevi.event.Event;
import tools.cevi.infra.Slug;

import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static io.restassured.RestAssured.given;

public class EventFixture implements QuarkusTestAfterEachCallback {
    private static final List<Long> createdEvents = new LinkedList<>();

    public static long createEvent(String title) {
        return createEvent(title, LocalDate.now());
    }

    public static long createEvent(String title, LocalDate displayDate) {
        QuarkusTransaction.begin();
        Event event = new Event();
        event.title = title;
        event.slug = Slug.of(title).toString();
        event.date = "some date";
        event.displayDate = displayDate;
        event.location = "some location";
        event.description = "some description";
        event.persist();
        QuarkusTransaction.commit();
        createdEvents.add(event.id);
        Log.info("EventFixture created " + event);
        return event.id;
    }

    public static long createEventWithRest(URL eventEndpoint, String title, String slug) {
        given()
                .contentType(ContentType.URLENC)
                .formParam("title", title)
                .formParam("slug", slug)
                .formParam("date", "17.02.2023")
                .formParam("displayDate", "2023-01-01")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(eventEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);

        List<Event> events = Event.listAll();
        var event = events.get(events.size()-1);
        Log.info("EventFixture created " + event);
        createdEvents.add(event.id);
        return event.id;
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        cleanupCreatedEvents();
    }

    public static void cleanupCreatedEvents() {
        Log.info("Cleanup Events created from EventFixutre: " + createdEvents);
        QuarkusTransaction.begin();
        createdEvents.forEach(id -> Event.deleteById(id));
        createdEvents.clear();
        QuarkusTransaction.commit();
    }
}

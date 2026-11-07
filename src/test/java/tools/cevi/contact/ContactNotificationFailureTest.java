package tools.cevi.contact;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * UC-005 Contact the International Working Group, A3 (notification cannot be delivered) and BR-007
 * on that path. The profile switches the mock mailer off and points the real one at a port nothing
 * listens on, so the delivery fails the way an unreachable mail relay does — without reaching any
 * external system.
 */
@QuarkusTest
@TestProfile(ContactNotificationFailureTest.UnreachableMailService.class)
class ContactNotificationFailureTest {
    private static final String PREFIX = "CLEANUP UC-005 A3 ";

    @TestHTTPEndpoint(ContactResource.class)
    @TestHTTPResource
    URL contactEndpoint;

    private final List<ExtLogRecord> records = new CopyOnWriteArrayList<>();
    private final Logger rootLog = Logger.getLogger("");
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
    void captureLog() {
        rootLog.addHandler(capture);
    }

    @AfterEach
    void cleanup() {
        rootLog.removeHandler(capture);
        QuarkusTransaction.requiringNew().run(() -> ContactFormEntry.delete("message like ?1", PREFIX + "%"));
    }

    /**
     * A3: the failure is noted for the operators, the message stays recorded (BR-002), and the
     * visitor sees the confirmation of step 9 without being confronted with the delivery problem.
     */
    @Test
    void an_undeliverable_notification_is_noted_and_the_visitor_is_confirmed() {
        String message = PREFIX + UUID.randomUUID();

        String html = Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertThat(html, containsString("Nachricht ist unterwegs"));
        assertThat(ContactFormEntry.find("message", message).count(), equalTo(1L));
        assertThat(records.stream()
                .filter(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
                .filter(r -> ContactResource.class.getName().equals(r.getLoggerName()))
                .count(), equalTo(1L));
    }

    /** BR-007, A3: noting the failure does not put the message contents into the operating records. */
    @Test
    void the_message_contents_stay_out_of_the_records_when_delivery_fails() {
        String message = PREFIX + "BR-007 " + UUID.randomUUID();

        Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK);

        StringBuilder text = new StringBuilder();
        for (ExtLogRecord logRecord : records) {
            text.append(logRecord.getFormattedMessage()).append('\n');
            for (Throwable t = logRecord.getThrown(); t != null; t = t.getCause()) {
                text.append(t).append('\n');
            }
        }
        assertThat(text.toString().contains(message), is(false));
    }

    /** The real mailer against a closed local port: every send fails at once with a refused connection. */
    public static class UnreachableMailService implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.mailer.mock", "false",
                    "quarkus.mailer.from", "website@localhost",
                    "quarkus.mailer.host", "127.0.0.1",
                    "quarkus.mailer.port", "1",
                    "quarkus.mailer.start-tls", "DISABLED");
        }
    }
}

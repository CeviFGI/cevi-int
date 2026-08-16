package tools.cevi.contact;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.net.URL;
import java.util.List;
import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class ContactResourceTest {
    @TestHTTPEndpoint(ContactResource.class)
    @TestHTTPResource
    URL contactEndpoint;

    @ConfigProperty(name = "application.contactform.to")
    String to;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void init() {
        mailbox.clear();
    }

    @Test
    public void page_working() {
        given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void form_saved() {
        Csrf.given().formParam("message", "my message").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK);

       List<ContactFormEntry> messages = ContactFormEntry.listAll();
        assertThat(messages, is(not(empty())));
        assertThat(messages.get(messages.size()-1).message, equalTo("my message"));

        List<Mail> sent = mailbox.getMailsSentTo(to);
        assertThat(sent, hasSize(1));
        Mail actual = sent.get(0);
        assertThat(actual.getText(), stringContainsInOrder("my message"));
        assertThat(actual.getSubject(), equalTo("[Cevi International Webseite] Kontaktformular ausgefüllt"));

        assertThat(mailbox.getTotalMessagesSent(), equalTo(1));
    }

    @Test
    public void form_fail_spam() {
        Csrf.given().formParam("message", "my message").formParam("spam", "10")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Fehler beim Absenden des Formulars. Bitte geben sie im Feld Spamschutz die Zahl 50 ein."));

        List<Mail> sent = mailbox.getMailsSentTo(to);
        assertThat(sent, hasSize(0));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    /**
     * A filled honeypot means the submission was produced automatically. Nothing is stored or
     * forwarded, but the answer looks exactly like a successful one (BR-032).
     */
    @Test
    public void form_discarded_when_honeypot_filled() {
        long before = ContactFormEntry.count();

        Csrf.given().formParam("message", "spam message").formParam("spam", "50")
                .formParam("website", "http://spam.example")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Vielen Dank"));

        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    /** The endpoint is anonymous, so an unbounded message would let anyone fill the database. */
    @Test
    public void form_rejects_message_over_the_length_limit() {
        long before = ContactFormEntry.count();

        Csrf.given().formParam("message", "x".repeat(ContactFormEntry.MAX_MESSAGE_LENGTH + 1))
                .formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Fehler beim Absenden des Formulars:"));

        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    @Test
    public void form_rejects_empty_message() {
        long before = ContactFormEntry.count();

        Csrf.given().formParam("message", "   ").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Fehler beim Absenden des Formulars:"));

        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    /** Without a CSRF token the submission is refused before it reaches the endpoint (NFR-011). */
    @Test
    public void form_without_csrf_token_is_rejected() {
        long before = ContactFormEntry.count();

        Csrf.givenWithoutToken().formParam("message", "my message").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_BAD_REQUEST);

        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }
}

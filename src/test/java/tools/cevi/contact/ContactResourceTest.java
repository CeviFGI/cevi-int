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
                .body(containsString("Bitte trage die Zahl 50 ein."))
                .body(containsString("<a href=\"#spam\">"));

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
                .body(containsString("Deine Nachricht wurde nicht gesendet."))
                .body(containsString("<a href=\"#message\">"));

        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    @Test
    public void form_rejects_empty_message() {
        long before = ContactFormEntry.count();

        Csrf.given().formParam("message", "   ").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Deine Nachricht wurde nicht gesendet."));

        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    /**
     * A placeholder is not a label — it disappears exactly when the visitor needs it, and a screen
     * reader announces nothing at all for a control without one (FR-037, BR-045, WCAG 1.3.1).
     */
    @Test
    public void every_control_carries_a_visible_label() {
        String body = given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertThat(body, containsString("<label for=\"message\">"));
        assertThat(body, containsString("<label for=\"spam\">"));
        // The reworded spam question, and the numeric keypad it asks for on a phone
        assertThat(body, containsString("Wie viel ergibt 20 + 30?"));
        assertThat(body, containsString("inputmode=\"numeric\""));
    }

    /**
     * The honeypot is what tells an automated submission apart, so it must stay in the markup and
     * out of sight — off-screen, never display:none, or a bot stops filling it (BR-032, FR-032).
     */
    @Test
    public void the_honeypot_stays_in_the_markup_and_out_of_sight() {
        given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("class=\"honeypot\""))
                .body(containsString("name=\"website\""))
                .body(containsString("tabindex=\"-1\""));
    }

    /** A rejected field is marked as such, or the message reaches the eye but not the ear. */
    @Test
    public void a_rejected_field_is_marked_at_the_control() {
        Csrf.given().formParam("message", "my message").formParam("spam", "10")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("aria-invalid=\"true\""))
                .body(containsString("class=\"field__error\""));
    }

    /** A rejected submission gives the typed text back rather than asking for it again (FR-008). */
    @Test
    public void a_rejected_submission_keeps_the_written_message() {
        Csrf.given().formParam("message", "Ich moechte an ein Lager").formParam("spam", "10")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Ich moechte an ein Lager"));
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

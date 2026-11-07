package tools.cevi.contact;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
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

/**
 * UC-005 Contact the International Working Group: the contact form through its endpoint — the main
 * success scenario, A1, A2, A4, A5 and BR-001 … BR-008. A3 is in
 * {@link ContactNotificationFailureTest}; the browser-only parts of BR-005 and BR-008 (the honeypot
 * off-screen, the focus on the error summary) are in {@code ContactFormE2ETest}.
 */
@QuarkusTest
public class ContactResourceTest {
    /** Every message this class writes starts with it, so {@link #cleanup()} can remove them. */
    private static final String PREFIX = "CLEANUP UC-005 ";

    @TestHTTPEndpoint(ContactResource.class)
    @TestHTTPResource
    URL contactEndpoint;

    @ConfigProperty(name = "application.contactform.to")
    String to;

    @Inject
    MockMailbox mailbox;

    @Inject
    EntityManager entityManager;

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
    void init() {
        mailbox.clear();
        rootLog.addHandler(capture);
    }

    @AfterEach
    void cleanup() {
        rootLog.removeHandler(capture);
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DROP TRIGGER IF EXISTS uc005_a2_insert").executeUpdate();
            ContactFormEntry.delete("message like ?1", PREFIX + "%");
        });
    }

    /** Main success scenario steps 1–2: the contact page opens without an account. */
    @Test
    public void page_working() {
        given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK);
    }

    /**
     * Main success scenario step 2: the page presents the kinds of support the working group
     * offers, ahead of the form.
     */
    @Test
    public void the_page_presents_the_kinds_of_support() {
        given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(stringContainsInOrder("Womit wir helfen können", "<ul class=\"help-grid\">",
                        "<li>", "<form"));
    }

    /**
     * Main success scenario steps 4–9, BR-002, BR-003: an accepted message is recorded, forwarded
     * to the configured address and confirmed.
     */
    @Test
    public void form_saved() {
        Csrf.given().formParam("message", "my message").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Nachricht ist unterwegs"));

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

    /**
     * BR-003: the visitor cannot choose the recipient — a recipient smuggled into the submission
     * is ignored and the notification still goes to the one configured address only.
     */
    @Test
    public void the_recipient_cannot_be_chosen_by_the_visitor() {
        Csrf.given().formParam("message", PREFIX + "BR-003").formParam("spam", "50")
                .formParam("to", "attacker@example.org")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK);

        assertThat(mailbox.getMailsSentTo("attacker@example.org"), hasSize(0));
        assertThat(mailbox.getMailsSentTo(to), hasSize(1));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(1));
    }

    /** A1, BR-001: a wrong answer is rejected at the spam field; nothing is recorded or sent. */
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
     * A1, BR-001: an empty answer is rejected like a wrong one — the form comes back with the
     * message, the spam field is marked and explains the expected answer, the summary on top says
     * nothing was sent. Nothing is recorded or sent (failure postcondition).
     */
    @Test
    public void an_empty_spam_answer_is_rejected() {
        String message = PREFIX + "A1 empty answer";
        long before = ContactFormEntry.count();

        String html = Csrf.given().formParam("message", message).formParam("spam", "")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertThat(html, containsString("Deine Nachricht wurde nicht gesendet."));
        assertThat(html, containsString("<a href=\"#spam\">"));
        assertFieldMarked(html, "spam");
        assertThat(errorAtField(html, "spam"), containsString("50"));
        assertThat(html, containsString(">" + message + "</textarea>"));
        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    /** A1, BR-001: without any spam answer in the submission, the message is not accepted either. */
    @Test
    public void a_missing_spam_answer_is_rejected() {
        long before = ContactFormEntry.count();

        String html = Csrf.given().formParam("message", PREFIX + "A1 no answer")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertFieldMarked(html, "spam");
        assertThat(ContactFormEntry.count(), equalTo(before));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }

    /**
     * A5, BR-005: a filled honeypot means the submission was produced automatically. Nothing is
     * stored or forwarded, but the answer looks exactly like a successful one.
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

    /**
     * A5, BR-005: the answer to a discarded submission is the very same page as after an accepted
     * one, so an automated sender cannot tell the two apart.
     */
    @Test
    public void the_discarded_answer_equals_the_accepted_one() {
        String accepted = Csrf.given().formParam("message", PREFIX + "A5 accepted").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();
        String discarded = Csrf.given().formParam("message", PREFIX + "A5 discarded").formParam("spam", "50")
                .formParam("website", "http://spam.example")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertThat(withoutCsrfToken(discarded), equalTo(withoutCsrfToken(accepted)));
    }

    /**
     * A4, BR-006: the endpoint is anonymous, so an unbounded message would let anyone fill the
     * database.
     */
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

    /**
     * A4, BR-006, BR-008: a message over 5000 characters comes back in the form, the message field
     * is marked, and the fault at that field names the limit that was exceeded.
     */
    @Test
    public void a_too_long_message_is_named_at_its_field() {
        String message = PREFIX + "x".repeat(5000);

        String html = Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertFieldMarked(html, "message");
        assertThat(errorAtField(html, "message"), containsString("5000"));
        assertThat(html, containsString(">" + message + "</textarea>"));
    }

    /** BR-006: a message of exactly 5000 characters is still accepted. */
    @Test
    public void a_message_of_exactly_the_limit_is_accepted() {
        String message = PREFIX + "x".repeat(5000 - PREFIX.length());

        Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Nachricht ist unterwegs"));

        assertThat(ContactFormEntry.find("message", message).count(), equalTo(1L));
        assertThat(mailbox.getMailsSentTo(to), hasSize(1));
    }

    /** A4: an empty message is rejected; nothing is recorded or sent. */
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
     * A4, BR-008: for an empty message the fault is named at the message field itself, the field
     * is marked, and the summary on top leads to it.
     */
    @Test
    public void an_empty_message_is_named_at_its_field() {
        String html = Csrf.given().formParam("message", "").formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertFieldMarked(html, "message");
        assertThat(errorAtField(html, "message"), is(not("")));
        assertThat(html, containsString("<a href=\"#message\">"));
    }

    /**
     * A2, BR-002: the message cannot be stored. The incomplete recording is discarded, the working
     * group is still notified, and the visitor sees the confirmation.
     */
    @Test
    public void a_message_that_cannot_be_recorded_still_reaches_the_working_group() {
        String message = PREFIX + "A2 cannot be stored " + UUID.randomUUID();
        refuseToStore(message);

        Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Nachricht ist unterwegs"));

        assertThat(ContactFormEntry.find("message", message).count(), equalTo(0L));
        List<Mail> sent = mailbox.getMailsSentTo(to);
        assertThat(sent, hasSize(1));
        assertThat(sent.get(0).getText(), containsString(message));
    }

    /**
     * BR-007: the operating records note that a message was recorded and under which reference,
     * but never its contents.
     */
    @Test
    public void the_message_contents_stay_out_of_the_operating_records() {
        String message = PREFIX + "BR-007 " + UUID.randomUUID();

        Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK);

        ContactFormEntry entry = ContactFormEntry.find("message", message).firstResult();
        assertThat(logText(), containsString("id=" + entry.getId()));
        assertThat(logText(), not(containsString(message)));
    }

    /** BR-007, A2: also when the message cannot be stored, its contents are not logged. */
    @Test
    public void the_message_contents_stay_out_of_the_records_when_storing_fails() {
        String message = PREFIX + "BR-007 A2 " + UUID.randomUUID();
        refuseToStore(message);

        Csrf.given().formParam("message", message).formParam("spam", "50")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK);

        assertThat(logText(), not(containsString(message)));
    }

    /**
     * BR-004: the form asks only for the message — besides the spam question and the hidden field
     * it carries no input, so no sender name, e-mail address or phone number is requested.
     */
    @Test
    public void no_sender_data_is_requested() {
        String html = given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();
        String form = contactForm(html);

        Set<String> names = new TreeSet<>();
        Matcher control = Pattern.compile("<(?:input|textarea|select)\\b[^>]*\\bname=\"([^\"]+)\"").matcher(form);
        while (control.find()) {
            names.add(control.group(1));
        }

        assertThat(names, equalTo(new TreeSet<>(Set.of(Csrf.FIELD, "message", "spam", "website"))));
    }

    /**
     * Main success scenario step 2, BR-004, BR-008: the page explains why no sender details are
     * asked for, and that a way to be reached belongs into the message.
     */
    @Test
    public void the_form_explains_why_no_sender_details_are_asked() {
        String html = given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertThat(helpOf(html, "message"), stringContainsInOrder("Antwort", "E-Mail-Adresse", "Telefonnummer",
                "sonst nichts"));
    }

    /**
     * BR-008: every input carries an explanation of why it is asked, tied to it so that a screen
     * reader reads it with the field.
     */
    @Test
    public void every_input_carries_an_explanation() {
        String html = given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        for (String field : new String[]{"message", "spam"}) {
            assertThat("control '" + field + "' points at its explanation",
                    Pattern.compile("id=\"" + field + "\"[^>]*aria-describedby=\"" + field + "-help\"")
                            .matcher(html).find(), is(true));
            assertThat("explanation of '" + field + "'", helpOf(html, field), is(not("")));
        }
        assertThat(helpOf(html, "spam"), containsString("automatisch verschickte"));
    }

    /**
     * BR-008, FR-037: a placeholder is not a label — it disappears exactly when the visitor needs
     * it, and a screen reader announces nothing at all for a control without one (WCAG 1.3.1).
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
        assertThat(contactForm(body), not(containsString("placeholder=")));
    }

    /**
     * BR-005, FR-032: the honeypot is what tells an automated submission apart, so it must stay in
     * the markup and out of sight — off-screen, never display:none, or a bot stops filling it.
     */
    @Test
    public void the_honeypot_stays_in_the_markup_and_out_of_sight() {
        given().when().get(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("class=\"honeypot\""))
                .body(containsString("name=\"website\""))
                .body(containsString("tabindex=\"-1\""));
    }

    /** A1, BR-008: a rejected field is marked as such, or the message reaches the eye but not the ear. */
    @Test
    public void a_rejected_field_is_marked_at_the_control() {
        Csrf.given().formParam("message", "my message").formParam("spam", "10")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("aria-invalid=\"true\""))
                .body(containsString("class=\"field__error\""));
    }

    /** A1: a rejected submission gives the typed text back rather than asking for it again (FR-008). */
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

    /** A2: the store refuses exactly this message, the way a full or broken database would. */
    private void refuseToStore(String message) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "CREATE TRIGGER uc005_a2_insert BEFORE INSERT ON contact_form_entries WHEN NEW.message = '"
                        + message + "' BEGIN SELECT RAISE(ABORT, 'UC-005 A2 test'); END").executeUpdate());
    }

    /** The contact form alone — the header carries the site search form on every page. */
    private static String contactForm(String html) {
        int start = html.indexOf("<form class=\"panel form-card\" action=\"/kontakt\"");
        assertThat("the contact form is present", start >= 0, is(true));
        return html.substring(start, html.indexOf("</form>", start));
    }

    private static void assertFieldMarked(String html, String field) {
        assertThat("field '" + field + "' is marked for correction",
                Pattern.compile("id=\"" + field + "\"[^>]*aria-invalid=\"true\"").matcher(html).find(), is(true));
    }

    /** The fault named inside the field block of {@code field}, not in the summary on top. */
    private static String errorAtField(String html, String field) {
        int start = html.indexOf("id=\"" + field + "\"");
        int end = html.indexOf("id=\"" + field + "-help\"", start);
        assertThat("field '" + field + "' is present", start >= 0 && end > start, is(true));
        Matcher error = Pattern.compile("<p class=\"field__error\">([^<]*)</p>")
                .matcher(html.substring(start, end));
        assertThat("field '" + field + "' names its fault", error.find(), is(true));
        return error.group(1).strip();
    }

    private static String helpOf(String html, String field) {
        Matcher help = Pattern.compile("id=\"" + field + "-help\">([^<]*)</span>").matcher(html);
        assertThat("field '" + field + "' has an explanation", help.find(), is(true));
        return help.group(1).replaceAll("\\s+", " ").strip();
    }

    private static String withoutCsrfToken(String html) {
        return html.replaceAll("name=\"" + Csrf.FIELD + "\" value=\"[^\"]*\"", "");
    }

    /**
     * The mock mailer of the dev and test profiles echoes every mail it pretends to send, body
     * included. That echo stands in for the mail service, which receives the message by BR-007 —
     * the prod profile sends for real ({@code quarkus.mailer.mock=false}) and has no such line.
     */
    private static boolean isMockMailboxEcho(ExtLogRecord logRecord) {
        return "quarkus-mailer".equals(logRecord.getLoggerName())
                && logRecord.getFormattedMessage().startsWith("Sending email");
    }

    /** Everything the operating records received during the test, including attached exceptions. */
    private String logText() {
        StringBuilder text = new StringBuilder();
        for (ExtLogRecord logRecord : records) {
            if (isMockMailboxEcho(logRecord)) {
                continue;
            }
            text.append(logRecord.getFormattedMessage()).append('\n');
            for (Throwable t = logRecord.getThrown(); t != null; t = t.getCause()) {
                text.append(t).append('\n');
            }
        }
        return text.toString();
    }
}

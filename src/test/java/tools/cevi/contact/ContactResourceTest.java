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
        given().contentType(ContentType.URLENC).formParam("message", "my message").formParam("spam", "50")
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
        given().contentType(ContentType.URLENC).formParam("message", "my message").formParam("spam", "10")
                .when().post(contactEndpoint).then().statusCode(HttpStatus.SC_OK)
                .body(containsString("Fehler beim Absenden des Formulars. Bitte geben sie im Feld Spamschutz die Zahl 50 ein."));

        List<Mail> sent = mailbox.getMailsSentTo(to);
        assertThat(sent, hasSize(0));
        assertThat(mailbox.getTotalMessagesSent(), equalTo(0));
    }
}

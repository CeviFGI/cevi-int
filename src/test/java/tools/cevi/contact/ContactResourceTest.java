package tools.cevi.contact;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
public class ContactResourceTest {
    @TestHTTPEndpoint(ContactResource.class)
    @TestHTTPResource
    URL contactEndpoint;

    @Test
    public void page_working() {
        given().when().get(contactEndpoint).then().statusCode(200);
    }

    @Test
    public void contact_form_saved() {
        given().contentType(ContentType.URLENC).formParam("message", "my message")
                .when().post(contactEndpoint).then().statusCode(200);

       List<ContactFormEntry> messages = ContactFormEntry.listAll();
        assertThat(messages, is(not(empty())));
        assertThat(messages.get(0).message, equalTo("my message"));
    }
}

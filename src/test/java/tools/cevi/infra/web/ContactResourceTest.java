package tools.cevi.infra.web;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.net.URL;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.cevi.service.ContactService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
public class ContactResourceTest {
    @Inject
    ContactService service;

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

       var messages = service.listContactMessages();
        assertThat(messages, hasSize(1));
        assertThat(messages.get(0).message(), equalTo("my message"));
    }
}

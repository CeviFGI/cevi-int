package tools.cevi.exchange;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;

import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class ExchangeResourceTest {
    @TestHTTPEndpoint(ExchangeResource.class)
    @TestHTTPResource
    URL exchangeEndpoint;
    @Test
    public void list_no_auth() {
        List<Exchange> exchanges = Exchange.listAll();
        assertThat(exchanges, is(not(empty())));
        given()
                .when()
                .get(exchangeEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(exchanges.get(0).description))
                .body(not(containsString("Neue Austauschmöglichkeit eintragen")));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void list_auth() {
        List<Exchange> exchanges = Exchange.listAll();
        assertThat(exchanges, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(exchangeEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(exchanges.get(0).description))
                .body(containsString("Neue Austauschmöglichkeit eintragen"));
    }

    @Test
    public void form_save_no_auth() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org")
                .formParam("organizationLink", "http://test.ch")
                .formParam("description", "desc")
                .when()
                .post(exchangeEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"))
                .cookie("quarkus-redirect-location", containsString(exchangeEndpoint.toString()));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void form_save_auth() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org")
                .formParam("organizationLink", "http://test.ch")
                .formParam("description", "desc")
                .when().post(exchangeEndpoint).then().statusCode(200);

        List<Exchange> exchanges = Exchange.listAll();
        assertThat(exchanges, is(not(empty())));
        assertThat(exchanges.get(exchanges.size()-1).organization, equalTo("test org"));
    }
}

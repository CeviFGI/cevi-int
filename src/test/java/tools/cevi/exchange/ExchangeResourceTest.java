package tools.cevi.exchange;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;
import java.util.UUID;

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

    @TestHTTPEndpoint(ExchangeResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

    @TestHTTPEndpoint(ExchangeResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

    @TestHTTPEndpoint(ExchangeResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

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
                .body(not(containsString("Neue Austauschmöglichkeit eintragen")))
                .body(not(containsString("Bearbeiten")));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void list_with_auth() {
        List<Exchange> exchanges = Exchange.listAll();
        assertThat(exchanges, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(exchangeEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(exchanges.get(0).description))
                .body(containsString("Neue Austauschmöglichkeit eintragen"))
                .body(containsString("Bearbeiten"));
    }

    @Test
    public void add_form_no_auth() {
        given()
                .redirects()
                .follow(false)
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_form_with_auth() {
        given()
                .when()
                .get(addEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Neue Austauschmöglichkeit erfassen"));
    }

    @Test
    public void add_submit_no_auth() {
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
    public void add_submit_with_auth() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org")
                .formParam("organizationLink", "http://test.ch")
                .formParam("description", "desc")
                .when().post(exchangeEndpoint).then().statusCode(200);

        List<Exchange> exchanges = Exchange.listAll();
        assertThat(exchanges, is(not(empty())));
        assertThat(exchanges.get(exchanges.size()-1).organization, equalTo("test org"));
    }

    @Test
    public void edit_form_no_auth() {
        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .redirects()
                .follow(false)
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_form_with_auth() {
        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Austauschmöglichkeit bearbeiten"));
    }

    @Test
    public void edit_submit_no_auth() {
        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;

        given().contentType(ContentType.URLENC)
                .formParam("id", firstId)
                .formParam("organization", "test title")
                .formParam("organizationLink", "12-19-2022")
                .formParam("description", "desc")
                .when()
                .post(exchangeEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_submit_with_auth() {
        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;
        long exchangeCount = Exchange.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", firstId)
                .formParam("organization", "organization_" + uuid)
                .formParam("organizationLink", "12-19-2022")
                .formParam("description", "desc")
                .when()
                .post(exchangeEndpoint)
                .then()
                .statusCode(200);

        assertThat(Exchange.count(), equalTo(exchangeCount));

        QuarkusTransaction.begin();
        assertThat(((Exchange) Exchange.findById(firstId)).organization, equalTo("organization_" + uuid));
        QuarkusTransaction.rollback();
    }

    @Test
    public void delete_no_auth() {
        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .redirects()
                .follow(false)
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_with_auth() {
        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Austausch löschen"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_confirmed_with_auth() {
        long exchangeCount = Exchange.count();

        long firstId = ((Exchange) Exchange.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Austausch"));

        assertThat(Exchange.count(), equalTo(exchangeCount-1));
    }
}

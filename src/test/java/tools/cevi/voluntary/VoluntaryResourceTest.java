package tools.cevi.voluntary;

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
public class VoluntaryResourceTest {
    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource
    URL voluntaryEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("edit")
    URL editEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("add")
    URL addEndpoint;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource("delete")
    URL deleteEndpoint;

    @Test
    public void list_no_auth() {
        List<VoluntaryService> voluntaryServices = VoluntaryService.listAll();
        assertThat(voluntaryServices, is(not(empty())));
        given()
                .when()
                .get(voluntaryEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(voluntaryServices.get(0).description))
                .body(not(containsString("Neues Volontariat eintragen<")))
                .body(not(containsString("Bearbeiten")));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void list_with_auth() {
        List<VoluntaryService> voluntaryServices = VoluntaryService.listAll();
        assertThat(voluntaryServices, is(not(empty())));
        given()
                .cookie("quarkus-credential")
                .when()
                .get(voluntaryEndpoint)
                .then()
                .statusCode(200)
                .body(containsString(voluntaryServices.get(0).description))
                .body(containsString("Neues Volontariat eintragen"))
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
                .body(containsString("Neues Volontariat eintragen"));
    }

    @Test
    public void add_submit_no_auth() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org vol")
                .formParam("organizationLink", "http://test.ch")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"))
                .cookie("quarkus-redirect-location", containsString(voluntaryEndpoint.toString()));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void add_submit_with_auth() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org vol")
                .formParam("organizationLink", "http://test.ch")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when().post(voluntaryEndpoint).then().statusCode(200);

        List<VoluntaryService> voluntaryService = VoluntaryService.listAll();
        assertThat(voluntaryService, is(not(empty())));
        assertThat(voluntaryService.get(voluntaryService.size()-1).organization, equalTo("test org vol"));
    }

    @Test
    public void edit_form_no_auth() {
        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

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
        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(editEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Volontariat bearbeiten"));
    }

    @Test
    public void edit_submit_no_auth() {
        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

        given().contentType(ContentType.URLENC)
                .formParam("id", firstId)
                .formParam("organization", "test title")
                .formParam("organizationLink", "12-19-2022")
                .formParam("location", "hier")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(302)
                .header("location", containsString("/auth/login"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void edit_submit_with_auth() {
        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

        long voluntaryServiceCount = VoluntaryService.count();
        var uuid = UUID.randomUUID();

        given()
                .contentType(ContentType.URLENC)
                .formParam("id", firstId)
                .formParam("organization", "organization_" + uuid)
                .formParam("organizationLink", "12-19-2022")
                .formParam("location", "hier")
                .formParam("description", "desc")
                .when()
                .post(voluntaryEndpoint)
                .then()
                .statusCode(200);

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount));

        QuarkusTransaction.begin();
        assertThat(((VoluntaryService) VoluntaryService.findById(firstId)).organization, equalTo("organization_" + uuid));
        QuarkusTransaction.rollback();
    }

    @Test
    public void delete_no_auth() {
        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

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
        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Volontariat löschen"));
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void delete_confirmed_with_auth() {
        long voluntaryServiceCount = VoluntaryService.count();

        long firstId = ((VoluntaryService) VoluntaryService.listAll().stream().findFirst().orElseThrow()).id;

        given()
                .queryParam("id", firstId)
                .queryParam("confirmed", true)
                .when()
                .get(deleteEndpoint)
                .then()
                .statusCode(200)
                .body(containsString("Anlässe"));

        assertThat(VoluntaryService.count(), equalTo(voluntaryServiceCount-1));
    }
}

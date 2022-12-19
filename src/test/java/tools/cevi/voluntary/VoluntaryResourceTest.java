package tools.cevi.voluntary;

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
public class VoluntaryResourceTest {
    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource
    URL voluntaryEndpoint;

    @Test
    public void page_working() {
        List<VoluntaryService> voluntaryServices = VoluntaryService.listAll();
        assertThat(voluntaryServices, is(not(empty())));
        given().when().get(voluntaryEndpoint).then().statusCode(200).body(containsString(voluntaryServices.get(0).description));
    }

    @Test
    public void form_save_need_auth() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org vol")
                .formParam("organizationLink", "http://test.ch")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when().post(voluntaryEndpoint).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = { "admin"})
    public void form_saved() {
        given().contentType(ContentType.URLENC).formParam("organization", "test org vol")
                .formParam("organizationLink", "http://test.ch")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when().post(voluntaryEndpoint).then().statusCode(200);

        List<VoluntaryService> voluntaryService = VoluntaryService.listAll();
        assertThat(voluntaryService, is(not(empty())));
        assertThat(voluntaryService.get(voluntaryService.size()-1).organization, equalTo("test org vol"));
    }
}

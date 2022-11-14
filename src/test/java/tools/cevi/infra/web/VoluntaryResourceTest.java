package tools.cevi.infra.web;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.cevi.service.VoluntaryService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class VoluntaryResourceTest {
    @Inject
    VoluntaryService service;

    @TestHTTPEndpoint(VoluntaryResource.class)
    @TestHTTPResource
    URL voluntaryEndpoint;

    @Test
    public void page_working() {
        var voluntaryServices = service.listVoluntaryServices();
        assertThat(voluntaryServices, is(not(empty())));
        given().when().get(voluntaryEndpoint).then().statusCode(200).body(containsString(voluntaryServices.get(0).description()));
    }
}

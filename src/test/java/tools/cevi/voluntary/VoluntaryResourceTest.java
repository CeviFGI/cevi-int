package tools.cevi.voluntary;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
}

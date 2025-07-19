package tools.cevi.fixture;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import tools.cevi.voluntary.VoluntaryService;

import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static io.restassured.RestAssured.given;

public class VoluntaryFixture implements QuarkusTestAfterEachCallback {
    private static final List<Integer> createdVoluntryServices = new LinkedList<>();

    public static long createVoluntaryService(String organization) {
        return createVoluntaryService(organization, LocalDate.now());
    }

    public static long createVoluntaryService(String organization, LocalDate displayDate) {
        QuarkusTransaction.begin();
        VoluntaryService voluntaryService = new VoluntaryService();
        voluntaryService.organization = organization;
        voluntaryService.description = "some description";
        voluntaryService.location = "some location";
        voluntaryService.organizationLink = "some link";
        voluntaryService.persist();
        QuarkusTransaction.commit();
        createdVoluntryServices.add(voluntaryService.id);
        Log.info("VoluntaryFixture created " + voluntaryService);
        return voluntaryService.id;
    }

    public static long createVoluntaryServiceWithRest(URL voluntaryServiceEndpoint, String organization) {
        given().contentType(ContentType.URLENC).formParam("organization", organization)
                .formParam("organizationLink", "http://test.ch")
                .formParam("location", "Bern")
                .formParam("description", "desc")
                .when()
                .post(voluntaryServiceEndpoint)
                .then()
                .statusCode(HttpStatus.SC_OK);

        List<VoluntaryService> voluntaryServices = VoluntaryService.listAll();
        var voluntaryService = voluntaryServices.getLast();
        Log.info("VoluntaryFixture created " + voluntaryService);
        createdVoluntryServices.add(voluntaryService.id);
        return voluntaryService.id;
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        cleanupCreatedVoluntaryServices();
    }

    public static void cleanupCreatedVoluntaryServices() {
        Log.info("Cleanup Events created from VoluntaryFixutre: " + createdVoluntryServices);
        QuarkusTransaction.begin();
        createdVoluntryServices.forEach(id -> VoluntaryService.deleteById(id));
        createdVoluntryServices.clear();
        QuarkusTransaction.commit();
    }
}

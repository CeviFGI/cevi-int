package tools.cevi.fixture;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import tools.cevi.infra.Slug;
import tools.cevi.voluntary.VoluntaryService;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class VoluntaryFixture implements QuarkusTestAfterEachCallback {
    private static final List<Integer> createdVoluntryServices = new LinkedList<>();

    public static long createVoluntaryService(String organization) {
        return createVoluntaryService(organization, "some description", "https://example.org/some-link");
    }

    /** For tests that care about the description or about where the offer links to. */
    public static long createVoluntaryService(String organization, String description, String organizationLink) {
        QuarkusTransaction.begin();
        VoluntaryService voluntaryService = new VoluntaryService();
        voluntaryService.organization = organization;
        voluntaryService.slug = uniqueSlug(organization);
        voluntaryService.description = description;
        voluntaryService.location = "some location";
        voluntaryService.organizationLink = organizationLink;
        voluntaryService.persist();
        QuarkusTransaction.commit();
        createdVoluntryServices.add(voluntaryService.id);
        Log.info("VoluntaryFixture created " + voluntaryService);
        return voluntaryService.id;
    }

    /**
     * A Short Name no other offer carries: readable from the Organisation, made unique by a random
     * tail, so that tests running against the shared database never collide (UC-004 BR-006).
     */
    public static String uniqueSlug(String organization) {
        return Slug.of(organization) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Remembers an offer a test created another way, so that it is removed after the test. */
    public static void trackForCleanup(int id) {
        createdVoluntryServices.add(id);
    }

    public static long createVoluntaryServiceWithRest(URL voluntaryServiceEndpoint, String organization) {
        Csrf.given().formParam("organization", organization)
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

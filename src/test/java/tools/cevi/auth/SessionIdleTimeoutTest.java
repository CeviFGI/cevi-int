package tools.cevi.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * UC-006 A6, BR-006: a session left unused stops being accepted, and the next maintenance function
 * leads to the sign-in page.
 * <p>
 * The 30 minutes of BR-006 are checked against the configuration in {@link SignInRulesTest}; here
 * the idle period is shortened to two seconds so the expiry itself can be observed.
 */
@QuarkusTest
@TestProfile(SessionIdleTimeoutTest.ShortIdlePeriod.class)
class SessionIdleTimeoutTest {

    public static class ShortIdlePeriod implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.auth.form.timeout", "PT2S",
                    "quarkus.http.auth.form.new-cookie-interval", "PT1S");
        }
    }

    /** A6: within the idle period the session is accepted, after it the sign-in page comes back. */
    @Test
    void unused_session_leads_back_to_the_sign_in_page() throws InterruptedException {
        String session = given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .extract().cookie("quarkus-credential");

        given().redirects().follow(false).cookie("quarkus-credential", session)
                .when().get("/anlaesse/add")
                .then().statusCode(HttpStatus.SC_OK);

        Thread.sleep(3_000);

        given().redirects().follow(false).cookie("quarkus-credential", session)
                .when().get("/anlaesse/add")
                .then().statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .header("location", containsString("/auth/login"));
    }
}

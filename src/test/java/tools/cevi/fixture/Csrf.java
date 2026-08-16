package tools.cevi.fixture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Builds a request that carries both halves of the CSRF double submit, the way a browser does after
 * it has loaded a form page.
 * <p>
 * The token is not bound to an identity, so any HTML page of the site yields a usable pair. The
 * public contact form is used, which keeps the helper usable from tests that run without a session
 * — those still have to reach the authorisation check rather than being turned away for a missing
 * token.
 */
public final class Csrf {

    /** Name of both the hidden form field and the cookie ({@code quarkus.rest-csrf} defaults). */
    public static final String FIELD = "csrf-token";

    private static final String TOKEN_SOURCE_PAGE = "/kontakt";
    private static final Pattern HIDDEN_FIELD = Pattern.compile(
            "name=\"" + FIELD + "\"\\s+value=\"([^\"]+)\"");

    private Csrf() {
    }

    /** @return a form-urlencoded request specification with a matching CSRF cookie and form field */
    public static RequestSpecification given() {
        Response page = RestAssured.given().when().get(TOKEN_SOURCE_PAGE).andReturn();

        String cookie = page.getCookie(FIELD);
        Matcher matcher = HIDDEN_FIELD.matcher(page.asString());
        if (cookie == null || !matcher.find()) {
            throw new IllegalStateException("No CSRF token found on " + TOKEN_SOURCE_PAGE
                    + " - cookie=" + cookie);
        }

        return RestAssured.given()
                .contentType(ContentType.URLENC)
                .cookie(FIELD, cookie)
                .formParam(FIELD, matcher.group(1));
    }

    /** @return a form-urlencoded request specification deliberately without any CSRF token */
    public static RequestSpecification givenWithoutToken() {
        return RestAssured.given().contentType(ContentType.URLENC);
    }
}

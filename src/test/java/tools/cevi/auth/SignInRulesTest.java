package tools.cevi.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.cevi.fixture.Csrf;
import tools.cevi.infra.DemoData;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UC-006 Sign In as Administrator: the rules behind the sign-in — BR-001, BR-002, BR-005, BR-006,
 * BR-008 — and the error page of A1. The session cookie's attributes (BR-007) are in
 * {@link SessionCookieAttributesTest}, the expiry of an unused session (A6) in
 * {@link SessionIdleTimeoutTest}.
 */
@QuarkusTest
class SignInRulesTest {

    private final List<ExtLogRecord> records = new CopyOnWriteArrayList<>();
    private final Logger signInLog = Logger.getLogger(AuthenticationEventLogger.class.getName());
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
            records.add((ExtLogRecord) logRecord);
        }

        @Override
        public void flush() {
            // nothing buffered
        }

        @Override
        public void close() {
            // nothing to release
        }
    };

    @BeforeEach
    void captureSignInLog() {
        signInLog.addHandler(capture);
    }

    @AfterEach
    void releaseSignInLog() {
        signInLog.removeHandler(capture);
    }

    /** BR-005, main success scenario step 5: a successful attempt is noted with its outcome. */
    @Test
    void successful_sign_in_is_recorded() {
        signIn("admin", "admin");

        assertThat(messagesAt(Level.INFO), hasItem(containsString("Sign-in succeeded for user 'admin'")));
    }

    /**
     * BR-005, A1 step 1: a failed attempt is recorded prominently, with the name that was tried and
     * where the attempt came from.
     */
    @Test
    void failed_sign_in_is_recorded_with_name_and_origin() {
        signIn("intruder", "guess");

        assertThat(messagesAt(Level.WARNING), hasItem(containsString("Sign-in failed for user 'intruder' from 127.0.0.1")));
    }

    /**
     * A1 step 2: the error page says the sign-in failed, and it is the same page for an unknown name
     * and for a wrong password, so it does not reveal which of the two was wrong.
     */
    @Test
    void error_page_does_not_reveal_which_credential_was_wrong() {
        String unknownName = errorPageAfter("nobody", "admin");
        String wrongPassword = errorPageAfter("admin", "wrong");

        assertThat(unknownName, containsString("Anmeldung fehlgeschlagen"));
        assertThat(withoutFormToken(unknownName), equalTo(withoutFormToken(wrongPassword)));
    }

    /** BR-001: being signed in is not enough, a maintenance function needs the administrator role. */
    @Test
    @TestSecurity(user = "visitor", roles = {"user"})
    void signed_in_account_without_administrator_role_is_refused() {
        given().redirects().follow(false).when().get("/anlaesse/add")
                .then().statusCode(HttpStatus.SC_FORBIDDEN);
        given().redirects().follow(false).when().get("/volontariat/add")
                .then().statusCode(HttpStatus.SC_FORBIDDEN);
        Csrf.given().formParam("id", 0).formParam("organization", "CLEANUP BR-001")
                .when().post("/volontariat")
                .then().statusCode(HttpStatus.SC_FORBIDDEN);
    }

    /** BR-002: the stored password is irreversible — not the password itself, but it still verifies. */
    @Test
    void password_is_not_stored_readably() {
        User admin = User.find("username", "admin").firstResult();

        assertThat(admin.password, not(containsString("admin")));
        assertThat(BcryptUtil.matches("admin", admin.password), is(true));
    }

    /** BR-006: a session is accepted for 30 minutes without use, not longer. */
    @Test
    void unused_session_expires_after_30_minutes() {
        Duration timeout = ConfigProvider.getConfig().getValue("quarkus.http.auth.form.timeout", Duration.class);

        assertThat(timeout, equalTo(Duration.ofMinutes(30)));
    }

    /**
     * BR-008: with no user account, the platform refuses to start when the deployment supplies no
     * password, and creates the first administrator account from the one it supplies.
     */
    @Test
    void first_administrator_account_comes_from_the_deployment() {
        AdminAccountBootstrap bootstrap = new AdminAccountBootstrap();
        bootstrap.username = "admin";
        try {
            QuarkusTransaction.requiringNew().run(() -> User.delete("username", "admin"));

            bootstrap.password = Optional.empty();
            assertThrows(IllegalStateException.class, () -> QuarkusTransaction.requiringNew().run(() -> bootstrap.startup(null)));
            bootstrap.password = Optional.of(" ");
            assertThrows(IllegalStateException.class, () -> QuarkusTransaction.requiringNew().run(() -> bootstrap.startup(null)));
            // A lambda, not User::count: a method reference binds to the un-enhanced PanacheEntityBase method
            assertThat(QuarkusTransaction.requiringNew().call(() -> User.count()), is(0L));

            bootstrap.password = Optional.of("admin");
            QuarkusTransaction.requiringNew().run(() -> bootstrap.startup(null));
            User created = QuarkusTransaction.requiringNew().call(() -> User.find("username", "admin").firstResult());
            assertThat(created.role, is("admin"));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                if (User.count("username", "admin") == 0) {
                    User.add("admin", "admin", "admin");
                }
            });
        }
    }

    /**
     * BR-008: demonstration accounts exist only outside production. No configuration file sets
     * the administrator password for the prod profile — neither unprefixed nor with {@code %prod.}
     * — and the demonstration data is excluded from the prod build.
     */
    @Test
    void demonstration_accounts_exist_only_outside_production() throws IOException {
        for (String file : List.of("application.properties", "application-prod.properties")) {
            Properties properties = new Properties();
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) {
                assertThat(file + " is on the classpath", in != null, is(true));
                properties.load(in);
            }
            assertThat(file, properties.stringPropertyNames(), not(hasItem("application.admin.password")));
            assertThat(file, properties.stringPropertyNames(), not(hasItem("%prod.application.admin.password")));
        }

        UnlessBuildProfile unless = DemoData.class.getAnnotation(UnlessBuildProfile.class);
        assertThat(unless != null && List.of(unless.value()).contains("prod"), is(true));
    }

    private static void signIn(String username, String password) {
        given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("j_username", username)
                .formParam("j_password", password)
                .when()
                .post("/auth/j_security_check")
                .then()
                .statusCode(HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private static String errorPageAfter(String username, String password) {
        String location = given().redirects().follow(false).contentType(ContentType.URLENC)
                .formParam("j_username", username).formParam("j_password", password)
                .when().post("/auth/j_security_check")
                .then().statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                .extract().header("location");
        return given().when().get(location).then().statusCode(HttpStatus.SC_OK).extract().asString();
    }

    private List<String> messagesAt(Level level) {
        return records.stream()
                .filter(logRecord -> logRecord.getLevel().equals(level))
                .map(ExtLogRecord::getFormattedMessage)
                .toList();
    }

    /** Each page carries a fresh form token, which is the only part allowed to differ. */
    private static String withoutFormToken(String html) {
        return html.replaceAll("name=\"csrf-token\" value=\"[^\"]*\"", "");
    }
}

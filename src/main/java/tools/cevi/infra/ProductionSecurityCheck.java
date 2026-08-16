package tools.cevi.infra;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Refuses to start a production instance whose session encryption key was not supplied.
 * <p>
 * Quarkus does not fail on a missing key: it generates a random one, logs it and carries on. That
 * is not obviously wrong from the outside — sessions simply stop working after every restart — so
 * a deployment can run for a long time before anyone notices that the key never arrived. Failing
 * loudly at startup turns a silent misconfiguration into a failed deployment.
 */
@ApplicationScoped
@IfBuildProfile("prod")
public class ProductionSecurityCheck {

    static final String SESSION_KEY_PROPERTY = "quarkus.http.auth.session.encryption-key";

    void startup(@Observes StartupEvent event) {
        String key = ConfigProvider.getConfig()
                .getOptionalValue(SESSION_KEY_PROPERTY, String.class)
                .orElse("");

        if (key.isBlank()) {
            throw new IllegalStateException(SESSION_KEY_PROPERTY + " is not configured. Supply the "
                    + "session encryption key (environment variable "
                    + "QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY, see docs/deployment.md) and start again.");
        }

        if (key.toUpperCase(java.util.Locale.ROOT).contains("DO-NOT-USE-IN-PRODUCTION")) {
            throw new IllegalStateException(SESSION_KEY_PROPERTY + " still holds the development "
                    + "placeholder. Supply a real, randomly generated key, see docs/deployment.md.");
        }
    }
}

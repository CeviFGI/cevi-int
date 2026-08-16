package tools.cevi.auth;

import java.util.Optional;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Creates the first administrator account from the credentials the deployment supplies, so that a
 * fresh or restored database never comes up with credentials that are readable in the repository
 * (BR-038).
 * <p>
 * Runs in every profile. In dev and test the password is configured in
 * {@code application.properties}; in production it comes from a secret and a missing one aborts the
 * start rather than leaving the installation unadministrable or, worse, seeded with a known
 * password.
 */
@ApplicationScoped
public class AdminAccountBootstrap {

    @ConfigProperty(name = "application.admin.username")
    String username;

    @ConfigProperty(name = "application.admin.password")
    Optional<String> password;

    @Transactional
    void startup(@Observes StartupEvent event) {
        if (User.count() > 0) {
            return;
        }

        if (password.isEmpty() || password.get().isBlank()) {
            throw new IllegalStateException("No user account exists and application.admin.password "
                    + "is not configured. Supply the initial administrator password (environment "
                    + "variable APPLICATION_ADMIN_PASSWORD, see docs/deployment.md) and start again.");
        }

        User.add(username, password.get(), "admin");
        Log.infof("Created the initial administrator account '%s' because the user table was empty", username);
    }
}

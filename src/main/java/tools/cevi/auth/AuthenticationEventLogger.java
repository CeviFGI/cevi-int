package tools.cevi.auth;

import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * Records the outcome of every sign-in attempt (BR-035).
 * <p>
 * The sign-in endpoint itself is provided by Quarkus, so there is no place in the application code
 * where a failed attempt would otherwise become visible: a guessing run looks exactly like ordinary
 * traffic in the request log. Failures are logged at WARN so they stand out in monitoring.
 */
@ApplicationScoped
public class AuthenticationEventLogger {

    private static final Logger LOG = Logger.getLogger(AuthenticationEventLogger.class);
    private static final String UNKNOWN = "<unknown>";

    void onLogin(@Observes FormAuthenticationEvent event) {
        String username = event.getSecurityIdentity() == null || event.getSecurityIdentity().getPrincipal() == null
                ? UNKNOWN
                : event.getSecurityIdentity().getPrincipal().getName();
        LOG.infof("Sign-in succeeded for user '%s' from %s", sanitize(username),
                remoteAddress(routingContext(event.getEventProperties())));
    }

    void onFailure(@Observes AuthenticationFailureEvent event) {
        RoutingContext context = routingContext(event.getEventProperties());
        LOG.warnf("Sign-in failed for user '%s' from %s: %s", sanitize(attemptedUsername(context)),
                remoteAddress(context), event.getAuthenticationFailure().getClass().getSimpleName());
    }

    private static RoutingContext routingContext(java.util.Map<String, Object> properties) {
        Object context = properties == null ? null : properties.get(RoutingContext.class.getName());
        return context instanceof RoutingContext routingContext ? routingContext : null;
    }

    /**
     * Reads the user name out of the submitted sign-in form. The form authentication mechanism has
     * already parsed the body at this point, so the attributes are available; if they are not, the
     * attempt is still worth logging without the name.
     */
    private static String attemptedUsername(RoutingContext context) {
        if (context == null) {
            return UNKNOWN;
        }
        String username = context.request().formAttributes().get("j_username");
        return username == null || username.isBlank() ? UNKNOWN : username;
    }

    private static String remoteAddress(RoutingContext context) {
        if (context == null || context.request().remoteAddress() == null) {
            return UNKNOWN;
        }
        return context.request().remoteAddress().hostAddress();
    }

    /**
     * The user name is attacker-controlled and ends up in a log line. Stripping line breaks keeps
     * it from forging further log entries; the length cap keeps a single attempt from filling the
     * log.
     */
    private static String sanitize(String value) {
        String singleLine = value.replaceAll("[\\r\\n]", "_");
        return singleLine.length() <= 60 ? singleLine : singleLine.substring(0, 60) + "…";
    }
}

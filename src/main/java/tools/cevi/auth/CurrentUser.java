package tools.cevi.auth;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Exposes the verified state of the current request to the templates as {@code inject:currentUser}.
 * <p>
 * The templates used to test for the mere presence of the session cookie, which any visitor can set
 * for themselves. That never granted access — the endpoints behind the links are annotated with
 * {@code @RolesAllowed} — but it is easily mistaken for an authorisation check, and it made the
 * navigation untestable.
 */
@Named("currentUser")
@RequestScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    /** @return {@code true} if the request carries a verified session holding the admin role */
    public boolean isAdmin() {
        return identity != null && !identity.isAnonymous() && identity.hasRole("admin");
    }

    /** @return {@code true} if the request carries a verified session of any kind */
    public boolean isSignedIn() {
        return identity != null && !identity.isAnonymous();
    }
}

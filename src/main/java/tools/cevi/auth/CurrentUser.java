package tools.cevi.auth;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
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

    /**
     * Routing context attribute the CSRF filter of {@code quarkus-rest-csrf} stores the token under
     * ({@code CsrfRequestResponseReactiveFilter.CSRF_TOKEN_KEY}). It is not the form field name, even
     * though the extension's error message prints the latter.
     */
    private static final String CSRF_TOKEN_ATTRIBUTE = "csrf_token";

    @Inject
    SecurityIdentity identity;

    @Inject
    CurrentVertxRequest request;

    /** @return {@code true} if the request carries a verified session holding the admin role */
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-006 BR-003: maintenance links are shown only when this is true
    public boolean isAdmin() {
        return identity != null && !identity.isAnonymous() && identity.hasRole("admin");
    }

    /** @return {@code true} if the request carries a verified session of any kind */
    public boolean isSignedIn() {
        return identity != null && !identity.isAnonymous();
    }

    /**
     * Whether the sign-out form can be offered on this page. The form needs a CSRF token, and the
     * token is only issued for a request that reached an endpoint. An error page for an address that
     * matches none (404, 405) gets no token, and rendering the form there would fail the page.
     *
     * @return {@code true} if the request carries a session and a CSRF token was issued for it
     */
    // UC-006 BR-009: signing out is offered only as a form carrying this browser's token
    public boolean isSignOutAvailable() {
        RoutingContext context = request == null ? null : request.getCurrent();
        return isSignedIn() && context != null && context.get(CSRF_TOKEN_ATTRIBUTE) != null;
    }
}

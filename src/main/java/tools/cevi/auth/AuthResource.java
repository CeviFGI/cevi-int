package tools.cevi.auth;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("auth")
public class AuthResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance login();
        public static native TemplateInstance error();
        public static native TemplateInstance loggedOut();
    }

    @Inject
    SecurityIdentity identity;

    // UC-006 BR-010: the general administration entry leads to sign-in or, when signed in, home
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response auth() {
        if (identity.isAnonymous()) {
            return Response.seeOther(URI.create("auth/login")).build();
        } else {
            return Response.seeOther(URI.create("/")).build();
        }
    }

    // UC-006 A2: an established session skips the sign-in form
    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    public Response login() {
        if (identity.isAnonymous()) {
            return Response.ok().entity(Templates.login()).build();
        } else {
            return Response.seeOther(URI.create("/")).build();
        }
    }

    /**
     * Signing out changes state, so it is a form submission and not a link: as a GET, any other
     * site could sign an administrator out simply by referencing the address.
     */
    // UC-006 BR-009: a POST whose CSRF token matches this browser's token cookie (quarkus-rest-csrf)
    @POST
    @Path("logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logout() {
        // UC-006 A4: whether a session exists is decided by the established identity, not by the
        // presence of a cookie, which an expired session leaves behind and any visitor can set
        if (!identity.isAnonymous()) {
            return Response.seeOther(URI.create("/auth/loggedOut"))
                    .header("Set-Cookie", "quarkus-credential=deleted; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT").build();
        }
        return Response.seeOther(URI.create("/")).build();
    }

    @GET
    @Path("loggedOut")
    public TemplateInstance loggedOut() {
        return Templates.loggedOut();
    }

    @GET
    @Path("error")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance error() {
        return Templates.error();
    }
}

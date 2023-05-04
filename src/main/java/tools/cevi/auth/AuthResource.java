package tools.cevi.auth;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Cookie;
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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response auth() {
        if (identity.isAnonymous()) {
            return Response.seeOther(URI.create("auth/login")).build();
        } else {
            return Response.seeOther(URI.create("/")).build();
        }
    }

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

    @GET
    @Path("logout")
    public Response logout(@CookieParam("quarkus-credential") Cookie cookie) {
        if (cookie != null) {
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

package tools.cevi.infra;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import javax.inject.Inject;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import java.net.URI;

@Path("/")
public class IndexResource {
    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @Inject
    Flyway flyway;

    @Inject
    SecurityIdentity identity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(String username);
        public static native TemplateInstance version(String version, String flywaySchemaVersion);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index(identity.isAnonymous() ? "" : identity.getPrincipal().getName());
    }

    @Path("/version")
    @GET()
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance version() {
        return Templates.version(version, flyway.info().current().getVersion().toString());
    }

    @Path("/admin")
    @GET()
    @Produces(MediaType.TEXT_HTML)
    public Response admin(@CookieParam("quarkus-credential") Cookie cookie) {
        if (cookie != null) {
            return Response.temporaryRedirect(URI.create("/")).build();
        }else {
            return Response.temporaryRedirect(URI.create("auth/login")).build();
        }
    }
}

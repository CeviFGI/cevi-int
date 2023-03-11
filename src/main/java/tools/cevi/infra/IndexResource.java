package tools.cevi.infra;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
        public static native TemplateInstance fgi();
        public static native TemplateInstance version(String version, String flywaySchemaVersion);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        return Response.seeOther(URI.create("/anlaesse")).build();
    }

    @Path("/fgi")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance fgi() {
        return Templates.fgi();
    }

    @Path("/version")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance version() {
        return Templates.version(version, flyway.info().current().getVersion().toString());
    }

    @Path("/admin")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response admin() {
        if (identity.isAnonymous()) {
            return Response.seeOther(URI.create("auth/login")).build();
        } else {
            return Response.seeOther(URI.create("/")).build();
        }
    }
}

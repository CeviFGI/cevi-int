package tools.cevi.infra;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import java.net.URI;
import java.util.List;

import tools.cevi.event.Event;
import tools.cevi.voluntary.VoluntaryService;

@Path("/")
public class IndexResource {
    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @Inject
    Flyway flyway;

    @Inject
    SecurityIdentity identity;

    /** How much of each list the start page shows before pointing at the whole of it (BR-048). */
    private static final int EVENT_TEASERS = 3;
    private static final int OFFER_TEASERS = 2;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance home(List<Event> events, List<VoluntaryService> services);
        public static native TemplateInstance fgi();
        public static native TemplateInstance datenschutzinformation();
        public static native TemplateInstance version(String version, String flywaySchemaVersion);
    }

    /**
     * The start page answers instead of forwarding (BR-047, FR-023 as amended). The typical visitor
     * follows a link shared in a chat group and has not chosen this site from a menu, so the first
     * screen has to say what the platform is before it offers anything to do. {@code /anlaesse}
     * keeps its own address, so every link shared before this change still leads where it did.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.home(Event.upcomingEvents(EVENT_TEASERS), VoluntaryService.newest(OFFER_TEASERS));
    }

    @Path("/fgi")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance fgi() {
        return Templates.fgi();
    }

    @Path("/datenschutzinformation")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance datenschutzinformation() {
        return Templates.datenschutzinformation();
    }

    /**
     * Written for operators. Naming the exact application and schema version to anyone who asks
     * makes it cheap to look up which published weaknesses apply to the running system (BR-039).
     */
    @Path("/version")
    @GET
    @RolesAllowed("admin")
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

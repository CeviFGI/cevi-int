package tools.cevi.infra.web;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.service.EventService;

import static java.util.Objects.requireNonNull;

@Path("/anlaesse")
public class AnlaessePage {
    @Inject
    private EventService eventService;

    private final Template anlaesse;

    public AnlaessePage(Template anlaesse) {
        this.anlaesse = requireNonNull(anlaesse, "page is required");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return anlaesse.data("events", eventService.listEvents());
    }
}

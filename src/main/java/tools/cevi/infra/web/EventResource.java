package tools.cevi.infra.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.domain.Event;
import tools.cevi.service.EventService;

@Path("anlaesse")
public class EventResource {
    @Inject
    EventService service;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Event> events);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(service.listEvents());
    }
}

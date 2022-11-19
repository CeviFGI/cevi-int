package tools.cevi.event;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("anlaesse")
public class EventResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Event> events);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(Event.listAll());
    }
}
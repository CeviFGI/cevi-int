package tools.cevi.voluntary;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("volontariat")
public class VoluntaryResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<VoluntaryService> services);
    }
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(VoluntaryService.listAll());
    }
}
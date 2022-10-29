package tools.cevi.infra.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.domain.VolontariatItem;
import tools.cevi.service.VoluntaryService;

@Path("volontariat")
public class VoluntaryResource {
    @Inject
    VoluntaryService service;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance voluntaryServiceList(List<VolontariatItem> services);
    }
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.voluntaryServiceList(service.listVoluntaryServices());
    }
}
package tools.cevi.infra.web;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.service.VolontariatService;

import static java.util.Objects.requireNonNull;

@Path("/volontariat")
public class VolontariatPage {
    @Inject
    private VolontariatService volontariatService;

    private final Template volontariat;

    public VolontariatPage(Template volontariat) {
        this.volontariat = requireNonNull(volontariat, "page is required");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return volontariat.data("volontariate", volontariatService.listVolontariate());
    }
}
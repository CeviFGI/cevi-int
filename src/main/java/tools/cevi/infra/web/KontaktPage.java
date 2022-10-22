package tools.cevi.infra.web;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;

@Path("/kontakt")
public class KontaktPage {
    private final Template kontakt;

    public KontaktPage(Template kontakt) {
        this.kontakt = requireNonNull(kontakt, "page is required");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return kontakt.instance();
    }
}

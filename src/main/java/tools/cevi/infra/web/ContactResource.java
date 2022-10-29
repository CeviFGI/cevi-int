package tools.cevi.infra.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("kontakt")
public class ContactResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance contactForm();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.contactForm();
    }
}

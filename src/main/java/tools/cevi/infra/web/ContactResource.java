package tools.cevi.infra.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.service.ContactService;

@Path("kontakt")
public class ContactResource {
    @Inject
    ContactService service;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance form();
        public static native TemplateInstance submitted();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance form() {
        return Templates.form();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("message") String message) {
        service.insert(message);
        return Templates.submitted();
    }
}

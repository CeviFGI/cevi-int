package tools.cevi.contact;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("kontakt")
public class ContactResource {
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
    @Transactional
    public TemplateInstance submit(@FormParam("message") String message) {
        ContactFormEntry entry = new ContactFormEntry();
        entry.message = message;
        entry.persist();
        return Templates.submitted();
    }
}

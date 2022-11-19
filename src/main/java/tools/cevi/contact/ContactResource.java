package tools.cevi.contact;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.narayana.jta.QuarkusTransaction;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("kontakt")
public class ContactResource {
    @Inject
    Mailer mailer;

    @ConfigProperty(name = "application.contactform.to")
    String to;

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
        try {
            QuarkusTransaction.begin();
            ContactFormEntry entry = new ContactFormEntry();
            entry.message = message;
            entry.persist();
            QuarkusTransaction.commit();
        } catch (Exception e) {
            Log.error("Unable to save message [" + message + "] to database. Try sending mail", e);
            QuarkusTransaction.rollback();
        }

        try {
            mailer.send(
                    Mail.withText(to, "[Cevi International Webseite] Kontaktformular ausgefüllt", "Message: " + message)
            );
        } catch (Exception e) {
            Log.error("Unable to send mail with message [" + message + "] to [" + to + "]", e);
        }

        return Templates.submitted();
    }
}

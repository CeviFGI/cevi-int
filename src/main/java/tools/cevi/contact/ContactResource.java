package tools.cevi.contact;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("kontakt")
public class ContactResource {
    @Inject
    Mailer mailer;

    @ConfigProperty(name = "application.contactform.to")
    String to;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance form(String message, boolean invalidAntispamValue);
        public static native TemplateInstance submitted();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance form() {
        return Templates.form("", false);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("message") String message, @FormParam("spam") String spam) {
        try {
            if (spam == null || !spam.equals("50")) {
                return Templates.form(message, true);
            }

            QuarkusTransaction.begin();
            ContactFormEntry entry = new ContactFormEntry();
            entry.message = message;
            entry.persist();
            QuarkusTransaction.commit();
            Log.info("Submitted " + message);
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

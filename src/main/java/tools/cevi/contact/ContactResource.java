package tools.cevi.contact;

import java.util.List;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import tools.cevi.infra.ValidationMessage;

@Path("kontakt")
public class ContactResource {
    @Inject
    Mailer mailer;

    @Inject
    Validator validator;

    @ConfigProperty(name = "application.contactform.to")
    String to;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance form(String message, boolean invalidAntispamValue, List<ValidationMessage> validationMessages);
        public static native TemplateInstance submitted();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance form() {
        return Templates.form("", false, List.of());
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("message") String message, @FormParam("spam") String spam,
                                   @FormParam("website") String honeypot) {
        // The honeypot field is hidden from anyone using a browser normally, so a value in it means
        // the submission was produced automatically (BR-032). Answering exactly as for an accepted
        // message keeps the sender from tuning around the check.
        if (honeypot != null && !honeypot.isBlank()) {
            Log.info("Discarded a contact form submission that filled the honeypot field");
            return Templates.submitted();
        }

        if (spam == null || !spam.equals("50")) {
            return Templates.form(message, true, List.of());
        }

        ContactFormEntry entry = new ContactFormEntry();
        entry.message = message;

        List<ValidationMessage> violations = validator.validate(entry).stream()
                .map(ValidationMessage::of).toList();
        if (!violations.isEmpty()) {
            return Templates.form(message, false, violations);
        }

        try {
            QuarkusTransaction.begin();
            entry.persist();
            QuarkusTransaction.commit();
            // The message text is personal data and would additionally allow forged log lines to be
            // injected, so only the reference is recorded (BR-034).
            Log.info("Contact form entry persisted, id=" + entry.getId());
        } catch (Exception e) {
            Log.error("Unable to save a contact form entry to database. Trying to send the mail anyway", e);
            QuarkusTransaction.rollback();
        }

        try {
            mailer.send(
                    Mail.withText(to, "[Cevi International Webseite] Kontaktformular ausgefüllt", "Message: " + message)
            );
        } catch (Exception e) {
            Log.error("Unable to send the contact form mail to [" + to + "]", e);
        }

        return Templates.submitted();
    }
}

package tools.cevi.event;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.annotation.security.RolesAllowed;
import tools.cevi.infra.ValidationMessage;

@Path("anlaesse")
public class EventResource {
    @Inject
    Validator validator;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Event> events);
        public static native TemplateInstance add(String title, String date, String location, String description, List<ValidationMessage> validationMessages);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(Event.listAll());
    }

    @GET
    @Path("add")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance add() {
        return Templates.add("", "", "", "", List.of());
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("title") String title, @FormParam("date") String date,
                                   @FormParam("location") String location, @FormParam("description") String description) {

        Event event = new Event();
        event.title = title;
        event.date = date;
        event.location = location;
        event.description = description;

        Set<ConstraintViolation<Event>> violations = validator.validate(event);
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                event.persist();
                QuarkusTransaction.commit();
                return Templates.list(Event.listAll());
            } catch (Exception e) {
                Log.error("Unable to save event [" + event  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        return Templates.add(title, date, location, description, violations.stream().map(ValidationMessage::of).toList());
    }
}

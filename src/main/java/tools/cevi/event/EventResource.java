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
        public static native TemplateInstance form(long id, String title, String date, String location, String description, List<ValidationMessage> validationMessages);
        public static native TemplateInstance delete(long id, Event event);
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
        return Templates.form(0, "", "", "", "", List.of());
    }

    @GET
    @Path("edit")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@QueryParam("id") long id) {
        Event event = Event.findById(id);
        if (event == null) {
            throw new NotFoundException("Event with id " + id + " not found");
        }
        return Templates.form(id, event.title, event.date, event.location, event.description, List.of());
    }

    @GET
    @Path("delete")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance delete(@QueryParam("id") long id, @QueryParam("confirmed") Boolean confirmed) {
        Event event = Event.findById(id);
        if (event == null) {
            throw new NotFoundException("Event with id " + id + " not found");
        }
        if (confirmed != null && confirmed) {
            return handleDelete(id);
        } else {
            return Templates.delete(id, event);
        }
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("id") long id, @FormParam("title") String title, @FormParam("date") String date,
                                   @FormParam("location") String location, @FormParam("description") String description) {

        if (id == 0) {
            return handleAdd(title, date, location, description);
        } else {
            return handleEdit(id, title, date, location, description);
        }
    }

    private TemplateInstance handleAdd(String title, String date, String location, String description) {
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
                Log.info("Created: " + event);
                return Templates.list(Event.listAll());
            } catch (Exception e) {
                Log.error("Unable to save [" + event  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        return Templates.form(0, title, date, location, description, violations.stream().map(ValidationMessage::of).toList());
    }

    private TemplateInstance handleEdit(long id, String title, String date, String location, String description) {
        Event event = null;
        Set<ConstraintViolation<Event>> violations = Set.of();
        try {
            QuarkusTransaction.begin();

            event = Event.findById(id);
            event.title = title;
            event.date = date;
            event.location = location;
            event.description = description;

            violations = validator.validate(event);
            if (violations.isEmpty()) {
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + event);
                return Templates.list(Event.listAll());
            } else {
                QuarkusTransaction.rollback();
            }
        } catch (Exception e) {
            Log.error("Unable to save event [" + event  + "] to database.", e);
            QuarkusTransaction.rollback();
        }
        return Templates.form(id, title, date, location, description, violations.stream().map(ValidationMessage::of).toList());
    }

    private TemplateInstance handleDelete(long id) {

        try {
            QuarkusTransaction.begin();
            Event event = Event.findById(id);
            event.delete();
            QuarkusTransaction.commit();
        } catch (Exception e) {
            Log.error("Unable to delete event [" + id  + "] from database.", e);
            QuarkusTransaction.rollback();
        }
        return Templates.list(Event.listAll());
    }
}

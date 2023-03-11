package tools.cevi.event;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;

import tools.cevi.infra.Slug;
import tools.cevi.infra.ValidationMessage;

@Path("anlaesse")
public class EventResource {
    @Inject
    Validator validator;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Event> events);
        public static native TemplateInstance form(long id, String title, String slug, String date, String displayDate, String location, String description, Set<ValidationMessage> validationMessages);
        public static native TemplateInstance delete(Event event);
        public static native TemplateInstance detail(Event event);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(Event.upcomingEvents());
    }

    @GET
    @Path("add")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance add() {
        return Templates.form(0, "", "", "", LocalDate.now().toString(), "", "", Set.of());
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
        return Templates.form(id, event.title, event.slug, event.date, event.displayDate.toString(), event.location, event.description, Set.of());
    }

    @GET
    @Path("detail")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@QueryParam("slug") String slug) {
        Event event = Event.findBySlug(slug);
        if (event == null) {
            throw new NotFoundException("Event with slug " + slug + " not found");
        }
        return Templates.detail(event);
    }

    @GET
    @Path("delete")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public Response delete(@QueryParam("id") long id, @QueryParam("confirmed") Boolean confirmed) {
        Event event = Event.findById(id);
        if (event == null) {
            throw new NotFoundException("Event with id " + id + " not found");
        }
        if (confirmed != null && confirmed) {
            return handleDelete(id);
        } else {
            return Response.status(Response.Status.OK).entity(Templates.delete(event).render()).build();
        }
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public Response submit(@FormParam("id") long id, @FormParam("title") String title,
                                   @FormParam("slug") String slug, @FormParam("date") String date,
                                   @FormParam("displayDate") String displayDate,
                                   @FormParam("location") String location, @FormParam("description") String description) {

        if (id == 0) {
            return handleAdd(title, slug, date, displayDate, location, description);
        } else {
            return handleEdit(id, title, slug, date, displayDate, location, description);
        }
    }

    private Response handleAdd(String title, String slug, String date, String displayDate, String location, String description) {
        if (slug == null || slug.isBlank()) {
            slug = generateUniqueSlug(0, title);
        }

        Event event = new Event();
        event.title = title;
        event.slug = slug;
        event.date = date;
        if (tryParseDate(displayDate)) {
            event.displayDate = LocalDate.parse(displayDate);
        }
        event.location = location;
        event.description = description;

        Set<ValidationMessage> violations = new HashSet<>();

        var eventBySlug = Event.findBySlug(slug);
        if (eventBySlug != null) {
            violations.add(ValidationMessage.of("slug", "Es existiert bereits ein anderer Eintrag mit demselben Slug"));
        }

        violations.addAll(validator.validate(event).stream().map(ValidationMessage::of).collect(Collectors.toSet()));
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Created: " + event);
                return Response.seeOther(URI.create("/anlaesse")).build();
            } catch (Exception e) {
                Log.error("Unable to save [" + event  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        Log.info("Violations encountered while adding: " + violations);
        return Response.status(Response.Status.BAD_REQUEST).entity(Templates.form(0, title, slug, date, displayDate, location, description, violations).render()).build();
    }

    private String generateUniqueSlug(long id, String title) {
        String slug = Slug.of(title).toString();
        slug = slug.substring(0, Math.min(slug.length(), 252));
        int counter = 0;
        while (!Event.isSlugUnique(slug) && Event.findBySlug(slug).id != id) {
            slug = Slug.of(title + counter).toString();
        }
        return slug;
    }

    private boolean tryParseDate(String date) {
        try {
            LocalDate.parse(date);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Response handleEdit(long id, String title, String slug, String date, String displayDate, String location, String description) {
        if (slug == null || slug.isBlank()) {
            slug = generateUniqueSlug(id, title);
            Log.info("Generated slug: " + slug);
        }

        Event event = null;
        Set<ValidationMessage> violations = new HashSet<>();
        try {
            QuarkusTransaction.begin();

            var eventBySlug = Event.findBySlug(slug);
            if (eventBySlug != null && eventBySlug.id != id) {
                violations.add(ValidationMessage.of("slug", "Es existiert bereits ein anderer Eintrag mit demselben Slug"));
            }

            event = Event.findById(id);
            event.title = title;
            event.slug = slug;
            event.date = date;
            if (tryParseDate(displayDate)) {
                event.displayDate = LocalDate.parse(displayDate);
            }
            event.location = location;
            event.description = description;

            violations.addAll(validator.validate(event).stream().map(ValidationMessage::of).collect(Collectors.toSet()));
            if (violations.isEmpty()) {
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + event);
                return Response.seeOther(URI.create("/anlaesse")).build();
            } else {
                QuarkusTransaction.rollback();
            }
        } catch (Exception e) {
            Log.error("Unable to save event [" + event  + "] to database.", e);
            QuarkusTransaction.rollback();
        }
        Log.info("Violations encountered while updating: " + violations);
        return Response.status(Response.Status.BAD_REQUEST).entity(Templates.form(id, title, slug, date, displayDate, location, description, violations).render()).build();
    }

    private Response handleDelete(long id) {

        Event event = null;
        try {
            QuarkusTransaction.begin();
            event = Event.findById(id);
            event.delete();
            QuarkusTransaction.commit();
            return Response.seeOther(URI.create("/anlaesse")).build();
        } catch (Exception e) {
            Log.error("Unable to delete event [" + id  + "] from database.", e);
            QuarkusTransaction.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Templates.delete(event)).build();
        }
    }
}

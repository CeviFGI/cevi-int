package tools.cevi.event;

import tools.cevi.infra.Transactions;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import jakarta.annotation.security.RolesAllowed;
import tools.cevi.infra.HtmlSanitizer;
import tools.cevi.infra.Slug;
import tools.cevi.infra.ValidationMessage;
import tools.cevi.infra.ValidationMessages;

@Path("anlaesse")
public class EventResource {
    private static final int MAX_SLUG_LENGTH = 255;
    private static final int MAX_DERIVED_SLUG_LENGTH = 252;

    @Inject
    Validator validator;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Event> events);
        public static native TemplateInstance form(long id, String title, String slug, String date, String displayDate, String location, String description, ValidationMessages validationMessages);
        public static native TemplateInstance delete(Event event, boolean removalFailed);
        public static native TemplateInstance detail(Event event, String publicAddress, String publicPath);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(Event.upcomingEvents());
    }

    // UC-002 BR-001: only an administrator may add, change or remove events
    // UC-006 BR-001: the administrator role decides, not merely a session
    @GET
    @Path("add")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance add() {
        return Templates.form(0, "", "", "", LocalDate.now().toString(), "", "", ValidationMessages.none());
    }

    // UC-002 BR-001: only an administrator may add, change or remove events
    // UC-006 BR-001: the administrator role decides, not merely a session
    @GET
    @Path("edit")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@QueryParam("id") long id) {
        Event event = Event.findById(id);
        if (event == null) {
            throw new NotFoundException("Event with id " + id + " not found");
        }
        return Templates.form(id, event.title, event.slug, event.date, event.displayDate.toString(), event.location, event.description, ValidationMessages.none());
    }

    @GET
    @Path("detail")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@QueryParam("slug") String slug, @Context UriInfo uriInfo) {
        Event event = Event.findBySlug(slug);
        if (event == null) {
            throw new NotFoundException("Event with slug " + slug + " not found");
        }
        URI publicAddress = publicAddress(uriInfo, event);
        return Templates.detail(event, publicAddress.toString(),
                publicAddress.getRawPath() + "?" + publicAddress.getRawQuery());
    }

    /**
     * The address the share control passes on and the page shows without script (UC-009 A4).
     * The script replaces the origin with the one the browser actually loaded the page from, so
     * the host taken from the request here only reaches a visitor whose browser runs no script.
     */
    // UC-009 BR-003: origin of the request, path from the Short Name, no other parameter.
    static URI publicAddress(UriInfo uriInfo, Event event) {
        return uriInfo.getBaseUriBuilder()
                .replacePath("/anlaesse/detail")
                .replaceQuery(null)
                .fragment(null)
                .queryParam("slug", event.slug)
                .build();
    }

    /**
     * Shows the confirmation page. Deleting itself happens in {@link #confirmDelete(long)}: a
     * GET must not change data, or a prefetching browser, a link preview or a plain link from
     * another site is enough to remove an event.
     */
    // UC-002 BR-001: only an administrator may add, change or remove events
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-002 BR-007: a removal is only asked for here; it happens after the confirmation is submitted
    // UC-002 BR-009: a GET never changes data, so following a link cannot remove an event
    @GET
    @Path("delete")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public Response delete(@QueryParam("id") long id) {
        Event event = Event.findById(id);
        if (event == null) {
            throw new NotFoundException("Event with id " + id + " not found");
        }
        return Response.status(Response.Status.OK).entity(Templates.delete(event, false).render()).build();
    }

    // UC-002 BR-001: only an administrator may add, change or remove events
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-002 BR-007: the removal happens only once the confirmation form is submitted
    // UC-002 BR-009: a POST whose CSRF token matches this browser's token cookie (quarkus-rest-csrf)
    @POST
    @Path("delete")
    @RolesAllowed("admin")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response confirmDelete(@FormParam("id") long id) {
        Event event = Event.findById(id);
        if (event == null) {
            throw new NotFoundException("Event with id " + id + " not found");
        }
        return handleDelete(event);
    }

    // UC-002 BR-001: only an administrator may add, change or remove events
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-002 BR-009: a POST whose CSRF token matches this browser's token cookie (quarkus-rest-csrf)
    @POST
    @RolesAllowed("admin")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response submit(@FormParam("id") long id, @FormParam("title") String title,
                                   @FormParam("slug") String slug, @FormParam("date") String date,
                                   @FormParam("displayDate") String displayDate,
                                   @FormParam("location") String location, @FormParam("description") String description) {

        // UC-002 BR-010: reduced here rather than in the templates: the description is rendered
        // unescaped, so what is stored has to be safe regardless of how it got in
        String safeDescription = HtmlSanitizer.sanitize(description);

        if (id == 0) {
            return handleAdd(title, slug, date, displayDate, location, safeDescription);
        } else {
            return handleEdit(id, title, slug, date, displayDate, location, safeDescription);
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

        Set<ValidationMessage> violations = new LinkedHashSet<>();

        // UC-002 BR-004: no two events share a short name
        var eventBySlug = Event.findBySlug(slug);
        if (eventBySlug != null) {
            violations.add(ValidationMessage.of("slug", "Es existiert bereits ein anderer Eintrag mit demselben Slug"));
        }

        violations.addAll(validator.validate(event).stream().map(ValidationMessage::of).collect(Collectors.toCollection(LinkedHashSet::new)));
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Created: " + event);
                return Response.seeOther(URI.create("/anlaesse")).build();
            } catch (Exception e) {
                Log.error("Unable to save [" + event  + "] to database.", e);
                Transactions.rollbackIfActive();
            }
        }
        Log.info("Violations encountered while adding: " + violations);
        return Response.status(Response.Status.BAD_REQUEST).entity(Templates.form(0, title, slug, date, displayDate, location, description, ValidationMessages.of(violations)).render()).build();
    }

    // UC-002 BR-005: a short name derived from the title gets a counter until it is free
    // UC-002 BR-006: the derived name is cut to 252 characters, so name and counter stay within 255
    static String generateUniqueSlug(long id, String title) {
        String base = Slug.of(title).toString();
        base = base.substring(0, Math.min(base.length(), MAX_DERIVED_SLUG_LENGTH));
        String slug = base;
        int counter = 0;
        while (isTakenByAnother(slug, id)) {
            String suffix = Integer.toString(counter++);
            slug = base.substring(0, Math.min(base.length(), MAX_SLUG_LENGTH - suffix.length())) + suffix;
        }
        return slug;
    }

    private static boolean isTakenByAnother(String slug, long id) {
        Event owner = Event.findBySlug(slug);
        return owner != null && owner.id != id;
    }

    // UC-002 BR-008: a display date that cannot be read is ignored rather than stored
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
        Set<ValidationMessage> violations = new LinkedHashSet<>();
        try {
            QuarkusTransaction.begin();

            // UC-002 BR-004: no two events share a short name
            var eventBySlug = Event.findBySlug(slug);
            if (eventBySlug != null && eventBySlug.id != id) {
                violations.add(ValidationMessage.of("slug", "Es existiert bereits ein anderer Eintrag mit demselben Slug"));
            }

            event = Event.findById(id);
            if (event == null) {
                // UC-002 A5: the event was removed after the form was opened
                Transactions.rollbackIfActive();
                throw new NotFoundException("Event with id " + id + " not found");
            }
            event.title = title;
            event.slug = slug;
            event.date = date;
            if (tryParseDate(displayDate)) {
                event.displayDate = LocalDate.parse(displayDate);
            }
            event.location = location;
            event.description = description;

            violations.addAll(validator.validate(event).stream().map(ValidationMessage::of).collect(Collectors.toCollection(LinkedHashSet::new)));
            if (violations.isEmpty()) {
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + event);
                return Response.seeOther(URI.create("/anlaesse")).build();
            } else {
                Transactions.rollbackIfActive();
            }
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            Log.error("Unable to save event [" + event  + "] to database.", e);
            Transactions.rollbackIfActive();
        }
        Log.info("Violations encountered while updating: " + violations);
        return Response.status(Response.Status.BAD_REQUEST).entity(Templates.form(id, title, slug, date, displayDate, location, description, ValidationMessages.of(violations)).render()).build();
    }

    /**
     * Removes the event. If the store refuses, the removal is rolled back and the confirmation page
     * comes back with a notice instead of an error page (UC-002 A8).
     */
    private Response handleDelete(Event event) {
        try {
            QuarkusTransaction.begin();
            Event.deleteById(event.id);
            QuarkusTransaction.commit();
            return Response.seeOther(URI.create("/anlaesse")).build();
        } catch (Exception e) {
            Log.error("Unable to delete event [" + event.id + "] from database.", e);
            Transactions.rollbackIfActive();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Templates.delete(event, true).render()).build();
        }
    }
}

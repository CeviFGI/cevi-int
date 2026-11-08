package tools.cevi.event;

import tools.cevi.infra.Transactions;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.net.URI;
import java.time.Instant;
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
import tools.cevi.infra.PublicAddress;
import tools.cevi.infra.Slug;
import tools.cevi.infra.ValidationMessage;
import tools.cevi.infra.ValidationMessages;

@Path("anlaesse")
public class EventResource {
    private static final URI EVENT_LIST = URI.create("/anlaesse");
    private static final String DETAIL_PATH = "/anlaesse/detail";
    private static final String CALENDAR_PATH = "/anlaesse/kalender.ics";
    private static final String SLUG_TAKEN = "Es existiert bereits ein anderer Eintrag mit demselben Slug";

    @Inject
    Validator validator;

    @CheckedTemplate
    public static class Templates {
        private Templates() {
        }

        public static native TemplateInstance list(List<Event> events);
        public static native TemplateInstance form(EventForm form, ValidationMessages validationMessages);
        public static native TemplateInstance delete(Event event, boolean removalFailed);
        public static native TemplateInstance detail(Event event, PublicAddress share, String calendarEntry);
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
        return Templates.form(EventForm.empty(), ValidationMessages.none());
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
        return Templates.form(EventForm.of(event), ValidationMessages.none());
    }

    @GET
    @Path("detail")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@QueryParam("slug") String slug, @Context UriInfo uriInfo) {
        Event event = Event.findBySlug(slug);
        if (event == null) {
            throw new NotFoundException("Event with slug " + slug + " not found");
        }
        // UC-009 BR-003: the Public Address the share control passes on
        PublicAddress share = PublicAddress.of(uriInfo, DETAIL_PATH, event.slug);
        // UC-012 BR-001, A1: the offer of the Calendar Entry only while the event has a period
        String calendarEntry = event.hasPeriod() ? PublicAddress.of(uriInfo, CALENDAR_PATH, event.slug).path() : null;
        return Templates.detail(event, share, calendarEntry);
    }

    /**
     * The Calendar Entry of an event (UC-012). A plain {@code GET} that changes nothing and records
     * nothing beyond the request log every page view gets (UC-012 BR-006).
     */
    // UC-012 BR-006: no record of its own, no counter; NFR-031 logging as for any page view
    @GET
    @Path("kalender.ics")
    @Produces(CalendarEntry.MEDIA_TYPE)
    public Response calendarEntry(@QueryParam("slug") String slug, @Context UriInfo uriInfo) {
        Event event = Event.findBySlug(slug);
        // UC-012 A2: removed, renamed, or without its period — the Not-Found Page, no entry
        if (event == null || !event.hasPeriod()) {
            throw new NotFoundException("No calendar entry for event with slug " + slug);
        }
        // UC-012 BR-003: the Public Address as for sharing without script
        String address = PublicAddress.of(uriInfo, DETAIL_PATH, event.slug).address();
        // UC-012 BR-005: a copy of the event as it is now
        String file = CalendarEntry.of(event, address, Instant.now());
        // NFR-048: text/calendar, a file name ending in .ics; UC-012 A3: the device may keep it as a file
        return Response.ok(file, CalendarEntry.MEDIA_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + CalendarEntry.fileName(event) + "\"")
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
    public Response submit(@BeanParam EventForm form) {
        // UC-002 BR-010: reduced here rather than in the templates: the description is rendered
        // unescaped, so what is stored has to be safe regardless of how it got in
        form.description = HtmlSanitizer.sanitize(form.description);

        if (form.id == 0) {
            return handleAdd(form);
        } else {
            return handleEdit(form);
        }
    }

    private Response handleAdd(EventForm form) {
        if (form.slug == null || form.slug.isBlank()) {
            form.slug = generateUniqueSlug(0, form.title);
        }

        Event event = new Event();
        Set<ValidationMessage> violations = new LinkedHashSet<>();
        apply(form, event, violations);

        // UC-002 BR-004: no two events share a short name
        var eventBySlug = Event.findBySlug(form.slug);
        if (eventBySlug != null) {
            violations.add(ValidationMessage.of("slug", SLUG_TAKEN));
        }

        violations.addAll(validate(event));
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Created: " + event);
                return Response.seeOther(EVENT_LIST).build();
            } catch (Exception e) {
                Log.error("Unable to save [" + event  + "] to database.", e);
                Transactions.rollbackIfActive();
            }
        }
        Log.info("Violations encountered while adding: " + violations);
        return rejected(form, violations);
    }

    /**
     * Takes the submitted values over into the event. The display date is only taken when it can be
     * read (BR-008); the period only when it is valid as a whole (BR-012) — otherwise its faults go
     * into {@code violations} and the entry is rejected.
     */
    private static void apply(EventForm form, Event event, Set<ValidationMessage> violations) {
        event.title = form.title;
        event.slug = form.slug;
        event.date = form.date;
        if (tryParseDate(form.displayDate)) {
            event.displayDate = LocalDate.parse(form.displayDate);
        }
        // UC-002 BR-012: both or neither, readable, the Last Day not before the First Day
        EventPeriod period = EventPeriod.read(form.firstDay, form.lastDay);
        violations.addAll(period.violations());
        event.firstDay = period.firstDay();
        event.lastDay = period.lastDay();
        event.location = form.location;
        event.description = form.description;
    }

    private Set<ValidationMessage> validate(Event event) {
        return validator.validate(event).stream().map(ValidationMessage::of).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Response rejected(EventForm form, Set<ValidationMessage> violations) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Templates.form(form, ValidationMessages.of(violations)).render()).build();
    }

    // UC-002 BR-005: a short name derived from the title gets a counter until it is free
    // UC-002 BR-006: the derived name is cut to 252 characters, so name and counter stay within 255
    static String generateUniqueSlug(long id, String title) {
        return Slug.unique(title, slug -> isTakenByAnother(slug, id));
    }

    private static boolean isTakenByAnother(String slug, long id) {
        Event owner = Event.findBySlug(slug);
        return owner != null && owner.id != id;
    }

    // UC-002 BR-008: a display date that cannot be read is ignored rather than stored
    private static boolean tryParseDate(String date) {
        try {
            LocalDate.parse(date);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Response handleEdit(EventForm form) {
        long id = form.id;
        // UC-002 BR-011: the submitted short name is kept even when the title changed; only an
        // emptied one is derived anew, which retires the previous Public Address.
        if (form.slug == null || form.slug.isBlank()) {
            form.slug = generateUniqueSlug(id, form.title);
            Log.info("Generated slug: " + form.slug);
        }

        Event event = null;
        Set<ValidationMessage> violations = new LinkedHashSet<>();
        try {
            QuarkusTransaction.begin();

            // UC-002 BR-004: no two events share a short name
            var eventBySlug = Event.findBySlug(form.slug);
            if (eventBySlug != null && eventBySlug.id != id) {
                violations.add(ValidationMessage.of("slug", SLUG_TAKEN));
            }

            event = Event.findById(id);
            if (event == null) {
                // UC-002 A5: the event was removed after the form was opened
                Transactions.rollbackIfActive();
                throw new NotFoundException("Event with id " + id + " not found");
            }
            apply(form, event, violations);

            violations.addAll(validate(event));
            if (violations.isEmpty()) {
                event.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + event);
                return Response.seeOther(EVENT_LIST).build();
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
        return rejected(form, violations);
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
            return Response.seeOther(EVENT_LIST).build();
        } catch (Exception e) {
            Log.error("Unable to delete event [" + event.id + "] from database.", e);
            Transactions.rollbackIfActive();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Templates.delete(event, true).render()).build();
        }
    }
}

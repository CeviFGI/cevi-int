package tools.cevi.voluntary;

import tools.cevi.infra.Transactions;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import jakarta.annotation.security.RolesAllowed;
import tools.cevi.infra.HtmlSanitizer;
import tools.cevi.infra.PublicAddress;
import tools.cevi.infra.Slug;
import tools.cevi.infra.ValidationMessage;
import tools.cevi.infra.ValidationMessages;

import java.net.URI;
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

@Path("volontariat")
public class VoluntaryResource {
    private static final String SLUG_TAKEN = "Es existiert bereits ein anderes Volontariat mit demselben Slug";
    private static final URI OFFER_LIST = URI.create("/volontariat");

    @Inject
    Validator validator;

    /** What the administrator entered in the offer form, already with the sanitised Description. */
    record Entry(String slug, String organization, String organizationLink, String location, String description) {
    }

    @CheckedTemplate
    public static class Templates {
        private Templates() {
        }

        public static native TemplateInstance list(List<VoluntaryService> services);
        public static native TemplateInstance detail(VoluntaryService service, PublicAddress share);
        public static native TemplateInstance form(long id, String slug, String organization, String organizationLink, String location, String description, ValidationMessages validationMessages);
        public static native TemplateInstance delete(VoluntaryService service, boolean removalFailed);
    }
    /** The Offer List: every recorded offer, each entry leading to its Offer Page (UC-003 steps 1-2). */
    // UC-003 BR-001: every recorded offer, no filter
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(VoluntaryService.offerList());
    }

    /**
     * The Offer Page of one offer (UC-003 step 4, A4). The list, the start page and the search all
     * link here, so an offer is read in one place only.
     */
    // UC-003 BR-005: the Short Name is the offer's Public Address
    @GET
    @Path("detail")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@QueryParam("slug") String slug, @Context UriInfo uriInfo) {
        VoluntaryService service = slug == null ? null : VoluntaryService.findBySlug(slug);
        if (service == null) {
            // UC-003 A5: an unknown Short Name, e.g. a removed offer or a changed Short Name
            throw new NotFoundException("VoluntaryService with slug " + slug + " not found");
        }
        // UC-011 BR-003: the Public Address the share control passes on
        return Templates.detail(service, PublicAddress.of(uriInfo, "/volontariat/detail", service.slug));
    }

    // UC-004 BR-001: only an administrator may add, change or remove offers
    // UC-006 BR-001: the administrator role decides, not merely a session
    @GET
    @Path("add")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance add() {
        return Templates.form(0, "", "", "", "", "", ValidationMessages.none());
    }

    // UC-004 BR-001: only an administrator may add, change or remove offers
    // UC-006 BR-001: the administrator role decides, not merely a session
    @GET
    @Path("edit")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@QueryParam("id") long id) {
        VoluntaryService service = VoluntaryService.findById(id);
        if (service == null) {
            // UC-004 A5: the offer was removed meanwhile
            throw new NotFoundException("VoluntaryService with id " + id + " not found");
        }
        // UC-004 A1: the form shows the current values, the Short Name included
        return Templates.form(id, service.slug, service.organization, service.organizationLink, service.location, service.description, ValidationMessages.none());
    }

    /**
     * Shows the confirmation page. Deleting itself happens in {@link #confirmDelete(long)}: a
     * GET must not change data, or a prefetching browser, a link preview or a plain link from
     * another site is enough to remove an offer (UC-002 BR-009).
     */
    // UC-004 BR-001: only an administrator may add, change or remove offers
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-004 BR-003: a removal is only asked for here; it happens after the confirmation is submitted
    @GET
    @Path("delete")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance delete(@QueryParam("id") long id) {
        VoluntaryService service = VoluntaryService.findById(id);
        if (service == null) {
            // UC-004 A5: the offer was removed meanwhile
            throw new NotFoundException("VoluntaryService with id " + id + " not found");
        }
        return Templates.delete(service, false);
    }

    // UC-004 BR-001: only an administrator may add, change or remove offers
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-004 BR-003: the removal happens only once the confirmation form is submitted
    // UC-002 BR-009: a POST whose CSRF token matches this browser's token cookie (quarkus-rest-csrf)
    @POST
    @Path("delete")
    @RolesAllowed("admin")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response confirmDelete(@FormParam("id") long id) {
        VoluntaryService service = VoluntaryService.findById(id);
        if (service == null) {
            // UC-004 A5: the offer was removed meanwhile
            throw new NotFoundException("VoluntaryService with id " + id + " not found");
        }
        return handleDelete(service);
    }

    // UC-004 BR-001: only an administrator may add, change or remove offers
    // UC-006 BR-001: the administrator role decides, not merely a session
    // UC-002 BR-009: a POST whose CSRF token matches this browser's token cookie (quarkus-rest-csrf)
    @POST
    @RolesAllowed("admin")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response submit(@FormParam("id") long id, @FormParam("slug") String slug,
                           @FormParam("organization") String organization,
                           @FormParam("organizationLink") String organizationLink,
                           @FormParam("location") String location,
                           @FormParam("description") String description) {

        // UC-002 BR-010: reduced here rather than in the templates: the description is rendered
        // unescaped, so what is stored has to be safe regardless of how it got in
        String safeDescription = HtmlSanitizer.sanitize(description);

        // UC-004 BR-007: without a Short Name one is derived from Organisation and location.
        // UC-004 BR-008: on an edit only an emptied Short Name is derived anew; a changed
        // Organisation or location leaves the submitted one alone.
        // UC-004 BR-009: an entered Short Name is taken as entered. One of only spaces counts as
        // none, as for events.
        if (slug == null || slug.isBlank()) {
            slug = generateUniqueSlug(id, organization, location);
        }

        Entry entry = new Entry(slug, organization, organizationLink, location, safeDescription);
        return id == 0 ? handleAdd(entry) : handleEdit(id, entry);
    }

    // UC-004 BR-007: Organisation followed by location, cut to 252 characters, a counter until it is free
    static String generateUniqueSlug(long id, String organization, String location) {
        String text = (organization == null ? "" : organization) + " " + (location == null ? "" : location);
        return Slug.unique(text, candidate -> isTakenByAnother(candidate, id));
    }

    // UC-004 BR-006: unique among the offers only; an event of the same name does not count
    private static boolean isTakenByAnother(String slug, long id) {
        VoluntaryService owner = VoluntaryService.findBySlug(slug);
        return owner != null && owner.id != id;
    }

    private Response handleAdd(Entry entry) {
        Set<ValidationMessage> violations = slugViolations(0, entry.slug());
        VoluntaryService service = new VoluntaryService();
        apply(service, entry);
        violations.addAll(validate(service));
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                service.persist();
                QuarkusTransaction.commit();
                Log.info("Created: " + service);
                return Response.seeOther(OFFER_LIST).build();
            } catch (Exception e) {
                // UC-004 A6: the incomplete change is discarded, the form comes back
                Log.error("Unable to save [" + service + "] to database.", e);
                Transactions.rollbackIfActive();
            }
        }
        return rejected(0, entry, violations);
    }

    private Response handleEdit(long id, Entry entry) {
        VoluntaryService service = null;
        Set<ValidationMessage> violations = new LinkedHashSet<>();
        try {
            QuarkusTransaction.begin();

            // Asked before the changed values reach the managed offer: the query would flush them
            // first and run into the database's own uniqueness check instead.
            violations.addAll(slugViolations(id, entry.slug()));

            service = VoluntaryService.findById(id);
            if (service == null) {
                // UC-004 A5: the offer was removed after the form was opened
                Transactions.rollbackIfActive();
                throw new NotFoundException("VoluntaryService with id " + id + " not found");
            }
            apply(service, entry);

            violations.addAll(validate(service));
            if (violations.isEmpty()) {
                service.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + service);
                return Response.seeOther(OFFER_LIST).build();
            }
            Transactions.rollbackIfActive();
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            // UC-004 A6: the incomplete change is discarded, the form comes back
            Log.error("Unable to save [" + service + "] to database.", e);
            Transactions.rollbackIfActive();
        }
        return rejected(id, entry, violations);
    }

    private static void apply(VoluntaryService service, Entry entry) {
        service.slug = entry.slug();
        service.organization = entry.organization();
        service.organizationLink = entry.organizationLink();
        service.location = entry.location();
        service.description = entry.description();
    }

    // UC-004 BR-006: no two offers share a Short Name
    private static Set<ValidationMessage> slugViolations(long id, String slug) {
        Set<ValidationMessage> violations = new LinkedHashSet<>();
        if (isTakenByAnother(slug, id)) {
            violations.add(ValidationMessage.of("slug", SLUG_TAKEN));
        }
        return violations;
    }

    // UC-004 step 7: mandatory values, lengths and a web address
    private Set<ValidationMessage> validate(VoluntaryService service) {
        return validator.validate(service).stream().map(ValidationMessage::of)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // UC-004 A2: the typed values come back with the fields that need correction marked
    private static Response rejected(long id, Entry entry, Set<ValidationMessage> violations) {
        Log.info("Violations encountered while saving an offer: " + violations);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Templates.form(id, entry.slug(), entry.organization(), entry.organizationLink(),
                        entry.location(), entry.description(), ValidationMessages.of(violations)).render())
                .build();
    }

    /**
     * Removes the offer. If the store refuses, the removal is rolled back and the confirmation page
     * comes back with a notice instead of an error page (UC-004 A8).
     */
    private Response handleDelete(VoluntaryService service) {
        try {
            QuarkusTransaction.begin();
            VoluntaryService.deleteById(service.id);
            QuarkusTransaction.commit();
            return Response.seeOther(OFFER_LIST).build();
        } catch (Exception e) {
            Log.error("Unable to delete VoluntaryService [" + service.id + "] from database.", e);
            Transactions.rollbackIfActive();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Templates.delete(service, true).render()).build();
        }
    }
}

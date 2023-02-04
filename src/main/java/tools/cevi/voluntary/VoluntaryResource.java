package tools.cevi.voluntary;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import tools.cevi.infra.ValidationMessage;

import java.util.List;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("volontariat")
public class VoluntaryResource {
    @Inject
    Validator validator;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<VoluntaryService> services);
        public static native TemplateInstance form(long id, String organization, String organizationLink, String location, String description, List<ValidationMessage> validationMessages);
        public static native TemplateInstance delete(long id, VoluntaryService service);
    }
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(VoluntaryService.listAll());
    }

    @GET
    @Path("add")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance add() {
        return Templates.form(0,"", "", "", "", List.of());
    }

    @GET
    @Path("edit")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@QueryParam("id") long id) {
        VoluntaryService voluntaryService = VoluntaryService.findById(id);
        if (voluntaryService == null) {
            throw new NotFoundException("VoluntaryService with id " + id + " not found");
        }
        return Templates.form(id, voluntaryService.organization, voluntaryService.organizationLink, voluntaryService.location, voluntaryService.description, List.of());
    }

    @GET
    @Path("delete")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance delete(@QueryParam("id") long id, @QueryParam("confirmed") Boolean confirmed) {
        VoluntaryService service = VoluntaryService.findById(id);
        if (service == null) {
            throw new NotFoundException("VoluntaryService with id " + id + " not found");
        }
        if (confirmed != null && confirmed) {
            return handleDelete(id);
        } else {
            return Templates.delete(id, service);
        }
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("id") long id, @FormParam("organization") String organization,
                                   @FormParam("organizationLink") String organizationLink,
                                   @FormParam("location") String location,
                                   @FormParam("description") String description) {

        if (id == 0) {
            return handleAdd(organization, organizationLink, location, description);
        } else {
            return handleEdit(id, organization, organizationLink, location, description);
        }
    }

    private TemplateInstance handleAdd(String organization, String organizationLink, String location, String description) {
        VoluntaryService voluntaryService = new VoluntaryService();
        voluntaryService.organization = organization;
        voluntaryService.organizationLink = organizationLink;
        voluntaryService.location = location;
        voluntaryService.description = description;

        Set<ConstraintViolation<VoluntaryService>> violations = validator.validate(voluntaryService);
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                voluntaryService.persist();
                QuarkusTransaction.commit();
                Log.info("Created: " + voluntaryService);
                return Templates.list(VoluntaryService.listAll());
            } catch (Exception e) {
                Log.error("Unable to save [" + voluntaryService  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        return Templates.form(0, organization, organizationLink, location, description, violations.stream().map(ValidationMessage::of).toList());
    }

    private TemplateInstance handleEdit(long id, String organization, String organizationLink, String location, String description) {
        VoluntaryService voluntaryService = null;
        Set<ConstraintViolation<VoluntaryService>> violations = Set.of();
        try {
            QuarkusTransaction.begin();

            voluntaryService = VoluntaryService.findById(id);
            voluntaryService.organization = organization;
            voluntaryService.organizationLink = organizationLink;
            voluntaryService.location = location;
            voluntaryService.description = description;

            violations = validator.validate(voluntaryService);
            if (violations.isEmpty()) {
                voluntaryService.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + voluntaryService);
                return Templates.list(VoluntaryService.listAll());
            } else {
                QuarkusTransaction.rollback();
            }
        } catch (Exception e) {
            Log.error("Unable to save [" + voluntaryService  + "] to database.", e);
            QuarkusTransaction.rollback();
        }
        return Templates.form(id, organization, organizationLink, location, description, violations.stream().map(ValidationMessage::of).toList());
    }

    private TemplateInstance handleDelete(long id) {
        try {
            QuarkusTransaction.begin();
            VoluntaryService voluntaryService = VoluntaryService.findById(id);
            voluntaryService.delete();
            QuarkusTransaction.commit();
        } catch (Exception e) {
            Log.error("Unable to delete VoluntaryService [" + id  + "] from database.", e);
            QuarkusTransaction.rollback();
        }
        return Templates.list(VoluntaryService.listAll());
    }
}
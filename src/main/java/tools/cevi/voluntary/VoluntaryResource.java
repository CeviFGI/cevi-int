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
        public static native TemplateInstance add(String organization, String organizationLink, String location, String description, List<ValidationMessage> validationMessages);
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
        return Templates.add("", "", "", "", List.of());
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("organization") String organization, @FormParam("organizationLink") String organizationLink,
                                   @FormParam("location") String location, @FormParam("description") String description) {

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
                return Templates.list(VoluntaryService.listAll());
            } catch (Exception e) {
                Log.error("Unable to save voluntaryService [" + voluntaryService  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        return Templates.add(organization, organizationLink, location, description, violations.stream().map(ValidationMessage::of).toList());
    }
}
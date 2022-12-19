package tools.cevi.exchange;

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

@Path("austausch")
public class ExchangeResource {
    @Inject
    Validator validator;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Exchange> exchanges);
        public static native TemplateInstance add(String organization, String organizationLink, String description, List<ValidationMessage> validationMessages);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(Exchange.listAll());
    }

    @GET
    @Path("add")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance add() {
        return Templates.add( "", "", "", List.of());
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("organization") String organization, @FormParam("organizationLink") String organizationLink,
                                   @FormParam("description") String description) {

        Exchange exchange = new Exchange();
        exchange.organization = organization;
        exchange.organizationLink = organizationLink;
        exchange.description = description;

        Set<ConstraintViolation<Exchange>> violations = validator.validate(exchange);
        if (violations.isEmpty()) {
            try {
                QuarkusTransaction.begin();
                exchange.persist();
                QuarkusTransaction.commit();
                return Templates.list(Exchange.listAll());
            } catch (Exception e) {
                Log.error("Unable to save exchange [" + exchange  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        return Templates.add(organization, organizationLink, description, violations.stream().map(ValidationMessage::of).toList());
    }
}

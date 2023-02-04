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
        public static native TemplateInstance form(long id, String organization, String organizationLink, String description, List<ValidationMessage> validationMessages);
        public static native TemplateInstance delete(long id, Exchange exchange);
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
    public TemplateInstance form() {
        return Templates.form( 0, "", "", "", List.of());
    }

    @GET
    @Path("edit")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@QueryParam("id") long id) {
        Exchange exchange = Exchange.findById(id);
        if (exchange == null) {
            throw new NotFoundException("Exchange with id " + id + " not found");
        }
        return Templates.form(id, exchange.organization, exchange.organizationLink, exchange.description, List.of());
    }

    @GET
    @Path("delete")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance delete(@QueryParam("id") long id, @QueryParam("confirmed") Boolean confirmed) {
        Exchange exchange = Exchange.findById(id);
        if (exchange == null) {
            throw new NotFoundException("Exchange with id " + id + " not found");
        }
        if (confirmed != null && confirmed) {
            return handleDelete(id);
        } else {
            return Templates.delete(id, exchange);
        }
    }

    @POST
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance submit(@FormParam("id") long id, @FormParam("organization") String organization,
                                   @FormParam("organizationLink") String organizationLink,
                                   @FormParam("description") String description) {

        if (id == 0) {
            return handleAdd(organization, organizationLink, description);
        } else {
            return handleEdit(id, organization, organizationLink, description);
        }
    }

    private TemplateInstance handleAdd(String organization, String organizationLink, String description) {
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
                Log.info("Created: " + exchange);
                return Templates.list(Exchange.listAll());
            } catch (Exception e) {
                Log.error("Unable to save [" + exchange  + "] to database.", e);
                QuarkusTransaction.rollback();
            }
        }
        return Templates.form(0, organization, organizationLink, description, violations.stream().map(ValidationMessage::of).toList());
    }

    private TemplateInstance handleEdit(long id, String organization, String organizationLink, String description) {
        Exchange exchange = null;
        Set<ConstraintViolation<Exchange>> violations = Set.of();
        try {
            QuarkusTransaction.begin();

            exchange = Exchange.findById(id);
            exchange.organization = organization;
            exchange.organizationLink = organizationLink;
            exchange.description = description;

            violations = validator.validate(exchange);
            if (violations.isEmpty()) {
                exchange.persist();
                QuarkusTransaction.commit();
                Log.info("Updated: " + exchange);
                return Templates.list(Exchange.listAll());
            } else {
                QuarkusTransaction.rollback();
            }
        } catch (Exception e) {
            Log.error("Unable to save [" + exchange  + "] to database.", e);
            QuarkusTransaction.rollback();
        }
        return Templates.form(id, organization, organizationLink, description, violations.stream().map(ValidationMessage::of).toList());
    }

    private TemplateInstance handleDelete(long id) {
        try {
            QuarkusTransaction.begin();
            Exchange exchange = Exchange.findById(id);
            exchange.delete();
            QuarkusTransaction.commit();
        } catch (Exception e) {
            Log.error("Unable to delete exchange [" + id  + "] from database.", e);
            QuarkusTransaction.rollback();
        }
        return Templates.list(Exchange.listAll());
    }
}

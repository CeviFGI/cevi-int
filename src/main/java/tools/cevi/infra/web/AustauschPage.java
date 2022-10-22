package tools.cevi.infra.web;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.service.ExchangeService;

import static java.util.Objects.requireNonNull;

@Path("/austausch")
public class AustauschPage {
    @Inject
    private ExchangeService exchangeService;

    private final Template austausch;

    public AustauschPage(Template austausch) {
        this.austausch = requireNonNull(austausch, "page is required");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return austausch.data("exchange", exchangeService.listExchangeOpportunities());
    }
}

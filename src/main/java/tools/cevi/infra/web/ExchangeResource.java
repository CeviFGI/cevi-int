package tools.cevi.infra.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import tools.cevi.domain.ExchangeItem;
import tools.cevi.service.ExchangeService;

@Path("austausch")
public class ExchangeResource {
    @Inject
    ExchangeService service;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance exchangeList(List<ExchangeItem> exchanges);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.exchangeList(service.listExchanges());
    }
}

package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import tools.cevi.domain.Exchange;
import tools.cevi.infra.jpa.ExchangeEntity;

@ApplicationScoped
public class ExchangeService {
    public List<Exchange> listExchanges() {
        return ExchangeEntity.listAll().stream().map(e -> this.map((ExchangeEntity) e)).toList();
    }

    private Exchange map(ExchangeEntity entity) {
        return new Exchange(entity.organization, entity.organizationLink, entity.description);
    }
}

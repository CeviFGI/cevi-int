package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import tools.cevi.domain.ExchangeItem;

@ApplicationScoped
public class ExchangeService {
    public List<ExchangeItem> listExchanges() {
        return List.of(
                new ExchangeItem("Olivenernte", "15. - 24. Oktober 2022", "Palästina"),
                new ExchangeItem("European Unify Conference", "20. - 23. Oktober 2022", "Wien"),
                new ExchangeItem("YE 50th anniversary", "28. Juni - 02. Juli 2023", "Berlin")
        );
    }
}

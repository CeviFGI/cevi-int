package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import tools.cevi.domain.Voluntary;
import tools.cevi.infra.jpa.VoluntaryServiceEntity;

@ApplicationScoped
public class VoluntaryService {
    public List<Voluntary> listVoluntaryServices() {
        return VoluntaryServiceEntity.listAll().stream().map(e -> this.map((VoluntaryServiceEntity) e)).toList();
    }

    private Voluntary map(VoluntaryServiceEntity entity) {
        return new Voluntary(entity.organization, entity.organizationLink, entity.location, entity.description);
    }
}

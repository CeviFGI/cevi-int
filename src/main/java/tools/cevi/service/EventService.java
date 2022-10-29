package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import tools.cevi.domain.Event;
import tools.cevi.infra.jpa.EventEntity;

@ApplicationScoped
public class EventService {
    public List<Event> listEvents() {
        return EventEntity.listAll().stream().map(e -> this.map((EventEntity) e)).toList();
    }

    private Event map(EventEntity entity) {
        return new Event(entity.title, entity.date, entity.location, entity.description);
    }
}

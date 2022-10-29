package tools.cevi.infra.jpa;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class EventEntity extends PanacheEntity {
    public String title;
    public String date;
    public String location;
    @Column(columnDefinition = "TEXT")
    public String description;
}

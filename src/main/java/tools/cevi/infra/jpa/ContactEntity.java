package tools.cevi.infra.jpa;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class ContactEntity extends PanacheEntity {
    @Column(columnDefinition = "TEXT")
    public String message;
}

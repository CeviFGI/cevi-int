package tools.cevi.infra.jpa;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name= "contact_form_entries")
@Entity
public class ContactFormEntryEntity extends PanacheEntity {
    @Column(columnDefinition = "TEXT")
    public String message;
}

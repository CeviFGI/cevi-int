package tools.cevi.contact;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name= "contact_form_entries")
@Entity
public class ContactFormEntry extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    @Column(columnDefinition = "TEXT")
    public String message;

    @Override
    public String toString() {
        return "ContactFormEntry{" +
                "id=" + id +
                ", message='" + message + '\'' +
                '}';
    }
}

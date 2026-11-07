package tools.cevi.contact;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Table(name= "contact_form_entries")
@Entity
public class ContactFormEntry extends PanacheEntityBase {
    /** Upper bound for a contact message, see UC-005 BR-006. */
    public static final int MAX_MESSAGE_LENGTH = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private int id;
    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank
    // UC-005 BR-006: the endpoint is anonymous. Without a limit a single request could write up to
    // the HTTP body limit into the database file, which shares a filesystem with the host.
    @Size(max = MAX_MESSAGE_LENGTH)
    public String message;

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        // UC-005 BR-007: the message is personal data and must not leak into the logs through a
        // concatenated entity; the id is what makes an entry traceable.
        return "ContactFormEntry{id=" + id + '}';
    }
}

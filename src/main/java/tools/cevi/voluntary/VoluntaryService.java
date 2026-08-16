package tools.cevi.voluntary;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Table(name = "voluntary_services")
@Entity
public class VoluntaryService extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public int id;
    @NotBlank
    @Size(max = 255)
    public String organization;
    @NotBlank
    @Size(max = 255)
    // The offer will eventually be rendered as a hyperlink, so the value must not be able to carry
    // another scheme (javascript:, data:) into the page.
    @Pattern(regexp = "^https?://.+", message = "muss mit http:// oder https:// beginnen")
    public String organizationLink;
    @NotBlank
    @Size(max = 255)
    public String location;
    @Column(columnDefinition = "TEXT")
    @NotBlank
    @Size(max = 65535)
    public String description;

    @Override
    public String toString() {
        return "VoluntaryService{" +
                "id=" + id +
                ", organization='" + organization + '\'' +
                ", organizationLink='" + organizationLink + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

package tools.cevi.voluntary;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Table(name = "voluntary_services")
@Entity
public class VoluntaryService extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public Long id;
    @NotBlank
    public String organization;
    @NotBlank
    public String organizationLink;
    @NotBlank
    public String location;
    @Column(columnDefinition = "TEXT")
    @NotBlank
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

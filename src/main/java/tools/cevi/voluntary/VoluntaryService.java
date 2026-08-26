package tools.cevi.voluntary;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
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

    /**
     * The most recently recorded offers, for the extract on the start page (BR-048). An offer
     * carries no date of its own, so the id is the only order the entity model offers.
     */
    public static List<VoluntaryService> newest(int limit) {
        return findAll(Sort.by("id", Sort.Direction.Descending)).page(0, limit).list();
    }

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

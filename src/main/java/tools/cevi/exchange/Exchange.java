package tools.cevi.exchange;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.annotations.GenericGenerator;

@Table(name = "exchanges")
@Entity
public class Exchange extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(name = "id", updatable = false, nullable = false)
    public Long id;
    @NotBlank
    public String organization;
    @NotBlank
    public String organizationLink;
    @NotBlank
    @Column(columnDefinition = "TEXT")
    public String description;

    @Override
    public String toString() {
        return "Exchange{" +
                "id=" + id +
                ", organization='" + organization + '\'' +
                ", organizationLink='" + organizationLink + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

package tools.cevi.infra.jpa;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "exchanges")
@Entity
public class ExchangeEntity extends PanacheEntity {
    public String organization;
    public String organizationLink;
    @Column(columnDefinition = "TEXT")
    public String description;
}

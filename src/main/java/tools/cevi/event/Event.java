package tools.cevi.event;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.List;

@Table(name = "events")
@Entity
public class Event extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(name = "id", updatable = false, nullable = false)
    public Long id;
    @NotBlank
    public String title;
    @NotBlank
    public String date;
    @NotBlank
    public String location;
    @NotNull
    public LocalDate displayDate;
    @NotBlank
    @Column(columnDefinition = "TEXT")
    public String description;

    public static List<Event> upcomingEvents() {
        return list("displayDate >= :today", Sort.by("displayDate"), Parameters.with("today", LocalDate.now()));
    }

    public static long deleteByTitle(String title) {
        return delete("title = :title", Parameters.with("title", title));
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", location='" + location + '\'' +
                ", displayDate=" + displayDate +
                ", description='" + description + '\'' +
                '}';
    }
}

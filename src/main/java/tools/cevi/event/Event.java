package tools.cevi.event;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

import java.time.LocalDate;
import java.util.List;

@Table(name = "events")
@Entity
public class Event extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public Long id;
    @NotBlank
    @Size(max= 255)
    public String title;
    @NotBlank
    @Column(unique = true)
    @Size(max= 255)
    public String slug;
    @NotBlank
    @Size(max= 255)
    public String date;
    @NotBlank
    @Size(max= 255)
    public String location;
    @NotNull
    public LocalDate displayDate;
    @NotBlank
    @Column(columnDefinition = "TEXT")
    @Size(max= 65535)
    public String description;

    public static List<Event> upcomingEvents() {
        return list("displayDate >= :today", Sort.by("displayDate"), Parameters.with("today", LocalDate.now()));
    }

    public static boolean isSlugUnique(String slug) {
        return find("slug = :slug", Parameters.with("slug", slug)).count() == 0;
    }

    public static Event findBySlug(String slug) {
        return (Event) find("slug = :slug", Parameters.with("slug", slug)).stream().findFirst().orElse(null);
    }

    public static long deleteById(Long id) {
        return delete("id = :id", Parameters.with("id", id));
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", slug='" + slug + '\'' +
                ", date='" + date + '\'' +
                ", location='" + location + '\'' +
                ", displayDate=" + displayDate +
                ", description='" + description + '\'' +
                '}';
    }
}

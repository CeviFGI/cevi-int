
package tools.cevi.event;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import tools.cevi.infra.Changes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Table(name = "events")
@Entity
public class Event extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public int id;
    // UC-002 BR-002: title, short name, date text, location, display date and description are mandatory
    // UC-002 BR-003: 255 characters each, 65535 for the description
    @NotBlank
    @Size(max= 255)
    public String title;
    // UC-002 BR-004: no two events share a short name
    // UC-001 BR-003: the short name identifies the event uniquely; its public address lasts until an
    // administrator changes the short name (UC-002 BR-011)
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
    // UC-002 BR-012: First Day and Last Day are optional, but only together, the Last Day not before
    // the First Day; checked by EventPeriod when the form is submitted
    public LocalDate firstDay;
    public LocalDate lastDay;
    @NotBlank
    @Column(columnDefinition = "TEXT")
    @Size(max= 65535)
    public String description;

    // UC-013 BR-004: set by the system, never by the administrator. Both receive the moment of
    // creation; the Last Change moves on every save that changes a detail. Hibernate calls
    // @PreUpdate only for an entity it found changed, so a save that changes nothing keeps both.
    @Column(nullable = false, updatable = false)
    public Instant created;
    @Column(nullable = false)
    public Instant lastChange;

    @PrePersist
    void recordCreation() {
        created = Changes.now();
        lastChange = created;
    }

    @PreUpdate
    void recordChange() {
        lastChange = Changes.now();
    }

    /** Whether the entry was changed after it was created (UC-013 BR-005). */
    public boolean wasChanged() {
        return lastChange.isAfter(created);
    }

    /** Whether the event has a period, and with it a Calendar Entry. */
    // UC-012 BR-001: the Calendar Entry is offered only while both days are recorded
    public boolean hasPeriod() {
        return firstDay != null && lastDay != null;
    }

    // UC-001 BR-001: listed only while the display date is today or later
    // UC-001 BR-002: closest display date first
    public static List<Event> upcomingEvents() {
        return list("displayDate >= :today", Sort.by("displayDate"), Parameters.with("today", LocalDate.now()));
    }

    /**
     * The nearest events only, for the extract on the start page (UC-008 BR-002). Reading the whole list
     * and cutting it in the template would grow with the table; this one does not.
     */
    // UC-008 BR-002: only the nearest events still ahead
    public static List<Event> upcomingEvents(int limit) {
        return find("displayDate >= :today", Sort.by("displayDate"), Parameters.with("today", LocalDate.now()))
                .page(0, limit).list();
    }

    /** The Upcoming Events changed most recently, the latest first (UC-013 BR-002). */
    public static List<Event> recentlyChangedUpcoming(int limit) {
        return find("displayDate >= :today", Sort.by("lastChange", Sort.Direction.Descending).and("id", Sort.Direction.Descending),
                Parameters.with("today", LocalDate.now())).page(0, limit).list();
    }

    // UC-001 BR-003: an event is reached under its short name
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
                ", firstDay=" + firstDay +
                ", lastDay=" + lastDay +
                ", description='" + description + '\'' +
                ", created=" + created +
                ", lastChange=" + lastChange +
                '}';
    }
}

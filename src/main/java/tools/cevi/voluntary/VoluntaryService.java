package tools.cevi.voluntary;

import java.text.Collator;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.cevi.infra.Changes;

@Table(name = "voluntary_services")
@Entity
public class VoluntaryService extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public int id;
    // UC-004 BR-002: Organisation, Organisation Link, location, Short Name and Description are mandatory
    // UC-004 BR-004: 255 characters each, 65535 for the description
    // UC-004 BR-006: no two offers share a Short Name; an event may carry the same one
    @NotBlank
    @Column(unique = true)
    @Size(max = 255)
    public String slug;
    @NotBlank
    @Size(max = 255)
    public String organization;
    @NotBlank
    @Size(max = 255)
    // UC-004 BR-005: the offer is rendered as a hyperlink, so the value must not be able to carry
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

    /**
     * The most recently recorded offers, for the extract on the start page (UC-008 BR-002). An offer
     * carries no date of its own, so the id is the only order the entity model offers.
     */
    // UC-008 BR-002: only the most recently recorded offers
    public static List<VoluntaryService> newest(int limit) {
        return findAll(Sort.by("id", Sort.Direction.Descending)).page(0, limit).list();
    }

    /**
     * Every recorded offer for the Offer List (UC-003 step 2, BR-001), by Organisation in
     * alphabetical order as a reader of de-CH expects it (umlauts beside their base letter, case
     * ignored), then in the order recorded. The same order the search uses among equally ranked
     * offers (UC-010 BR-004), so an offer stands in the same place in both.
     */
    public static List<VoluntaryService> offerList() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("de-CH"));
        return VoluntaryService.<VoluntaryService>listAll().stream()
                .sorted(Comparator.<VoluntaryService, String>comparing(s -> s.organization, collator)
                        .thenComparingInt(s -> s.id))
                .toList();
    }

    /** The offers changed most recently, the latest first (UC-013 BR-002). */
    public static List<VoluntaryService> recentlyChanged(int limit) {
        return findAll(Sort.by("lastChange", Sort.Direction.Descending).and("id", Sort.Direction.Descending))
                .page(0, limit).list();
    }

    /** The offer stored under {@code slug}, or {@code null} (UC-004 BR-006). */
    public static VoluntaryService findBySlug(String slug) {
        return find("slug = :slug", Parameters.with("slug", slug)).firstResult();
    }

    @Override
    public String toString() {
        return "VoluntaryService{" +
                "id=" + id +
                ", slug='" + slug + '\'' +
                ", organization='" + organization + '\'' +
                ", organizationLink='" + organizationLink + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", created=" + created +
                ", lastChange=" + lastChange +
                '}';
    }
}

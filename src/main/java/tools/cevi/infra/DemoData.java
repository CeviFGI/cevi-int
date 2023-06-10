package tools.cevi.infra;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import tools.cevi.auth.User;
import tools.cevi.event.Event;
import tools.cevi.exchange.Exchange;
import tools.cevi.voluntary.VoluntaryService;

import java.time.LocalDate;

@ApplicationScoped
@SuppressWarnings("unused")
public class DemoData {
    @Transactional
    void startup(@Observes StartupEvent event) {
        if (Event.count() == 0) {
            Log.info("Database is empty. Generating some demo data...");
            createEvent("Past Event", "In der Vergangenheit", "Antwerp", "Generalversammlung des YMCA", LocalDate.now().minusDays(1));
            createEvent("Olivenernte", "15. - 24. Oktober 2023", "Palästina", "Unterstützt durch Horyzon. Kosten: 900 - 1'200 USD je nach Unterkunft + Reise. Das Programm ist eine internationale Solidaritäts- und Schutzaktion für die palästinensischen Bauern und Familien, um sie bei der Ernte ihrer Olivenbäume zu unterstützen, die von Angriffen, Entwurzlung, Beschlagnahmungen und Einschränkungen bedroht sind. Neben der Olivenernte umfasst das Programm auch Führungen in verschiedenen Städten, palästinensischer Kultur und einführernde Vorträge zu Konfliktthemen.<br><a href=\"https://www.jai-pal.org/en/campaigns/olive-tree-campaign/olive-picking-program/picking2022-inv\">Weitere Informationen</a>", LocalDate.now());
            createEvent("European Unify Conference", "20. - 23. Oktober 2023", "Wien", "Fest zu den christlichen Wurzeln des Cevi. Kosten: 95 Euro Festival + Reise + Unterkunft.<br><a href=\"https://www.ymca-unify.eu/european/unify_2022\">Weitere Informationen</a>, <a href=\"https://mcusercontent.com/4164786c200962ea4be64ffd8/files/20dfc6b4-c4f8-19fe-6006-d42a99a47a60/Program_Unify_2022.pdf\">Programm</a>", LocalDate.now().plusDays(1));
            createEvent("YE 50th anniversary", "28. Juni - 02. Juli 2023", "Berlin", "Es werden ca. 500 Personen erwartet. Detailinformationen folgen Ende September/Anfang Oktober.", LocalDate.now().plusDays(2));
            createEvent("YMCA General Assembly", "Mai 2023", "Antwerp", "Generalversammlung des YMCA", LocalDate.now().plusDays(3));

            createVoluntaryService("Horyzon", "https://horyzon.ch/", "Kolumbien, Palästina",  "Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>");
            createVoluntaryService("Volunteers for Europe", "http://vfe.cvjm.de/", "Deutschland",  "Dauer 2 Wochen oder 3-6 Monate. Siehe Webseite von CVJM");

            createExchange("Olivenernte", "15. - 24. Oktober 2022", "Palästina");
            createExchange("European Unify Conference", "20. - 23. Oktober 2022", "Wien");
            createExchange("YE 50th anniversary", "28. Juni - 02. Juli 2023", "Berlin");

            User.add("patrick", "patrick", "admin");

            Log.info("Generation finished");
        }
    }

    private void createExchange(String organization, String organizationLink, String description) {
        Exchange entity = new Exchange();
        entity.organization = organization;
        entity.organizationLink = organizationLink;
        entity.description = description;
        entity.persist();
    }

    private void createVoluntaryService(String organization, String organizationLink, String location, String description) {
        VoluntaryService entity = new VoluntaryService();
        entity.organization = organization;
        entity.organizationLink = organizationLink;
        entity.location = location;
        entity.description = description;
        entity.persist();
    }

    private void createEvent(String title, String date, String location, String description, LocalDate displayDate) {
        Event entity = new Event();
        entity.title = title;
        entity.slug = Slug.of(title).toString();
        entity.date = date;
        entity.location = location;
        entity.description = description;
        entity.displayDate = displayDate;
        entity.persist();
    }
}

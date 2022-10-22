package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import tools.cevi.domain.EventItem;

@ApplicationScoped
public class EventService {
    public List<EventItem> listEvents() {
        return List.of(
                new EventItem("Olivenernte", "15. - 24. Oktober 2022", "Palästina", "Unterstützt durch Horyzon. Kosten: 900 - 1'200 USD je nach Unterkunft + Reise. Das Programm ist eine internationale Solidaritäts- und Schutzaktion für die palästinensischen Bauern und Familien, um sie bei der Ernte ihrer Olivenbäume zu unterstützen, die von Angriffen, Entwurzlung, Beschlagnahmungen und Einschränkungen bedroht sind. Neben der Olivenernte umfasst das Programm auch Führungen in verschiedenen Städten, palästinensischer Kultur und einführernde Vorträge zu Konfliktthemen.<br><a href=\"https://www.jai-pal.org/en/campaigns/olive-tree-campaign/olive-picking-program/picking2022-inv\">Weitere Informationen</a>"),
                new EventItem("European Unify Conference", "20. - 23. Oktober 2022", "Wien", "Fest zu den christlichen Wurzeln des Cevi. Kosten: 95 Euro Festival + Reise + Unterkunft.<br><a href=\"https://www.ymca-unify.eu/european/unify_2022\">Weitere Informationen</a>, <a href=\"https://mcusercontent.com/4164786c200962ea4be64ffd8/files/20dfc6b4-c4f8-19fe-6006-d42a99a47a60/Program_Unify_2022.pdf\">Programm</a>"),
                new EventItem("YMCA General Assembly", "Mai 2023", "Antwerp", ""),
                new EventItem("YE 50th anniversary", "28. Juni - 02. Juli 2023", "Berlin", "Es werden ca. 500 Personen erwartet. Detailinformationen folgen Ende September/Anfang Oktober.")
        );
    }
}

package tools.cevi.service;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import tools.cevi.domain.VolontariatItem;

@ApplicationScoped
public class VoluntaryService {
    public List<VolontariatItem> listVoluntaryServices() {
        return List.of(
                new VolontariatItem("Horyzon", "https://horyzon.ch/", "Kolumbien, Palästina",  "Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>"),
                new VolontariatItem("Volunteers for Europe", "http://vfe.cvjm.de/", "Kolumbien, Palästina",  "Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>"),
                new VolontariatItem("Horyzon", "https://horyzon.ch/", "Kolumbien, Palästina",  "Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>"),
                new VolontariatItem("Horyzon", "https://horyzon.ch/", "Kolumbien, Palästina",  "Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>"),
                new VolontariatItem("Horyzon", "https://horyzon.ch/", "Kolumbien, Palästina",  "Dauer 2 Wochen oder 3-6 Monate. Siehe <a href=\"https://horyzon.ch/de/spenden-und-unterstuetzen/einsaetze-weltweit/volontariat/\">Weitere Informationen</a>")
                );
    }
}

# Handover — Stand 2026-11-07 (nach der 10. Session)

Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** UC-003 implementieren (eigene Volontariatsseite), dann der Rest von
Revision 8 (Abschnitt „Danach“, Punkt 1).

## Stand

- UC-001, 002, 005, 006, 007, 009 `Tested`.
- UC-004 implementiert und getestet (Commit `ac6ef6e`), Status bleibt `Approved`, bis UC-003 steht:
  Erst mit der Volontariatsseite lassen sich die Postcondition „Offer Page spiegelt die Änderung
  sofort“ und BR-008 „die alte Adresse führt nicht mehr zum Angebot“ über HTTP prüfen. Danach
  `/use-case-coverage UC-004`, dann `Tested`.
- UC-003, 008, 010, 011 `Approved`, noch nicht implementiert.
- `tooling/docker.sh verify` grün, alles committet.
- Spec-Lint grün mit `--strict` (3 Befunde `SPEC_CHANGED_AFTER_STATUS` in
  `docs/.spec-lint-baseline.json` akzeptiert: UC-001, UC-002, UC-009). Für UC-001/002 wurden in der
  9. Session nur die geänderten Elemente geprüft (Delta-Audit), kein voller `/use-case-coverage`.
  Nach einer Spec-Änderung an einem `Tested`-Use-Case: Audit, dann `--update-baseline`.
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`
- NFR-006, NFR-011, NFR-021 stehen auf `In Progress`, weil UC-004 sie verlinkt; wieder `Verified`,
  sobald UC-004 `Tested` ist.

## Was die 10. Session an UC-004 gemacht hat

- `VoluntaryService.slug` (`NOT NULL UNIQUE`) + `findBySlug`; Ableitung über den gemeinsamen Helfer
  `Slug.unique(text, taken)`, den jetzt auch `EventResource` nutzt.
- Flyway-**Java**-Migration `src/main/java/db/migration/V1_0_5__VoluntaryServiceSlug.java` (leitet
  Kurznamen bestehender Volontariate mit `Slug` ab; SQLite kann keine Diakritika entfernen). Test
  gegen eigene DB-Datei: `src/test/java/db/migration/VoluntaryServiceSlugMigrationTest`.
- `VoluntaryResource` auf das Muster von `EventResource` gebracht: Weiterleitung 303 nach dem
  Speichern, 400 bei abgelehnter Eingabe, A5 (404), A6 (Formular zurück), A8 (Bestätigungsseite mit
  Hinweis, 500).
- Tests: `VoluntaryManageTest` (Kurzname, A1, A2, A5, A6, A8), `VoluntaryResourceTest` ergänzt
  (Benutzer ohne Admin-Rolle, CSRF bei Ändern/Löschen, Javadocs), `SlugTest` (`unique`).
- Gewählte Defaults (der Benutzer weiss davon): Kurzname = `Slug.unique(Organisation + " " + Ort)`;
  Kurzname nur aus Leerzeichen gilt als leer und wird abgeleitet (wie bei Anlässen); die Migration
  vergibt die id, wenn nichts Lesbares ableitbar ist.

## Konventionen und Fallen

- Code-Marker `// UC-XXX BR-YYY:` direkt über dem Code, der die Regel durchsetzt (in Qute:
  `{! UC-XXX BR-YYY: … !}`); jedes Test-Javadoc nennt Flow oder Regel (`A1`, `BR-003`,
  „Main success scenario“). Ausnahme: an `V1.0.4__Slug.sql` fehlt `// UC-002 BR-004:` bewusst
  (angewendete Migration nicht ändern, sonst Flyway-Prüfsummenfehler).
- In Test-Javadocs und Markern keine FR-/NFR-IDs nennen, die der Use Case nicht verlinkt (der Audit
  meldet das als Drift).
- Testdaten mit Präfix `CLEANUP UC-XXX ` speichern, damit das Aufräumen nach dem Test sie erfasst.
  Volontariate direkt in der DB anlegen: `slug` ist Pflicht – `VoluntaryFixture.uniqueSlug(org)`.
- `tools.cevi.infra.Transactions.rollbackIfActive()` statt `QuarkusTransaction.rollback()` verwenden.
- Eindeutigkeit eines Kurznamens **vor** dem Setzen der Werte auf die verwaltete Entität prüfen: die
  Abfrage löst sonst einen Auto-Flush aus und läuft in die UNIQUE-Constraint der DB (landet dann im
  A6-Zweig ohne Feldmarkierung).
- Header und Footer verlinken `/anlaesse` und `/kontakt` auf jeder Seite: ein Assert auf
  `href="/kontakt"` beweist nichts – immer auf den Link der Seite selbst ankern
  (z. B. `href="/kontakt">Schreib uns</a>`).
- Der Mock-Mailer (dev/test) loggt jede Mail inkl. Text unter dem Logger `quarkus-mailer`; Log-Tests
  müssen diese Zeile ausnehmen.
- Fehlschlagende Speicherung testen: SQLite-Trigger mit `RAISE` (Beispiele `uc005_a2_insert` in
  `ContactResourceTest`, `uc004_a6_*`/`uc004_a8_delete` in `VoluntaryManageTest`); fehlschlagenden
  Mailversand: eigenes Testprofil mit echtem Mailer gegen `127.0.0.1:1`
  (`ContactNotificationFailureTest`).
- Unerwarteten Fehler (500-Seite) testen: `FailingPageResource` unter `src/test`
  (`/test-only/failing-page`), existiert nur im Testprofil.
- `PublicPages.java` und die Seitenlisten in `EditorAssetsTest`, `NoThirdPartyAssetsTest`,
  `SearchResourceTest` enthalten nur anonym erreichbare Seiten – eine Seite hinter `@RolesAllowed`
  misst dort ohne Sitzung nur die Login-Seite. Die neue Volontariatsseite gehört dort hinein.
- Entität im Test vor einem POST mit `findById` geladen? Danach liefert `findById` die
  zwischengespeicherte alte Kopie (Sitzung des Tests). Ergebnis über HTTP oder in eigener
  Transaktion prüfen (`QuarkusTransaction.requiringNew().call(...)`, siehe
  `VoluntaryManageTest.stored`).
- Sonar `analyze_code_snippet` meldet ohne Dateipfad immer `java:S1598` (Paket ↔ Pfad); das ist
  kein Befund im Code.

## Danach

1. Revision 8 implementieren:
   - **UC-003** Volontariatsseite unter dem Kurznamen (Muster: `EventResource.detail`,
     `/anlaesse/detail?slug=…`). Ganze Karte in der Liste führt zur Seite; „Zur Organisation“ nur
     noch auf der Seite (BR-003); Ausklappen in der Liste entfällt (`tags/voluntaryService.html`,
     Tests `a_long_offer_is_shortened_…` und `an_offer_leads_to_the_organisation_…` in
     `VoluntaryResourceTest` anpassen). Hilfetext zur Beschreibung im Formular („Ein Angebot hat
     keine eigene Seite…“) anpassen. In `VoluntaryManageTest` die BR-008-Tests um einen Abruf der
     alten/neuen Adresse ergänzen. Noch offen: Reihenfolge der Liste (UC-003 Schritt 2) – sinnvollen
     Default wählen und melden.
   - **UC-011** Teilen (`share.js` wiederverwenden), **UC-010** (Treffer → Volontariatsseite statt
     `/volontariat?angebot=id`), **UC-008** (Links im Auszug; Umfang des Auszugs wie bisher).
   - Sonar-Check für neuen Code.
2. Coverage-Audits und Status / FR- und NFR-Status nachführen (UC-003, 004, 008, 010, 011; optional
   auch ein voller Audit für UC-001 und UC-002, siehe „Stand“).
3. Offene Fragen OQ-005 … OQ-015 mit dem Product Owner klären (`/open-questions`).

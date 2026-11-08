# Handover – Stand 2026-11-08 (nach der 13. Session)

Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** Die Lücken der Coverage-Audits von UC-008 und UC-010 schliessen (unten), dann
beide auf `Tested` setzen.

## Stand

- UC-001 … 007, 009, 011 `Tested`. `tooling/docker.sh verify` grün, alles committet.
- UC-008 und UC-010: Code vollständig (Audit 2026-11-08: Implementierung 17/17 bzw. 27/27), Tests
  lückenhaft (siehe unten). Status beider noch `Approved`.
- Spec-Lint grün mit `--strict` (3 Befunde `SPEC_CHANGED_AFTER_STATUS` in
  `docs/.spec-lint-baseline.json` akzeptiert: UC-001, UC-002, UC-009). Nach einer Spec-Änderung an
  einem `Tested`-Use-Case: Audit, dann `--update-baseline`.
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`
- FR-/NFR-Status: `Verified`, sobald alle Use Cases, die die Anforderung verlinken, `Tested` sind;
  `In Progress`, solange erst ein Teil davon `Tested` ist.

## Massnahmen

### 1. UC-008 Enter the Platform – Testlücken schliessen (`/use-case-test UC-008`)

Audit: Tests 6/17. Alle Punkte betreffen Tests (`IndexResourceTest`, `StartPageE2ETest`), ausser den
Markern in 1.9.

1. **Schritt 5:** Test für den Fachgruppen-Hinweis („Du weisst noch nicht, was passt?“). Auf den Link
   der Seite selbst ankern: `href="/kontakt">Kontakt aufnehmen</a>` (Header/Footer verlinken
   `/kontakt` überall).
2. **Post-S-2:** Test, dass `GET /` keine Daten ändert (Muster
   `VoluntaryBrowseTest.browsing_changes_no_data`).
3. **Schritt 3 / BR-004 (Anlass-Hälfte):** kommenden Anlass anlegen → erscheint im Auszug auf `/`,
   Link führt auf `/anlaesse/detail?slug=…`; vergangener Anlass (Display Date vorbei) fehlt. Die
   Startseite nutzt `Event.upcomingEvents(int)` – die bisherigen Tests decken nur die Variante ohne
   Argument. Muster für Angebote: `VoluntaryBrowseTest.the_entry_on_the_start_page_leads_to_the_offer_page`.
4. **Schritt 2:** Lead prüfen, der die Fachgruppe International des Cevi Schweiz als Betreiberin
   nennt (`home.html:15-16`), nicht nur den Titel.
5. **A1 / A2 / BR-003 / Post-F-1:** im Test mit leeren Abschnitten (`IndexResourceTest:71`) auch den
   „Schreib uns“-Button in jedem `.empty`-Block (`home.html:51`, `:75`) prüfen, dass der Hinweis aus
   Schritt 5 trotzdem folgt (A2 „continues at step 5“) und dass keine Daten geändert wurden.
6. **A4:** mit `@TestSecurity` auf `/` prüfen, dass die Wartungslinks in `<div class="admin-bar">` als
   `btn--ghost` stehen, kein `btn--primary` (Muster `VoluntaryBrowseTest:306-318`).
7. **BR-002:** Limit des Auszugs testen: 4 kommende Anlässe bzw. 3 Angebote → der überzählige fehlt
   auf `/` (`IndexResource.EVENT_TEASERS = 3`, `OFFER_TEASERS = 2`).
8. **BR-004 (Gegenfall):** Angebotskarte auf `/` hat keinen Link auf die Organisation
   (`organizationLink`).
9. **Marker / Javadoc:**
   - `// UC-008 BR-00X:` bzw. `{! UC-008 BR-00X: … !}` direkt über dem durchsetzenden Code statt nur in
     Javadoc/Qute-Kommentaren: `IndexResource.java:34,47`, `Event.java:59`,
     `VoluntaryService.java:53`, `home.html:46-47,54-55`, `tags/voluntaryService.html:2`.
   - Leerer A2-Block (`home.html:71`) hat gar keinen Marker.
   - `StartPageE2ETest`: Tests in Zeile 26 und 40 ohne Javadoc mit Flow/Regel.
   - `IndexResourceTest:53` soll BR-002/A3 nennen.
   - FR-IDs (FR-023, FR-034) aus den Test-Javadocs `IndexResourceTest:38` und `StartPageE2ETest:13`
     entfernen.
10. **Drift (entschieden):** `StartPageE2ETest.java:56-60` nennt NFR-013, das UC-008 nicht verlinkt.
    Entscheid des Users: Verweis aus dem Test entfernen (NFR-013 ist über die Design-System-Tests
    für alle Seiten abgedeckt); die Spezifikation bleibt unverändert. Ist die Prüfung im Test dann
    nur noch eine Doublette der Design-System-Tests, den Test ganz streichen.

Danach Coverage-Audit wiederholen, `verify`, dann UC-008 `Tested`; FR-023, FR-034 `Verified`, FR-043
von `In Progress` auf `Verified`.

### 2. UC-010 Search Events and Offers – eine Testlücke, veraltete Kommentare

Audit: Tests 26/27.

1. **BR-002 Bilddaten:** in `SearchResourceTest.the_description_is_searched_as_read_text_not_as_markup`
   eine Beschreibung mit `<img src="data:image/png;base64,…">` speichern und zeigen, dass ein Token
   aus der data-URL nichts findet (Code entfernt den Tag bereits, `Excerpt.java:49`).
2. **Drift – veraltete Kommentare** (Spezifikation seit FR-043: Angebotstreffer führt auf die
   Volontariatsseite; der Code tut das schon). Empfehlung (vom User noch nicht ausdrücklich
   bestätigt, aber durch FR-043 vorgegeben): nur die Kommentare nachführen.
   - `templates/tags/searchHit.html:4-5` („to the offer's entry in the Offer List“)
   - `SearchResourceTest.java:38-39` („the Offer List entry a search leads to“)
   - `css/search.css:3` („the offer a search result leads to“)

Danach `verify`, UC-010 `Tested`; FR-041, FR-042, NFR-046, NFR-047 `Verified`. Mit UC-010 werden auch
NFR-041 und C-021 `Verified` (verlinkt von UC-009, UC-010, UC-011; die anderen beiden sind `Tested`).

### 3. Kleinigkeiten

- `docs/requirements.md:266` (Verifikationstabelle NFR-041) nennt nur `EventShareTest` für das
  8-KB-Skriptlimit; `VoluntaryShareTest` prüft es jetzt auch für die Volontariatsseite – über
  `/requirements` ergänzen.
- UC-009 BR-004 hat dieselbe Schwäche, die der Audit von UC-011 fand: `ShareEventE2ETest` prüft nur
  Anfragen an fremde Hosts, nicht, dass das Teilen gar keine Anfrage auslöst. Muster:
  `ShareOfferE2ETest.requestsFromNowOn()`.

### 4. Optional / später

- Voller Audit für UC-001 und UC-002 (in der 9. Session nur Delta-Audit); `/spec-review UC-003` für
  die Ergänzung von Schritt 2 (Reihenfolge der Liste); die drei überlappenden Testpaare in
  `VoluntaryResourceTest`/`VoluntaryManageTest` zusammenlegen.
- Offene Fragen OQ-005 … OQ-015 mit dem Product Owner klären (`/open-questions`).

## Konventionen und Fallen

- Code-Marker `// UC-XXX BR-YYY:` direkt über dem Code, der die Regel durchsetzt (in Qute:
  `{! UC-XXX BR-YYY: … !}`); jedes Test-Javadoc nennt Flow oder Regel (`A1`, `BR-003`,
  „Main success scenario“). Ausnahme: an `V1.0.4__Slug.sql` fehlt `// UC-002 BR-004:` bewusst
  (angewendete Migration nicht ändern, sonst Flyway-Prüfsummenfehler).
- In Test-Javadocs und Markern keine FR-/NFR-IDs nennen, die der Use Case nicht verlinkt (der Audit
  meldet das als Drift).
- Testdaten mit Präfix `CLEANUP UC-XXX ` speichern, damit das Aufräumen nach dem Test sie erfasst.
  Volontariate direkt in der DB anlegen: `slug` ist Pflicht – `VoluntaryFixture.uniqueSlug(org)`.
- Public Address (Teilen, UC-009/UC-011) immer über `tools.cevi.infra.PublicAddress.of(...)` bauen;
  die Share-Stand-ins der e2e-Tests liegen in `e2e/ShareStandIns`.
- `tools.cevi.infra.Transactions.rollbackIfActive()` statt `QuarkusTransaction.rollback()` verwenden.
- Eindeutigkeit eines Kurznamens **vor** dem Setzen der Werte auf die verwaltete Entität prüfen: die
  Abfrage löst sonst einen Auto-Flush aus und läuft in die UNIQUE-Constraint der DB (landet dann im
  A6-Zweig ohne Feldmarkierung).
- Header und Footer verlinken `/anlaesse` und `/kontakt` auf jeder Seite: ein Assert auf
  `href="/kontakt"` beweist nichts – immer auf den Link der Seite selbst ankern
  (z. B. `href="/kontakt">Schreib uns</a>`).
- In der Volontariatskarte steht der Ort vor dem Namen der Organisation: Suchen in der Karte ab
  `<article` beginnen, nicht ab dem Namen.
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
  misst dort ohne Sitzung nur die Login-Seite.
- Links mit Kurznamen in Templates immer über `.queryValue` bauen (Kurznamen werden wie eingegeben
  gespeichert und dürfen `&`, `#`, `+` enthalten).
- Entität im Test vor einem POST mit `findById` geladen? Danach liefert `findById` die
  zwischengespeicherte alte Kopie (Sitzung des Tests). Ergebnis über HTTP oder in eigener
  Transaktion prüfen (`QuarkusTransaction.requiringNew().call(...)`, siehe
  `VoluntaryManageTest.stored`).
- Sonar `analyze_code_snippet` meldet ohne Dateipfad immer `java:S1598` (Paket ↔ Pfad); das ist
  kein Befund im Code.
- Einzelne e2e-Klasse ohne Unit-Tests und Coverage-Gate:
  `tooling/docker.sh mvn verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=KlassenName -Djacoco.skip=true`

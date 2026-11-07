# Handover — Stand 2026-11-07 (nach der 12. Session)

Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** Rest von Revision 8 (Abschnitt „Danach“, Punkt 1): UC-011 (Teilen auf der
Volontariatsseite).

## Stand

- UC-001, 002, 003, 004, 005, 006, 007, 009 `Tested`.
- 12. Session: Coverage-Audits für UC-003 und UC-004 wiederholt, die gefundenen Testlücken
  geschlossen (UC-003: Reihenfolge innerhalb einer Organisation, Auszug der Startseite → Volontariatsseite,
  Felder nach dem Besuch unverändert; UC-004: Bereinigung der Beschreibung über den Endpunkt, Liste
  und Volontariatsseite nach Hinzufügen/Ändern/Löschen, UC-004-Marker im `VoluntaryFormE2ETest`).
  Zweiter Audit: UC-003 20/20, UC-004 bis auf Post-S-1 „Volontariatsseite eines neuen Angebots“,
  danach ergänzt. Beide auf `Tested`; FR-008 … FR-013, FR-022, FR-027, FR-033, FR-035, FR-038,
  FR-044 und NFR-006, NFR-011, NFR-021 auf `Verified`; FR-043 `In Progress` (UC-008 fehlt noch).
- UC-008: der Test, dass der Auszug der Startseite zur Volontariatsseite führt, existiert jetzt
  (`VoluntaryBrowseTest.the_entry_on_the_start_page_leads_to_the_offer_page`, unter UC-003 A4).
  Audit für UC-008 fehlt. UC-010: Treffer führen auf die Volontariatsseite, Audit fehlt. UC-011
  (Teilen) noch offen.
- Audit-Hinweis ohne Handlungsbedarf: `VoluntaryResourceTest` und `VoluntaryManageTest` überlappen
  in drei Testpaaren (Hinzufügen, Bearbeitungsformular, Löschen) – Zusammenlegen optional.
- `tooling/docker.sh verify` grün, alles committet.
- Spec-Lint grün mit `--strict` (3 Befunde `SPEC_CHANGED_AFTER_STATUS` in
  `docs/.spec-lint-baseline.json` akzeptiert: UC-001, UC-002, UC-009). Für UC-001/002 wurden in der
  9. Session nur die geänderten Elemente geprüft (Delta-Audit), kein voller `/use-case-coverage`.
  Nach einer Spec-Änderung an einem `Tested`-Use-Case: Audit, dann `--update-baseline`.
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`
- Optional: `/spec-review UC-003` für die Ergänzung von Schritt 2 (Reihenfolge der Liste).

## Was die 11. Session an UC-003 gemacht hat

- `VoluntaryResource.detail` unter `/volontariat/detail?slug=…` (A5: 404), Template
  `VoluntaryResource/detail.html` (Wartungslinks A6 als `admin-bar`, „Zur Organisation“ nach der
  Beschreibung). Liste via `VoluntaryService.offerList()` (de-CH-Collator, dann id).
- Karte `tags/voluntaryService.html` wie die Anlass-Karte; Ausklappen, `chosen`, `?angebot=` und das
  zugehörige CSS (`.disclosure`, `.card--chosen`) sowie die Extension `shortened` entfernt.
- 404-Seite bietet zusätzlich „Zum Volontariat“. Tests: `VoluntaryBrowseTest`,
  `VoluntaryBrowseE2ETest`; die Demo-Seite `/volontariat/detail?slug=horyzon-kolumbien-palastina`
  steht in `PublicPages` und den Seitenlisten der Qualitätstests.

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
  misst dort ohne Sitzung nur die Login-Seite.
- Links mit Kurznamen in Templates immer über `.queryValue` bauen (Kurznamen werden wie eingegeben
  gespeichert und dürfen `&`, `#`, `+` enthalten).
- Entität im Test vor einem POST mit `findById` geladen? Danach liefert `findById` die
  zwischengespeicherte alte Kopie (Sitzung des Tests). Ergebnis über HTTP oder in eigener
  Transaktion prüfen (`QuarkusTransaction.requiringNew().call(...)`, siehe
  `VoluntaryManageTest.stored`).
- Sonar `analyze_code_snippet` meldet ohne Dateipfad immer `java:S1598` (Paket ↔ Pfad); das ist
  kein Befund im Code.

## Danach

1. Revision 8 implementieren:
   - **UC-011** Teilen auf der Volontariatsseite (`share.js` wiederverwenden, Muster
     `EventResource/detail.html` und `EventResource.publicAddress`; Dialog mit Organisation und Ort).
   - Sonar-Check für neuen Code.
2. Coverage-Audits und Status / FR- und NFR-Status nachführen (UC-008, 010, 011; optional
   auch ein voller Audit für UC-001 und UC-002, siehe „Stand“).
3. Offene Fragen OQ-005 … OQ-015 mit dem Product Owner klären (`/open-questions`).

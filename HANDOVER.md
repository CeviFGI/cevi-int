# Handover — Stand 2026-11-07 (nach der 8. Session)

Übergabe für die nächste Session. Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** Die offenen Code-↔-Spec-Entscheide aus Abschnitt 2 mit dem Product Owner
klären, danach Revision 8 implementieren (Abschnitt 3), beginnend mit UC-004 (Kurzname).

## Stand

| Use Case | Status |
|---|---|
| UC-001, UC-002, UC-005, UC-006, UC-007, UC-009 | `Tested` (Coverage-Audits ohne Lücke, FR-Status nachgeführt) |
| UC-003, UC-004, UC-008, UC-010, UC-011 | `Approved` (Revision 8, via `/spec-review` freigegeben, noch nicht implementiert) |

- `tooling/docker.sh verify` grün (321 Unit-, 96 e2e-Tests, Coverage-Gate erfüllt). Alles committet.
- Spec-Lint: `0 errors, 0 warnings` mit `--strict`; 1 Befund in `docs/.spec-lint-baseline.json`
  akzeptiert (UC-009 SPEC_CHANGED_AFTER_STATUS). Prüfen mit
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`.
- Requirements Revision 8: Jedes Volontariat bekommt eine eigene Detailseite mit Kurzname
  (FR-043, FR-044) und Teilen (FR-045). Spezifiziert in UC-003, UC-004, UC-010, UC-011,
  Entitätsmodell (`VOLUNTARY_SERVICE.slug`) und Glossar.
- NFR-006, NFR-011, NFR-021 stehen auf `In Progress`, weil UC-004 sie jetzt (wie UC-002) verlinkt;
  sie werden wieder `Verified`, sobald UC-004 `Tested` ist.
- Offene Fragen OQ-005 … OQ-015 in `docs/open_questions.md` (alle nicht blockierend). FR-029 und
  FR-030 sind `Deferred`, bis OQ-005 / OQ-006 beantwortet sind.
- `docs/architecture/stack.md`: Mail Service ist das einzige externe System (Client-Layer = Quarkus
  Mailer, direkt aus der Resource, kein eigener Adapter).

### Konventionen und Fallen

- Code-Marker `// UC-XXX BR-YYY:` direkt über dem Code, der die Regel durchsetzt (in Qute:
  `{! UC-XXX BR-YYY: … !}`); jedes Test-Javadoc nennt Flow oder Regel (`A1`, `BR-003`,
  „Main success scenario“). Ausnahme: an `V1.0.4__Slug.sql` fehlt `// UC-002 BR-004:` bewusst
  (angewendete Migration nicht ändern, sonst Flyway-Prüfsummenfehler).
- In Test-Javadocs und Markern keine FR-/NFR-IDs nennen, die der Use Case nicht verlinkt (der Audit
  meldet das als Drift).
- Testdaten mit Präfix `CLEANUP UC-XXX ` speichern, damit das Aufräumen nach dem Test sie erfasst.
- `tools.cevi.infra.Transactions.rollbackIfActive()` statt `QuarkusTransaction.rollback()` verwenden.
- Header und Footer verlinken `/anlaesse` und `/kontakt` auf jeder Seite: ein Assert auf
  `href="/kontakt"` beweist nichts – immer auf den Link der Seite selbst ankern
  (z. B. `href="/kontakt">Schreib uns</a>`).
- Der Mock-Mailer (dev/test) loggt jede Mail inkl. Text unter dem Logger `quarkus-mailer`; Log-Tests
  müssen diese Zeile ausnehmen (Prod nicht betroffen).
- Fehlschlagende Speicherung testen: SQLite-Trigger mit `RAISE` (Beispiel `uc005_a2_insert` in
  `ContactResourceTest`); fehlschlagenden Mailversand: eigenes Testprofil mit echtem Mailer gegen
  `127.0.0.1:1` (`ContactNotificationFailureTest`).
- Unerwarteten Fehler (500-Seite) testen: `FailingPageResource` unter `src/test`
  (`/test-only/failing-page`), existiert nur im Testprofil.
- `PublicPages.java` und die Seitenlisten in `EditorAssetsTest`, `NoThirdPartyAssetsTest`,
  `SearchResourceTest` enthalten nur anonym erreichbare Seiten – eine Seite hinter `@RolesAllowed`
  misst dort ohne Sitzung nur die Login-Seite (deshalb ist `/version` nicht mehr drin).

## 1. Revision-8-Specs — freigegeben

Alle fünf am 2026-11-07 via `/spec-review` freigegeben. Bei der Umsetzung beachten:
- UC-004: neue A8 (Entfernen schlägt fehl), A5 deckt auch das Absenden für ein gelöschtes Volontariat ab.
- UC-003: neue A6 (Bearbeitungslinks auf der Volontariatsseite), A3 gilt nur noch für die Liste.
- UC-008: Auslöser statt Vorbedingung; A3 nennt direkt geöffnete Anlass-/Volontariatsseiten.
- Liegen gelassene Infos (nicht blockierend): Ableitungsform des Kurznamens (UC-004 BR-007),
  Kurzname nur aus Leerzeichen (UC-004 BR-009), Reihenfolge der Volontariatsliste (UC-003 Schritt 2),
  Umfang des Auszugs auf der Startseite (UC-008 BR-002).

## 2. Abweichungen Code ↔ Spec — Entscheid nötig (welche Seite stimmt?)

- **UC-001 (für `/spec-review UC-001`):** BR-003 („Link bleibt gültig“) widerspricht UC-002, wo der
  Kurzname beim Bearbeiten geändert werden kann – der alte Link gibt dann 404. BR-004 sagt nicht,
  was ohne brauchbare Wortgrenze passiert (der Code schneidet hart, `ExcerptTest` dokumentiert das).
- **UC-002 (für `/spec-review UC-002`):** Schritt 9 sagt, der neue Anlass sei in der Liste
  sichtbar – mit vergangenem Anzeigedatum erscheint er dort aber nicht (UC-001 BR-001).
- **UC-009:** Marker in `EventResource/detail.html:25` zitiert die alte BR-005; `share.js:1` und
  `EventShareTest.java:126/136` behaupten 2 KB (NFR-041) – seit Revision 7 gilt nur 8 KB total;
  `ShareEventE2ETest.java:91` nennt NFR-042, das UC-009 nicht verlinkt. Notiz zu NFR-041 in
  `requirements.md` („tests still state the narrower revision-6 rule“) ist teilweise veraltet.

## 3. Danach

1. Revision 8 implementieren: UC-004 (Kurzname, Flyway-Migration inkl. Kurznamen für bestehende
   Volontariate), UC-003 (Volontariatsseite), UC-011 (`share.js` wiederverwenden), UC-010
   (Treffer → Volontariatsseite), UC-008 (Links im Auszug). Sonar-Check für neuen Code.
2. Coverage-Audits und Status / FR-Status nachführen.
3. Offene Fragen OQ-005 … OQ-015 mit dem Product Owner klären (`/open-questions`).

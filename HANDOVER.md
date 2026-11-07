# Handover — Stand 2026-11-07 (nach der 6. Session)

Übergabe für die nächste Session. Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** Revision-8-Specs freigeben (Abschnitt 1: `/spec-review UC-XXX`), danach
die offenen Code-↔-Spec-Entscheide aus Abschnitt 2 mit dem Product Owner klären.

## Stand

| Use Case | Status |
|---|---|
| UC-001, UC-002, UC-005, UC-006, UC-007, UC-009 | `Tested` (Coverage-Audits ohne Lücke, FR-Status nachgeführt) |
| UC-003, UC-004, UC-008, UC-010, UC-011 | `Draft` (Revision 8, noch nicht implementiert) |

- `tooling/docker.sh verify` grün (321 Unit-, 96 e2e-Tests, Coverage-Gate erfüllt). Alles committet.
- Spec-Lint: `0 errors, 0 warnings` mit `--strict`; 1 Befund in `docs/.spec-lint-baseline.json`
  akzeptiert (UC-009 SPEC_CHANGED_AFTER_STATUS). Prüfen mit
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`.
- Requirements Revision 8: Jedes Volontariat bekommt eine eigene Detailseite mit Kurzname
  (FR-043, FR-044) und Teilen (FR-045). Spezifiziert in UC-003, UC-004, UC-010, UC-011,
  Entitätsmodell (`VOLUNTARY_SERVICE.slug`) und Glossar.
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

## 1. Freigabe der Revision-8-Specs (Product Owner)

**UC-003, UC-004, UC-008, UC-010, UC-011** stehen auf `Draft`.
Vorgehen: `/spec-review UC-XXX` → Status auf Approved setzen → implementieren.

Vom Product Owner am 2026-11-07 bestätigt (noch nicht via `/spec-review` freigegeben):
- Kurzname bleibt beim Bearbeiten erhalten, auch wenn Organisation/Ort ändern (UC-004 BR-008);
  ein beim Bearbeiten geleerter Kurzname wird neu abgeleitet.
- Ein eingegebener Kurzname wird unverändert gespeichert (nur Eindeutigkeit und Länge geprüft),
  wie bei Anlässen (UC-004 BR-009, Entitätsmodell).
- Volontariat darf denselben Kurznamen wie ein Anlass haben (UC-004 BR-006).
- Adresse der Volontariatsseite: `/volontariat/detail?slug=<Kurzname>` (UC-011 BR-003).
- Startseite: Einträge im Auszug führen auf die Detailseite (UC-008 BR-004).

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

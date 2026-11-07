# Handover — Stand 2026-11-07 (Ende 4. Session)

Übergabe für die nächste Session. Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** Abschnitt 3 (Testlücken UC-005, dann UC-007) mit `/use-case-test UC-XXX`,
danach jeweils `/use-case-coverage UC-XXX`, Drift mit dem Product Owner klären, bei grünem Audit
Status auf `Tested` und FR-Status nachführen (Spec-Lint `REQ_STATUS_DRIFT` zeigt die Zielwerte).

## Stand

- Requirements Revision 8: Jedes Volontariat bekommt eine eigene Detailseite (Offer Page) mit
  Kurzname (FR-043, FR-044) und Teilen (FR-045). Spezifiziert in UC-003, UC-004, UC-010, UC-011,
  Entitätsmodell (`VOLUNTARY_SERVICE.slug`) und Glossar. **Noch nichts davon ist implementiert.**
- Spec-Lint: `0 errors, 0 warnings` mit `--strict`; 1 Befund in `docs/.spec-lint-baseline.json`
  akzeptiert (UC-009 SPEC_CHANGED_AFTER_STATUS: Audit ohne Lücke, verify grün). Prüfen mit
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`.
- `tooling/docker.sh verify` ist grün (Stand Ende 4. Session: 297 Unit-, 96 e2e-Tests,
  Coverage-Gate erfüllt). Alles committet.
- **UC-001 ist `Tested`** (4. Session; drittes Coverage-Audit ohne Drift, letzte Lücke A2
  geschlossen). FR-001/002/003/036 `Verified`; FR-022/027/035/038 `In Progress`, weil UC-003 noch
  `Draft` ist. Product-Owner-Entscheide 2026-11-07 umgesetzt: Anlassseite ohne Admin-Links und ohne
  Countdown, Karten ohne „sichtbar bis“; der Teaser „Fragen dazu? / Schreib uns“ ist neu UC-001
  BR-007 (Schritt 5).
- **Falle bei Navigationslinks in Tests:** Header und Footer verlinken `/anlaesse` und `/kontakt` auf jeder
  Seite. Ein Assert auf `href="/kontakt"` beweist darum nichts – immer auf den Link der Seite selbst
  ankern (z. B. `href="/kontakt">Schreib uns</a>`).
- **UC-002 und UC-006 sind `Tested`** (Commit `490c34e`; Coverage-Audits 34/34 bzw. 27/27,
  FR-Status nachgeführt).
  Spec-Änderungen nach PO-Entscheid: UC-002 A8 (Entfernen schlägt fehl), A5 auch beim Absenden,
  NFR-006/011/021 verlinkt; UC-006 BR-009 (Abmelden nur per Formular) und BR-010 (zwei allgemeine
  Admin-Einstiege `/auth`, `/admin`). Neu: `CurrentUser.isSignOutAvailable()` – Fehlerseiten ohne
  CSRF-Token (404/405 für angemeldete Admins) lieferten vorher 500.
- Marker `// UC-002 BR-004:` fehlt bewusst an `V1.0.4__Slug.sql` (angewendete Migration nicht
  ändern, sonst Flyway-Prüfsummenfehler).
- Die „Open Points for Confirmation“ aus `requirements.md` sind jetzt OQ-005 … OQ-015 in
  `docs/open_questions.md` (alle nicht blockierend, offen). FR-029 und FR-030 sind `Deferred`, bis
  OQ-005 / OQ-006 beantwortet sind.
- Die FR-Status folgen den Use Cases (`/requirements` bzw. Spec-Lint `REQ_STATUS_DRIFT`); FRs von
  Use Cases, die noch nicht `Tested` sind, stehen auf `Open` bzw. `In Progress`.
- Product-Owner-Entscheide 2026-11-07 (in der Spec nachgeführt): UC-007 BR-004 Quellengruppen
  „offen beim Aufruf, einklappbar“; CSRF-Token an „diesen Browser“ gebunden (UC-002, UC-004);
  NFR-031 erlaubt die Client-IP im Protokoll der Anmeldeversuche.

## 1. Freigabe der überarbeiteten Specs (Product Owner)

Status `Draft`, warten auf Review/Genehmigung: **UC-003, UC-004, UC-008, UC-010, UC-011**.
Vorgehen: `/spec-review UC-XXX` → Status auf Approved setzen → implementieren.

Annahmen am 2026-11-07 vom Product Owner bestätigt (noch nicht via `/spec-review` freigegeben):
- Kurzname bleibt beim Bearbeiten erhalten, auch wenn Organisation/Ort ändern (UC-004 BR-008);
  ein beim Bearbeiten geleerter Kurzname wird neu abgeleitet.
- Ein eingegebener Kurzname wird unverändert gespeichert (nur Eindeutigkeit und Länge geprüft),
  wie bei Anlässen (UC-004 BR-009, Entitätsmodell).
- Volontariat darf denselben Kurznamen wie ein Anlass haben (UC-004 BR-006).
- Adresse der Volontariatsseite: `/volontariat/detail?slug=<Kurzname>` (UC-011 BR-003).
- Startseite: Einträge im Auszug führen auf die Detailseite (UC-008 BR-004).

## 2. Bugs aus den Coverage-Audits — erledigt

Alle behoben und getestet (Commit `fcf4810`). Neu: `tools.cevi.infra.Transactions.rollbackIfActive()`
statt `QuarkusTransaction.rollback()` in allen Resources verwenden.

## 3. Testlücken (`/use-case-test UC-XXX`)

- **UC-005:** A2, A3 (Mailfehler), BR-007 (Nachricht nicht im Log), leere Spam-Antwort,
  Feldfehler bei A4, Fehlen von Absenderfeldern (BR-004), Hilfetexte (BR-008).
- **UC-007:** A3 (500-Seite), kein E2E-Test, Versionswerte, Rückweg auf 404-Seite,
  Inhalt Datenschutzseite, „keine Daten geändert“.
- UC-001/UC-002/UC-006 erledigt (inkl. Code-Marker und Test-Javadoc).
- Übrige: Code-Marker `// UC-XXX BR-YYY:` fehlen weitgehend (Format in `docs/architecture/stack.md`);
  UC-ID im Test-Javadoc fehlt noch.

## 4. Abweichungen Code ↔ Spec — Entscheid nötig (welche Seite stimmt?)

- **UC-001 (für `/spec-review UC-001`, Hinweise aus dem Audit):** BR-003 („Link bleibt gültig“)
  widerspricht UC-002, wo der Kurzname beim Bearbeiten geändert werden kann – der alte Link gibt
  dann 404. BR-004 sagt nicht, was ohne brauchbare Wortgrenze passiert (sehr langes Wort/URL: der
  Code schneidet hart, `ExcerptTest` dokumentiert das).
- **UC-002 (für `/spec-review UC-002`):** Schritt 9 sagt, der neue Anlass sei in der Liste
  sichtbar – mit vergangenem Anzeigedatum erscheint er dort aber nicht (UC-001 BR-001).
- **`docs/architecture/stack.md`:** Integration-Tabelle hat eine Zeile `Service → Persistence`,
  obwohl Service `–` ist → `/stack` erneut laufen lassen.
- **UC-007:** `/version` wird in `PublicPages.java`, `NoThirdPartyAssetsTest`, `EditorAssetsTest`
  als öffentliche Seite geführt (misst anonym nur die Login-Seite). Veralteter Marker
  `IndexResourceTest.java:49` (BR-001 → BR-004).
- **UC-009:** Marker in `EventResource/detail.html:25` zitiert die alte BR-005; `share.js:1` und
  `EventShareTest.java:126/136` behaupten 2 KB (NFR-041) – seit Revision 7 gilt nur 8 KB total;
  `ShareEventE2ETest.java:91` nennt NFR-042, das UC-009 nicht verlinkt. Notiz zu NFR-041 in
  `requirements.md` („tests still state the narrower revision-6 rule“) ist teilweise veraltet.

## 5. Danach

1. Revision 8 implementieren: UC-004 (Kurzname, Flyway-Migration inkl. Kurznamen für bestehende
   Volontariate), UC-003 (Volontariatsseite), UC-011 (`share.js` wiederverwenden), UC-010
   (Treffer → Volontariatsseite), UC-008 (Links im Auszug). Sonar-Check für neuen Code.
2. Coverage-Audits wiederholen und Status / FR-Status nachführen (`/requirements` setzt FR-Status
   aus den Use Cases).
3. Offene Fragen OQ-005 … OQ-015 mit dem Product Owner klären (`/open-questions`).

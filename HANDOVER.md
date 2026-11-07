# Handover — Stand 2026-11-07

Übergabe für die nächste Session. Nach Erledigung der Punkte diese Datei löschen oder nachführen.

## Stand

- Requirements Revision 8: Jedes Volontariat bekommt eine eigene Detailseite (Offer Page) mit
  Kurzname (FR-043, FR-044) und Teilen (FR-045). Spezifiziert in UC-003, UC-004, UC-010, UC-011,
  Entitätsmodell (`VOLUNTARY_SERVICE.slug`) und Glossar. **Noch nichts davon ist implementiert.**
- Spec-Lint: `0 errors, 0 warnings` mit `--strict`; 1 Befund in `docs/.spec-lint-baseline.json`
  akzeptiert (UC-009 SPEC_CHANGED_AFTER_STATUS: Audit ohne Lücke, verify grün). Prüfen mit
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`.
- `tooling/docker.sh verify` war am 2026-11-07 grün (94 e2e-Tests, Coverage-Gate erfüllt).
- Die „Open Points for Confirmation“ aus `requirements.md` sind jetzt OQ-005 … OQ-015 in
  `docs/open_questions.md` (alle nicht blockierend, offen). FR-029 und FR-030 sind `Deferred`, bis
  OQ-005 / OQ-006 beantwortet sind.
- Nach den Coverage-Audits vom 2026-11-07 wurden UC-002 und UC-006 von Implemented auf Approved
  zurückgesetzt; die FR-Status folgen jetzt den Use Cases (daher viele FRs auf `Open`).

## 1. Freigabe der überarbeiteten Specs (Product Owner)

Status `Draft`, warten auf Review/Genehmigung: **UC-003, UC-004, UC-008, UC-010, UC-011**.
Vorgehen: `/spec-review UC-XXX` → Status auf Approved setzen → implementieren.

Ohne Rückfrage getroffene Annahmen, bitte bestätigen:
- Kurzname bleibt beim Bearbeiten erhalten, auch wenn Organisation/Ort ändern (UC-004 BR-008);
  ein beim Bearbeiten geleerter Kurzname wird neu abgeleitet.
- Ein eingegebener Kurzname wird unverändert gespeichert (nur Eindeutigkeit und Länge geprüft),
  wie bei Anlässen (UC-004 BR-009, Entitätsmodell).
- Volontariat darf denselben Kurznamen wie ein Anlass haben (UC-004 BR-006).
- Adresse der Volontariatsseite: `/volontariat/detail?slug=<Kurzname>` (UC-011 BR-003).
- Startseite: Einträge im Auszug führen auf die Detailseite (UC-008 BR-004).

## 2. Bugs aus den Coverage-Audits (zuerst angehen)

| Prio | UC | Problem | Ort |
|---|---|---|---|
| hoch | UC-002 BR-005 | `generateUniqueSlug` erhöht `counter` nie → dritter Anlass mit gleichem Titel (ohne Kurzname) hängt in Endlosschleife | `EventResource.java:184-192` |
| hoch | UC-002 BR-006 | Zähler-Pfad kürzt nicht auf 252 Zeichen → kann 255 überschreiten | `EventResource.java:189` |
| mittel | UC-006 A4 | Logout entscheidet nach Cookie-Präsenz statt `identity.isAnonymous()` → abgelaufene Session bekommt „Abgemeldet“ | `AuthResource.java:60-65` |
| mittel | UC-006 BR-007 | Secure-Flag des Session-Cookies nicht explizit gesetzt / nicht belegt | `application-prod.properties` |
| mittel | UC-005 A2 | Wirft `commit()`, wirft danach vermutlich auch `rollback()` → keine Mail, Fehlerseite | `ContactResource.java:79-86` |
| niedrig | UC-001 BR-006 | „Neuen Anlass eintragen“ ist `btn--primary` (soll ruhiger sein, wie `btn--ghost` auf Start-/Detailseite) | `EventResource/list.html:12` |
| niedrig | UC-007 BR-004 | Gruppen der Quellen sollen auf dem Handy eingeklappt sein, sind aber immer `open` (oder Spec auf „einklappbar“ ändern, FR-039 sagt nur das) | `IndexResource/fgi.html` |

Nach dem Fix jeweils `/use-case-implement UC-XXX`, Test dazu, dann Status wieder auf Implemented.

## 3. Testlücken (`/use-case-test UC-XXX`)

- **UC-001:** A1 (leere Liste), Datumstext/Ort in der Liste, Link Liste → Detailseite, E2E läuft
  den Hauptablauf nicht durch, BR-006, „keine Daten geändert“.
- **UC-002:** A6 (Speicherfehler), BR-006, BR-008 (ungültiges Anzeigedatum), Pflichtfelder/A2,
  Längenlimits ausser Titel, Add/Edit ohne CSRF-Token, kein E2E-Test für das Anlass-Formular.
- **UC-005:** A2, A3 (Mailfehler), BR-007 (Nachricht nicht im Log), leere Spam-Antwort,
  Feldfehler bei A4, Fehlen von Absenderfeldern (BR-004), Hilfetexte (BR-008).
- **UC-006:** Log der Anmeldeversuche (BR-005), Idle-Timeout (BR-006), bcrypt (BR-002),
  Cookie-Attribute (BR-007), Fehlerseitentext (A1), Nicht-Admin → 403 (BR-001),
  Bootstrap-Abbruch (BR-008), Wartungsfunktion nach Logout.
- **UC-007:** A3 (500-Seite), kein E2E-Test, Versionswerte, Rückweg auf 404-Seite,
  Inhalt Datenschutzseite, „keine Daten geändert“.
- Alle: Marker `// UC-XXX BR-YYY:` im Code und UC-ID im Test-Javadoc fehlen weitgehend
  (Format in `docs/architecture/stack.md`).

## 4. Abweichungen Code ↔ Spec — Entscheid nötig (welche Seite stimmt?)

- **UC-001:** Countdown auch auf der Anlassseite; Teaser „Fragen dazu? / Schreib uns“; Admin-Links
  auf der Anlassseite (A4 nennt nur die Liste).
- **UC-002:** CSRF-Token ist nicht an die angemeldete Session gebunden (Spec sagt „own session“) →
  Spec auf „dieser Browser“ lockern oder Token binden. Fehlgeschlagenes Löschen hat keinen Flow.
  UC-004/UC-006-Code zitiert UC-002 BR-009/BR-010 → eigene Regeln oder als Requirement.
- **UC-006:** Zusätzliche Einstiege `GET /auth` und `GET /admin`; Client-IP im INFO/WARN-Log
  widerspricht NFR-031 (IPs nur DEBUG).
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

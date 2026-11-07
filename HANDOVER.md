# Handover — Stand 2026-11-07 (nach der 9. Session)

Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** Revision 8 implementieren (Abschnitt „Danach“, Punkt 1), beginnend mit UC-004.
Die Abweichungen zwischen Code und Spec der letzten Handover sind entschieden und erledigt
(Commit `f26b515`).

## Stand

- UC-001, 002, 005, 006, 007, 009 `Tested`; UC-003, 004, 008, 010, 011 `Approved` (Revision 8:
  eigene Volontariatsseite mit Kurzname und Teilen), noch nicht implementiert.
- `tooling/docker.sh verify` grün, alles committet.
- Spec-Lint grün mit `--strict` (3 Befunde `SPEC_CHANGED_AFTER_STATUS` in
  `docs/.spec-lint-baseline.json` akzeptiert: UC-001, UC-002, UC-009). Für UC-001/002 wurden in der
  9. Session nur die geänderten Elemente geprüft (Delta-Audit), kein voller `/use-case-coverage`.
  Nach einer Spec-Änderung an einem `Tested`-Use-Case: Audit, dann `--update-baseline`.
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.13.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`
- NFR-006, NFR-011, NFR-021 stehen auf `In Progress`, weil UC-004 sie verlinkt; wieder `Verified`,
  sobald UC-004 `Tested` ist.

## Konventionen und Fallen

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
  müssen diese Zeile ausnehmen.
- Fehlschlagende Speicherung testen: SQLite-Trigger mit `RAISE` (Beispiel `uc005_a2_insert` in
  `ContactResourceTest`); fehlschlagenden Mailversand: eigenes Testprofil mit echtem Mailer gegen
  `127.0.0.1:1` (`ContactNotificationFailureTest`).
- Unerwarteten Fehler (500-Seite) testen: `FailingPageResource` unter `src/test`
  (`/test-only/failing-page`), existiert nur im Testprofil.
- `PublicPages.java` und die Seitenlisten in `EditorAssetsTest`, `NoThirdPartyAssetsTest`,
  `SearchResourceTest` enthalten nur anonym erreichbare Seiten – eine Seite hinter `@RolesAllowed`
  misst dort ohne Sitzung nur die Login-Seite.
- Entität im Test vor einem POST mit `findById` geladen? Danach liefert `findById` die
  zwischengespeicherte alte Kopie (Sitzung des Tests). Ergebnis über HTTP prüfen (Beispiel
  `EventEditTest.edit_changed_slug_retires_the_previous_address`).
- Sonar `analyze_code_snippet` meldet ohne Dateipfad immer `java:S1598` (Paket ↔ Pfad); das ist
  kein Befund im Code.

## Entscheide der 9. Session (in den Specs festgehalten)

- Kurzname eines Anlasses bleibt, bis ein Admin ihn ändert (UC-001 BR-003, UC-002 BR-011) – gleich
  wie bei Volontariaten (UC-004 BR-008); für UC-004 also dasselbe Verhalten implementieren.
- Auszug ohne brauchbare Wortgrenze: harter Schnitt mit „…“ (UC-001 BR-004, `Excerpt`).
- UC-002 A9: Anlass mit vergangenem Anzeigedatum wird gespeichert, aber nicht gelistet.

## Danach

1. Revision 8 implementieren: UC-004 (Kurzname, Flyway-Migration inkl. Kurznamen für bestehende
   Volontariate), UC-003 (Volontariatsseite), UC-011 (`share.js` wiederverwenden), UC-010
   (Treffer → Volontariatsseite), UC-008 (Links im Auszug). Sonar-Check für neuen Code.
   Nicht spezifiziert (in der Review als Info liegen gelassen, sinnvollen Default wählen und
   melden): Ableitungsform des Kurznamens (UC-004 BR-007, wie bei Anlässen), Kurzname nur aus
   Leerzeichen (UC-004 BR-009), Reihenfolge der Volontariatsliste (UC-003 Schritt 2), Umfang des
   Auszugs auf der Startseite (UC-008 BR-002, wie bisher).
2. Coverage-Audits und Status / FR- und NFR-Status nachführen (optional auch ein voller Audit für
   UC-001 und UC-002, siehe „Stand“).
3. Offene Fragen OQ-005 … OQ-015 mit dem Product Owner klären (`/open-questions`).

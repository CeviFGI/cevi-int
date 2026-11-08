# Handover – Stand 2026-11-08 (nach der 15. Session)

Nach Erledigung der Punkte diese Datei löschen oder nachführen.

**Nächster Schritt:** `/acceptance-protocol` – alle Use Cases sind `Tested`, die Abnahme mit dem
Product Owner steht an.

## Stand

- UC-001 … UC-011 alle `Tested`. `tooling/docker.sh verify` grün, alles committet.
- Spec-Lint grün mit `--strict` (3 Befunde `SPEC_CHANGED_AFTER_STATUS` in
  `docs/.spec-lint-baseline.json` akzeptiert: UC-001, UC-002, UC-009). Nach einer Spec-Änderung an
  einem `Tested`-Use-Case: Audit, dann `--update-baseline`.
  `python3 ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/3.14.0/skills/spec-review/scripts/spec_lint.py --docs docs --strict`
- FR-/NFR-Status: `Verified`, sobald alle Use Cases, die die Anforderung verlinken, `Tested` sind;
  `In Progress`, solange erst ein Teil davon `Tested` ist.

## Massnahmen

### 1. Optional / später

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

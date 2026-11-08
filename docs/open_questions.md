# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Does the shared Public Address keep the `?slug=` query parameter?

**Element:** UC-009 BR-003
**Raised by:** /use-case-implement, 2026-11-06
**Blocking:** No
**Status:** Resolved

BR-003 defines the Public Address as scheme, host and port followed by "the event's path built from its Short
Name — with no parameters". The Event Page is served today under `/anlaesse/detail?slug=<Short Name>`, so the
Short Name itself travels as a query parameter. Readings: (a) share `/anlaesse/detail?slug=<Short Name>` as the
event list links to it, and read "no parameters" as no parameter beyond the Short Name; (b) introduce a path
`/anlaesse/<Short Name>` without any query, which changes the routing of UC-001.

**Answer:** (a) — the shared address is `<origin>/anlaesse/detail?slug=<Short Name>`; no other parameter, no
fragment.
**Answered by:** Patrick Uhlmann, 2026-11-06
**Resolved in:** UC-009 BR-003

## OQ-002: Where does the host of the Public Address come from when no script runs?

**Element:** UC-009 A4
**Raised by:** /use-case-implement, 2026-11-06
**Blocking:** No
**Status:** Resolved

A4 shows the Public Address as text when the share script does not run, so the server must render it. BR-003
wants the host the visitor's browser actually loaded the page from and "no host from anything the request merely
claims"; the server knows the host only from the request. Readings: (a) render the absolute address from the
request's scheme, host and port as Quarkus resolves them — the `Host` header, and in production the
`X-Forwarded-*` headers that only Traefik can set (`application-prod.properties`, `docs/deployment.md` §2) — which
the script replaces with the browser's own origin whenever it runs; (b) render only the path, so the server never
reflects a host.

**Answer:** (a) — the server renders the absolute address from the request's scheme, host and port as Quarkus
resolves them behind the trusted proxy; the script uses the browser's own origin.
**Answered by:** Patrick Uhlmann, 2026-11-06
**Resolved in:** UC-009 A4, UC-009 BR-003

## OQ-003: How long is the Description excerpt on the Search Results Page, and around which word?

**Element:** UC-010 step 3
**Raised by:** /use-case-implement, 2026-11-06
**Blocking:** No
**Status:** Resolved

Step 3 shows "the passage around the first occurrence of the Search Term" in the Description, but states neither
the length of the passage nor what "the first occurrence" is for a Search Term of several words, which BR-003
lets match in any order. Readings: (a) 160 characters, as the excerpt on the lists (FR-035), starting shortly
before the earliest occurrence of any word of the Search Term, with an ellipsis where text was cut, and no
highlighting; (b) the same, with the matching words highlighted; (c) the occurrence of the first word of the
Search Term as typed.

**Answer:** (a) — 160 characters around the earliest occurrence of any word of the Search Term, an ellipsis at
each cut end, no highlighting.
**Answered by:** Patrick Uhlmann, 2026-11-06
**Resolved in:** UC-010 step 3, UC-010 BR-008

## OQ-004: Where does a word end, and which letters count as the same letter?

**Element:** UC-010 BR-003
**Raised by:** /use-case-implement, 2026-11-06
**Blocking:** No
**Status:** Resolved

BR-003 matches a word of the Search Term against the beginning of a word of the entry and treats a letter with
an accent or umlaut as the letter without it, but does not define what separates two words, nor whether letters
such as `ł`, `ø` or `ß`, which carry no separable accent, are folded. Readings: (a) every character that is
neither a letter nor a digit separates words, in the Search Term and in the entry alike (`YMCA/YWCA` and
`Cevi-Lager` are two words each); letters are folded by Unicode decomposition, plus `ł`→`l`, `ø`→`o`, `đ`→`d`;
`ß` stays `ß`; (b) only spaces separate words, so punctuation stays part of a word.

**Answer:** (a) — any character other than a letter or a digit separates words; decomposition plus `ł`, `ø`,
`đ`; `ß` is not folded.
**Answered by:** Patrick Uhlmann, 2026-11-06
**Resolved in:** UC-010 BR-003

## OQ-005: Are exchange offers completed, or are the `EXCHANGE` table and its display fragment dropped?

**Element:** FR-029
**Raised by:** /requirements, 2026-08-16
**Blocking:** No
**Status:** Resolved

FR-029 records exchange offers as present in the schema but used by no use case. Readings: (a) complete them with a use case of their own; (b) drop the table and the fragment.

**Answer:** (b) — Die Austauschangebote werden nicht umgesetzt: die Tabelle `EXCHANGE` und das Fragment
`tags/exchange.html` werden entfernt, FR-029 entfällt.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** FR-029, Glossary: Exchange Offer

## OQ-006: Do administrators manage accounts in the application, or does the operator do it at deployment level?

**Element:** FR-030
**Raised by:** /requirements, 2026-08-16
**Blocking:** No
**Status:** Resolved

The initial account comes from a deployment secret (UC-006 BR-008). Readings: (a) in-application account management for administrators; (b) accounts stay a deployment matter of the operator.

**Answer:** (b) — Benutzerkonten bleiben Sache des Betreibers auf Deployment-Ebene; es gibt keine
Kontoverwaltung in der Applikation, FR-030 entfällt.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** FR-030

## OQ-007: Are 1000 ms p95 and 50 concurrent visitors the right targets for the expected audience?

**Element:** NFR-001
**Raised by:** /requirements, 2026-08-16
**Blocking:** No
**Status:** Resolved

Asks about NFR-001 and NFR-002 together; both thresholds are marked as assumed in the catalog.

**Answer:** Wir können die Anforderungen entfernen. Die Applikation hat nur wenige User und die Response Zeit der
Seite ist wirklich nicht so wichtig.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** NFR-001, NFR-002

## OQ-008: Which availability, startup and backup expectations are binding?

**Element:** NFR-003
**Raised by:** /requirements, 2026-08-16
**Blocking:** No
**Status:** Resolved

Asks about NFR-003, NFR-004 and NFR-020 together; the platform has no monitoring and no documented backup today, so the assumed thresholds cannot be checked yet.

**Answer:** Einverstanden, NFR-003 und NFR-004 entfernen, NFR-020 behalten jedoch reicht es ein tägliches Backup zu
haben. Die Restorezeit muss nicht definiert werden.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** NFR-003, NFR-004, NFR-020

## OQ-009: Are 5 sign-in attempts and 1 contact submission per 10 seconds and source address the right limits?

**Element:** NFR-026
**Raised by:** /requirements, 2026-08-16
**Blocking:** No
**Status:** Resolved

NFR-026 states the rate limits as assumptions.

**Answer:** Bestätigt, die Limits passen so.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** NFR-026

## OQ-010: Which browsers must be supported, and down to which version?

**Element:** C-006
**Raised by:** /requirements, 2026-08-16
**Blocking:** No
**Status:** Resolved

C-006 names the browser support without a confirmed lower bound.

**Answer:** Aktuelle Evergreen-Browser reichen; keine feste Anzahl Versionen und keine Untergrenze pro
Browser.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** C-006

## OQ-011: Does the working group supply the start page's headline and lead copy, or is a proposal enough?

**Element:** FR-034
**Raised by:** /requirements, 2026-08-25
**Blocking:** No
**Status:** Resolved

FR-034 requires the start page to state in one screen what the platform is; the wording itself is not decided.

**Answer:** Der Vorschlag reicht; Titel und Lead der Startseite bleiben wie umgesetzt
(„Raus aus der Schweiz. Rein in die Welt.“). Die Fachgruppe muss keinen Text liefern.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** FR-034

## OQ-012: Is a 160-character excerpt in the list acceptable to the administrators who write the descriptions?

**Element:** FR-035
**Raised by:** /requirements, 2026-08-25
**Blocking:** No
**Status:** Resolved

FR-035 and UC-001 BR-004 ask for a short excerpt without naming a length; the implementation and UC-010 BR-008 use 160 characters, which the administrators have not confirmed.

**Answer:** 160 Zeichen für den Auszug in den Listen sind bestätigt.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** FR-035

## OQ-013: Is Lora accepted as body typeface, or does body copy use the Corporate Design's third face (Helvetica Neue / system sans)?

**Element:** NFR-044
**Raised by:** /requirements, 2026-08-25
**Blocking:** No
**Status:** Resolved

NFR-044 binds the Cevi Corporate Design; the body typeface is a proposal.

**Answer:** Lora bleibt die Schrift für Fliesstext; kein Wechsel auf Helvetica Neue / System-Sans.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** NFR-044

## OQ-014: Is the dark colour scheme part of this redesign or of a later one?

**Element:** NFR-045
**Raised by:** /requirements, 2026-08-25
**Blocking:** No
**Status:** Resolved

NFR-045 describes the dark scheme; its timing is not decided.

**Answer:** Ein dunkles Farbschema wird nicht benötigt, weder jetzt noch später; NFR-045 entfällt.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** NFR-045

## OQ-015: Is 8 KB of self-hosted script per public page the right ceiling, and are 300 ms p95 at 500 events and 200 offers the right search target?

**Element:** NFR-041
**Raised by:** /requirements, 2026-11-06
**Blocking:** No
**Status:** Resolved

Asks about NFR-041 and NFR-046 together; both thresholds were set on 2026-11-06 as proposals.

**Answer:** Eigentlich braucht es da kein Limit. Es ist wirklich nicht so wichtig. Auch die Zeit für die
Suchabfrage ist nicht so relevant. Wir haben so wenig Daten und User es wird kein Problem werden.
Ergänzung zu NFR-041: Das 24 KB CSS-Limit kann auch fallen und auch das 90 KB Limit für Web Fonts. Mit
Third-Party Libraries habe ich kein Problem, aber ich möchte soweit möglich vermeiden, solche extern zu
referenzieren/laden; wenn wir sie selber hosten können, kein Problem.
**Answered by:** Patrick Uhlmann, 2026-11-08
**Resolved in:** NFR-041, NFR-046, C-020, C-021

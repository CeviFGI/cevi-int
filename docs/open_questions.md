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
**Status:** Answered

Step 3 shows "the passage around the first occurrence of the Search Term" in the Description, but states neither
the length of the passage nor what "the first occurrence" is for a Search Term of several words, which BR-003
lets match in any order. Readings: (a) 160 characters, as the excerpt on the lists (FR-035), starting shortly
before the earliest occurrence of any word of the Search Term, with an ellipsis where text was cut, and no
highlighting; (b) the same, with the matching words highlighted; (c) the occurrence of the first word of the
Search Term as typed.

**Answer:** (a) — 160 characters around the earliest occurrence of any word of the Search Term, an ellipsis at
each cut end, no highlighting.
**Answered by:** Patrick Uhlmann, 2026-11-06

## OQ-004: Where does a word end, and which letters count as the same letter?

**Element:** UC-010 BR-003
**Raised by:** /use-case-implement, 2026-11-06
**Blocking:** No
**Status:** Answered

BR-003 matches a word of the Search Term against the beginning of a word of the entry and treats a letter with
an accent or umlaut as the letter without it, but does not define what separates two words, nor whether letters
such as `ł`, `ø` or `ß`, which carry no separable accent, are folded. Readings: (a) every character that is
neither a letter nor a digit separates words, in the Search Term and in the entry alike (`YMCA/YWCA` and
`Cevi-Lager` are two words each); letters are folded by Unicode decomposition, plus `ł`→`l`, `ø`→`o`, `đ`→`d`;
`ß` stays `ß`; (b) only spaces separate words, so punctuation stays part of a word.

**Answer:** (a) — any character other than a letter or a digit separates words; decomposition plus `ł`, `ø`,
`đ`; `ß` is not folded.
**Answered by:** Patrick Uhlmann, 2026-11-06

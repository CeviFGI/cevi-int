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

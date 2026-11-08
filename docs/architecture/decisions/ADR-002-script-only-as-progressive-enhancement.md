# ADR-002: Public pages use script only as self-hosted progressive enhancement

**Status:** Superseded
**Date:** 2026-11-08
**Requirements:** C-021, NFR-041, NFR-024, NFR-042, C-017, C-020, NFR-035, FR-040, FR-042
**Superseded by:** ADR-009

## Context

Every function of a public page must work without JavaScript (C-021): a visitor on a locked-down or
slow phone must lose nothing but convenience. Two functions cannot be offered without script,
though: the operating system's Share Dialog (Web Share API) and writing to the clipboard (FR-040),
and Live Results while typing (FR-042). On 2026-11-06 the product owner first admitted the share
script as a single exception of at most 2 KB on one page, and the same day replaced the collection
of exceptions by the rule both followed.

## Decision

Script may be added to a public page only to enhance a function that already works without it,
only as a self-hosted file from the page's own `/js/` folder without a third-party library or
framework, and with at most 8 KB of self-hosted script per public page in total (NFR-041). No page
carries inline script, so the Content-Security-Policy keeps `script-src 'self'` (NFR-024), and no
script is loaded from another host (NFR-042).

Sharing uses the Share Dialog wherever the browser offers it and copies the Public Address to the
clipboard otherwise; the choice follows only what the browser offers, never a guess about the
device. Without the script the page shows the Public Address as selectable text. The Event Page and
the Offer Page use the same share script.

## Consequences

- C-017, C-020 and NFR-035 stay meaningful: no framework or library reaches a visitor.
- 8 KB leaves room for the share script, the search script and one further small enhancement.
- `EditorAssetsTest`, `EventShareTest` and `VoluntaryShareTest` check that every public page carries
  only self-hosted, non-inline script within the budget.
- Every enhancement needs a working variant without script, which has to be built and tested too.
- **Alternatives considered:** prepared share links to single messengers, which work without script
  — declined because they serve only the channels they happen to list; a public site free of any
  script — given up by the product owner on 2026-11-06, because each new enhancement would have
  needed its own exception.

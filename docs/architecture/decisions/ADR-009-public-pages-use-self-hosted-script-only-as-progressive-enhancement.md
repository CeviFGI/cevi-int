# ADR-009: Public pages use self-hosted script only as progressive enhancement, third-party libraries included

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** C-021, NFR-024, NFR-042, C-017, NFR-030, NFR-035, FR-040, FR-042
**Supersedes:** ADR-002

## Context

Every function of a public page must work without JavaScript (C-021): a visitor on a locked-down or
slow phone must lose nothing but convenience. The operating system's Share Dialog and writing to the
clipboard (FR-040) and Live Results while typing (FR-042) cannot be offered without script. ADR-002
admitted only the project's own script, without a third-party library or framework and with at most
8 KB per public page. On 2026-11-08 the product owner dropped the budget and admitted third-party
libraries, as long as they are not loaded from another host (NFR-041 Rejected, OQ-015).

## Decision

Script may be added to a public page only to enhance a function that already works without it. It
is served from the application's own origin, and no script is loaded from another host (NFR-042). A
third-party library is allowed when it is resolved as a Maven dependency (a WebJar, C-017, NFR-030)
and kept current (NFR-035). There is no byte budget for script. No page carries inline script, so
the Content-Security-Policy keeps `script-src 'self'` (NFR-024).

Sharing uses the Share Dialog wherever the browser offers it and copies the Public Address to the
clipboard otherwise; the choice follows only what the browser offers, never a guess about the
device. Without the script the page shows the Public Address as selectable text. The Event Page and
the Offer Page use the same share script.

## Consequences

- An enhancement can build on an existing library instead of reimplementing it.
- Every library reaching a visitor is a dependency the dependency scanner reports and NFR-035 keeps
  current; the rich-text editor stays limited to the two administrator form pages (NFR-036, ADR-003).
- `EditorAssetsTest`, `EventShareTest` and `VoluntaryShareTest` keep checking that every public page
  carries only self-hosted, non-inline script, but no longer its size.
- Every enhancement needs a working variant without script, which has to be built and tested too.
- **Alternatives considered:** keeping the 8 KB budget and the ban on third-party libraries —
  dropped by the product owner; loading a library from a CDN — breaks NFR-042.

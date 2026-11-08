# ADR-010: Self-host the brand web fonts as files in the repository

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** NFR-044, NFR-042, NFR-030, NFR-035, C-018
**Supersedes:** ADR-004

## Context

NFR-044 requires Montserrat and Lora, and NFR-042 forbids loading them from Google Fonts or any
other host. That leaves the files in the repository, which at first glance reads like a breach of
NFR-030. NFR-030 exists so that a dependency scanner can report vulnerabilities in code the browser
executes; a `woff2` file is data, not code, and no scanner tracks it. A WebJar was checked:
`org.webjars.npm:fontsource__montserrat` last released in 2022 and would itself violate NFR-035,
and no Lora WebJar exists. ADR-004 took the same decision and measured it against a 90 KB font
budget, which the product owner dropped on 2026-11-08 (NFR-041 Rejected, OQ-015).

## Decision

The two typefaces are subset and placed in the repository as `woff2` files, served from the
application's own origin and split by `unicode-range`. NFR-030 applies to executable libraries
only, NFR-035 does not apply to static brand assets, and C-018 admits SIL OFL-1.1 for fonts. The
same reasoning covers `logo.png`. The font files have no byte budget.

## Consequences

- The Latin-Extended and italic faces are fetched only when a page renders such a character, so
  place names like Poděbrady stay correct without charging every visitor.
- `NoThirdPartyAssetsTest` checks that no page or stylesheet requests another host; `StaticAssetTest`
  no longer has to hold the font bytes to a budget.
- A new font release has to be subset and replaced by hand.
- **Alternatives considered:** Google Fonts — breaks NFR-042; a font WebJar — outdated or not
  available, breaks NFR-035.

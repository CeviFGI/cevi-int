# ADR-001: Build the interface on an own token-based design system, without a CSS framework

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** C-019, C-020, NFR-037, NFR-041, NFR-044, NFR-045

## Context

The platform's audience is 15–30 and arrives on a phone through a shared link; the feedback that
triggered the redesign of 2026-08-25 was that the site is unattractive. The Cevi Schweiz Corporate
Design Manual (2020) fixes logo, brand colours and typefaces (C-019, NFR-044). Every framework would
have to be overridden to reach them, adds a dependency subject to NFR-035, and costs more transferred
bytes than NFR-041 allows. The product owner decided this on 2026-08-25, together with a graphic
rather than photographic visual system and a new start page with the existing page structure
otherwise intact.

## Decision

The visual layer is the project's own CSS. `css/tokens.css` is the single file that declares
custom properties for colour, type size, spacing, radius, shadow and motion; every other stylesheet
uses those tokens and contains no literal colour or hard-coded font size (NFR-037). A dark scheme is
a redefinition of the tokens only (NFR-045). No image field is added to `EVENT` or
`VOLUNTARY_SERVICE`; the visual system is graphic.

The stylesheet bundle has a ceiling of 24 KB gzipped (NFR-041). It was 12 KB when estimated before
the design system existed, raised to 16 KB on the measured 13.8 KB, and to 24 KB after phase 4
measured 16.5 KB, because the product owner decided that the stylesheet's own size is not worth
optimising against at this scale. The ceiling still forbids a framework: Bootstrap alone is about
27 KB gzipped before a single override. The complete first view stays under 150 KB.

## Consequences

- One value is changed in one place; a component that needs a new value adds a token first.
- The thresholds are enforced by the build, not by review: `CssBudgetTest` (stylesheet bytes, a single
  `:root`, no literal colours), `ResponsiveLayoutE2ETest` and `TypographyFloorE2ETest` (NFR-013),
  `ContrastE2ETest`, `FocusVisibleE2ETest` and `NavigationDrawerE2ETest` (NFR-014, NFR-039),
  `TapTargetE2ETest` (NFR-038), `ReducedMotionE2ETest` (NFR-040). The judgement clauses of WCAG 2.1 AA
  and the 150 KB first view as a whole are not machine-checked.
- Every component has to be written and maintained by the project itself.
- **Alternatives considered:** a CSS framework (would have to be overridden to reach C-019 and breaks
  NFR-041); photographic imagery per record; a deeper restructuring of the navigation. All declined by
  the product owner in favour of the narrower option.

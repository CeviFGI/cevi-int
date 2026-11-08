# ADR-008: Build the interface on an own token-based design system, without a CSS framework and without a byte budget

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** C-019, C-020, NFR-037, NFR-044
**Supersedes:** ADR-001

## Context

The Cevi Schweiz Corporate Design Manual (2020) fixes logo, brand colours and typefaces (C-019,
NFR-044). Every CSS framework would have to be overridden to reach them and adds a dependency
subject to NFR-035 (C-020). ADR-001 additionally capped the stylesheet bundle at 24 KB gzipped and
provided for a dark scheme as a redefinition of the tokens. On 2026-11-08 the product owner dropped
both (NFR-041 and NFR-045 Rejected, OQ-014, OQ-015): with few visitors and little data, the size of
the transferred assets is not a target, and a dark scheme is not wanted.

## Decision

The visual layer is the project's own CSS. `css/tokens.css` is the single file that declares
custom properties for colour, type size, spacing, radius, shadow and motion; every other stylesheet
uses those tokens and contains no literal colour or hard-coded font size (NFR-037). The stylesheet
bundle has no byte ceiling. No dark scheme is offered. No image field is added to `EVENT` or
`VOLUNTARY_SERVICE`; the visual system is graphic.

## Consequences

- One value is changed in one place; a component that needs a new value adds a token first.
- `CssBudgetTest` keeps checking a single `:root` and the absence of literal colours, but no longer
  the stylesheet's size. `ResponsiveLayoutE2ETest`, `TypographyFloorE2ETest`, `ContrastE2ETest`,
  `FocusVisibleE2ETest`, `NavigationDrawerE2ETest`, `TapTargetE2ETest` and `ReducedMotionE2ETest`
  keep enforcing NFR-013, NFR-014 and NFR-038 … NFR-040.
- Without the byte ceiling, only C-020 keeps a CSS framework out.
- Every component has to be written and maintained by the project itself.
- **Alternatives considered:** keeping the 24 KB ceiling — dropped by the product owner; a CSS
  framework — would have to be overridden to reach C-019 and breaks C-020.

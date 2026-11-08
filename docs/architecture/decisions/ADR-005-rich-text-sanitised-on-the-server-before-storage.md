# ADR-005: Sanitise rich text on the server before storage

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** NFR-010, NFR-011

## Context

Descriptions are written in a rich-text editor and rendered unescaped on the public pages. Whatever
the editor in the browser allows is a convenience for the administrator, not a boundary: a
cross-site request (NFR-011) lets an unauthenticated attacker reach the administrator-only write
path, so the submitted HTML cannot be trusted.

## Decision

The resource reduces every submitted Description to the documented allow-list of tags, attributes
and URL schemes (`HtmlSanitizer.sanitize(...)`, allow-list in `docs/entity_model.md`) before it is
stored. The stored value, not the submitted one, is the authority; the length limit applies after
the reduction.

## Consequences

- A stored Description can be rendered unescaped without further checks.
- The editor's toolbar has to match the allow-list (ADR-003); formatting outside it is discarded.
- **Alternatives considered:** sanitising when rendering — every template rendering a Description
  would have to remember it, and a stored payload would stay dangerous.

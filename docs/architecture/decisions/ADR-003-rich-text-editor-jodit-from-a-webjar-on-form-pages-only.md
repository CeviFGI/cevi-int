# ADR-003: Use Jodit from a WebJar as rich-text editor, loaded on the two form pages only

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** NFR-035, NFR-036, NFR-030, C-017, C-018, C-009, FR-033

## Context

The previous editor, Summernote 0.9.1, was the newest release and dated from October 2024; a jQuery 4
incompatibility reported against it in February 2026 stayed open. It could not satisfy NFR-035 and
had to be replaced by a maintained editor. The formatting it offered (FR-033) is the acceptance
criterion for the replacement: an editor that drops tables, colours or the source view would
silently reduce what administrators can publish, and offering a button whose result the sanitiser
discards (NFR-010) is a defect. The editor used to be loaded from `base.qute.html` on every page,
although only two administrator pages use it, and a maintained editor is larger than the old one.

## Decision

The rich-text editor is Jodit (MIT, dependency-free), resolved as a WebJar Maven dependency and
served under version-less `/webjars/...` URLs (NFR-030, C-017). It is loaded by the two event and
offer form templates only, never from `base.qute.html`, so no public page carries it (NFR-036).
Its toolbar offers exactly the formatting that the stored-value allow-list accepts.

## Consequences

- jQuery is removed from the application.
- Public pages stay faster than before the exchange instead of slower.
- `EditorAssetsTest` guards that public pages do not load the editor.
- **Alternatives considered:** CKEditor 5 and TipTap — distributed as ES modules meant for a bundler,
  excluded by C-017; TinyMCE — GPLv2+ since version 7 and refuses to start self-hosted unless the
  application declares GPL use, excluded by C-018 (which is narrower than C-009, which only demands
  that a component be free of charge); keeping Summernote — fails NFR-035.

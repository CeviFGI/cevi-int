# Rich-Text Editor Evaluation

**Date:** 2026-08-24 · **Verdict:** Summernote replaced by **Jodit 4** (`org.webjars.npm:jodit`).

Summernote's last release, 0.9.1, is from October 2024. Its repository still receives commits, but
the jQuery 4 incompatibility reported in February 2026 ([summernote#4787]) is open with no fix, no
pull request and no assignee, and the "drop jQuery" issue ([summernote#4505]) has been open for
years. That fails NFR-035: a frontend library delivered to the browser must have an upstream release
not older than 12 months, so that a reported vulnerability can be answered by upgrading rather than
by patching a dead component locally.

---

## 1. What the replacement had to satisfy

| # | Requirement | Source |
|---|---|---|
| 1 | Upstream release not older than 12 months | NFR-035 |
| 2 | Every button in FR-033 — headings, emphasis, superscript/subscript, font family and size, colour, lists, alignment, line height, tables, links, inline pictures, source view | FR-033 |
| 3 | Prebuilt browser bundle resolvable as a Maven dependency; no npm, Node or bundler | C-017, NFR-030 |
| 4 | Permissive licence (MIT, BSD, Apache-2.0) | C-018 |
| 5 | Runs under `script-src 'self'` — no inline script, no `eval`, no `new Function` | NFR-024 |
| 6 | Output survives the server-side allow-list | NFR-010, `docs/entity_model.md` |

## 2. Candidates

| Editor | Licence | Latest release | Verdict |
|---|---|---|---|
| **Jodit 4** | MIT | 4.13.23, 2026-08-18 (weekly cadence) | **Chosen.** No dependencies, single JS + CSS file, full feature parity, no `eval`/`new Function` in the bundle. |
| Summernote 0.9.1 | MIT | 0.9.1, 2024-10-09 | Fails requirement 1. Needs jQuery, which the jQuery 4 upgrade then blocks (`docs/jquery4_evaluation.md`). |
| TinyMCE 8 | **GPLv2+** | 8.8.2, 2026 | Fails requirement 4. Since v7 self-hosting requires `license_key: 'gpl'` — without it the editor disables itself — which would put copyleft obligations on what this site ships. |
| CKEditor 5 | GPLv2+ / commercial | 48.4.0, 2026-08-05 | Fails requirements 3 and 4: distributed as ES modules for a bundler, and free use requires declaring the GPL licence key. |
| TipTap 3 | MIT | 3.30.3, 2026-08-24 | Fails requirement 3 — headless, assembled at build time from npm packages. |
| Quill 2 | BSD-3 | 2.0.3, 2024-11-30; last commit 2025-07 | Fails requirements 1 and 2 (no table UI). Swapping one quiet editor for another gains nothing. |
| Trix 2 | MIT | 2.1.19, 2026-05-09 | Passes 1, 3, 4, 5 and is by far the smallest, but fails requirement 2: no tables, colours, font families or source view. Only an option if FR-033 is deliberately reduced. |
| SunEditor 3 | MIT | 3.3.0, 2026-08-06 | Passes on paper, but 3.x is a fresh rewrite with a much smaller user base than Jodit and no larger feature set. Kept as the fallback. |
| Editor.js | Apache-2.0 | 2.31.6, 2026-04-07 | Stores blocks as JSON, not HTML — would change the storage format of both descriptions and the entire sanitiser boundary. |
| Toast UI Editor | MIT | 3.2.2, 2023-02 | Unmaintained; fails requirement 1. |

## 3. Transfer size

Measured on the minified builds, gzipped:

| | raw | gzipped |
|---|---|---|
| jQuery 3.7.1 slim + Summernote lite (JS + CSS), today | 278 KB | 69 KB |
| Jodit 4 (`es2021/jodit.min.js` + `jodit.min.css`) | 858 KB | 251 KB |

Jodit is the heavier component — but the old stack was loaded from `base.qute.html`, so **every
visitor** downloaded 69 KB of editor for pages that have nothing to edit. The editor now loads from
the two administrator form templates only (NFR-036, enforced by `EditorAssetsTest`), so:

- public pages: **69 KB → 0 KB**,
- the two form pages, reachable only with an administrator session: 69 KB → 251 KB.

The `es2021` build is used rather than `es2021.en` because it carries the German locale
(`language: 'de'`).

## 4. What changed

| File | Change |
|---|---|
| `pom.xml` | `org.webjars.npm:jquery` and `:summernote` → `org.webjars.npm:jodit:4.12.29` |
| `templates/base.qute.html` | editor assets removed; `{#insert head}` and `{#insert scripts}` added |
| `templates/{Event,Voluntary}Resource/form.html` | fill those two blocks; textarea id `summernote` → `description-editor` |
| `META-INF/resources/js/summernote-init.js` | replaced by `editor-init.js` (`Jodit.make`, German, toolbar per FR-033) |
| `infra/EditorAssetsTest` | new — asserts NFR-036 in both directions |
| `e2e/VoluntaryFormE2ETest` | `.note-editable` → `.jodit-wysiwyg` |

`HtmlSanitizer` and the allow-list in `docs/entity_model.md` are unchanged: Jodit writes colours and
font sizes as inline `style` attributes and tables as ordinary `table` markup, both of which the
policy already accepts. `video` and `file` are left out of the toolbar for the same reason `video`
was left out under Summernote — the sanitiser drops the resulting iframe.

## 5. Verification

`tooling/docker.sh verify` on a fresh `/tmp/international.db`: **84 unit tests and 6 e2e tests pass,
the 80 % coverage gate holds**. The e2e test types into the editor of the real browser and asserts
the stored value; the run recorded `<p>Freiwilligenarbeit in Bern</p>` for the created offer, so the
value reaches the server through the hidden textarea. `editor-init.js` and `EditorAssetsTest` were
checked with the Sonar snippet analyser — no issues.

[summernote#4787]: https://github.com/summernote/summernote/issues/4787
[summernote#4505]: https://github.com/summernote/summernote/issues/4505

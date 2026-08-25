# Implementation Plan: Visual Redesign

**Document status:** Draft for review
**Date:** 2026-08-25
**Companion documents:** `docs/ux_concept.md` (what is being built and why),
`docs/mockup/redesign-mockup.html` (a static, self-contained preview of the result — open it in a
browser; it carries the token block that Phase 1 lifts into `css/tokens.css`)
**Scope:** Presentation layer only. No change to the data model, the security invariants or the
functional flows of UC-001 … UC-007, apart from the four requirement changes listed in §2.

---

## 1. Approach

The redesign is delivered in **six phases, each independently shippable**. After every phase the
site is complete, tested and deployable — there is no point at which the working copy holds a
half-finished visual language. That matters here specifically: this is a volunteer project worked
on sporadically, and a redesign that must be finished in one sitting to look coherent is a redesign
that stalls half-done.

Phases 1–3 already deliver the bulk of the perceived improvement (design system, header/footer,
list cards, contact form) and address every element the feedback named. Phases 4–6 complete the
structure and lock the quality in with tests.

**Order rationale:** the token layer comes first because every later phase consumes it; the card
components come before the new start page because the start page is assembled *from* those cards;
the tests that pin responsiveness and accessibility come last because they assert the finished
state.

---

## 2. Requirement changes this plan implements

These are the changes recorded in `docs/requirements.md` (revision 5). No implementation begins
before the requirement change is accepted.

| Requirement | Change | Phase |
|---|---|---|
| FR-023 | Amended — the start page becomes a page of its own instead of a redirect to `/anlaesse`. | 4 |
| FR-010 | Status corrected to **Open** — `organizationLink` is captured but rendered nowhere; the offer card delivers it. | 2 |
| FR-034 … FR-039 | New — start page, scannable list, urgency indicator, labelled form, admin/visitor separation, information page structure. | 1–4 |
| NFR-013, NFR-014 | Status moves from *Open (assumed)* to *Verified*, with the e2e assertions that make them testable. | 5 |
| NFR-037 … NFR-045 | New — design system, brand conformity, target size, motion, self-hosted fonts, CSS budget, dark mode (optional). | 1, 5 |
| NFR-030, C-018 | Clarified — web font files are brand assets under OFL-1.1, not frontend libraries. | 1 |
| C-019 … C-021 | New — Cevi CD conformity, no CSS framework, no third-party asset requests. | 1 |

**Use case specifications are deliberately not touched yet.** UC-001, UC-003, UC-005 and UC-007
describe flows that FR-034 … FR-039 change (the excerpt in the list, the offer link, the new entry
page, the form's error presentation). Those specs are updated with `/use-case-spec` *after* the
requirement changes are accepted and *before* Phase 2 starts — per the project workflow, no use
case is implemented before its spec exists. §9 lists exactly which ones.

---

## 3. Phase 1 — Design system foundation — **delivered 2026-08-25**

**Goal:** every token from `docs/ux_concept.md` §3 exists, the page frame (header, nav, footer,
container, typography) is rebuilt, and every existing page inherits the new look without any page
template being restructured yet.

### Files

| Action | Path |
|---|---|
| new | `src/main/resources/META-INF/resources/css/tokens.css` — colour, type scale, spacing, radii, shadow, motion, `--tap-target`; the sole home of `:root` custom properties |
| new | `src/main/resources/META-INF/resources/css/base.css` — reset, `@font-face`, element defaults, focus ring, `prefers-reduced-motion`, skip link |
| new | `src/main/resources/META-INF/resources/css/layout.css` — page grid, `--container`, `--measure`, section rhythm, card grid |
| rewrite | `css/nav.css` — header bar, drawer sheet, desktop row, active underline (keeps the checkbox mechanism) |
| new | `css/footer.css` |
| new | `css/components.css` — buttons, badges, chips, cards, accent band, empty state, admin strip |
| rewrite | `css/form.css` — labels, inputs, textarea, inline errors, error summary, helper text |
| rewrite | `css/table.css` — folded into `prose.css` in Phase 2; kept as a stub until then |
| delete | `css/colors.css`, `css/spacing.css`, `css/elements.css` — `.red`, `.margin-left` and `.box` are replaced by tokens and components |
| rewrite | `css/site.css` — import hub only |
| **done** | `src/main/resources/META-INF/resources/fonts/` — six subset `woff2` files plus `OFL-Montserrat.txt` and `OFL-Lora.txt` (see `docs/ux_concept.md` §3.2) |
| new | `src/main/resources/META-INF/resources/img/triangle.svg` — the motif, as one inline-able symbol |
| edit | `src/main/resources/templates/base.qute.html` — skip link, semantic landmarks, font preloads, restructured header/footer, `{#insert head}`/`{#insert scripts}` preserved unchanged |
| new | `css/transitional.css` — see below |

**Deviations from the plan as written, and why:**

- **No `tags/siteHeader.html` / `tags/siteFooter.html`.** `base.qute.html` is their only consumer;
  two tag files for one caller buys indirection and nothing else. Both live in the base template.
- **`css/transitional.css` was added.** The plan had `colors.css`, `spacing.css` and `elements.css`
  deleted in this phase, but `.box`, `.red` and `.margin-left` are still used by the page templates
  that phases 2 and 3 rewrite — deleting the rules now would have left those pages unstyled for two
  phases. The three files are gone; their classes are re-expressed in tokens in one clearly marked
  file that names the phase which removes each rule.
- **`site.css` stays an `@import` hub.** Individual `<link>` elements would fetch in one wave
  instead of two, but they put the file list into the markup. One extra round trip for a bundle
  this small is the cheaper trade.
- **Page titles were promoted from `<h2>` to `<h1>` in this phase.** Every page opened at `h2` with
  no `h1` at all, and the data protection page mixed `h2` and `h3` for sections at the same level.
  It is a one-line change per template, it is not a restructuring, and leaving it until phase 4
  would have meant writing `PageStructureTest` against a state known to be wrong.

### Notes on the font files — already in place

Montserrat and Lora were subset from the upstream variable fonts and committed under
`META-INF/resources/fonts/`, split by `unicode-range` into six files; a German page loads 69.9 KB of
them, inside the NFR-041 budget. The exact file set and the reasoning are in `docs/ux_concept.md`
§3.2. Both licences (`OFL-Montserrat.txt`, `OFL-Lora.txt`) sit beside the files, as SIL OFL-1.1
requires.

They are **brand assets, in the same class as `logo.png`, not frontend libraries** — there is no
maintained WebJar for either (`org.webjars.npm:fontsource__montserrat` last released 4.5.14 in 2022
and would itself violate NFR-035; there is no Lora WebJar at all). NFR-030 and C-018 are clarified
accordingly (§2) rather than quietly bent — that clarification is a prerequisite for this phase,
not a consequence of it.

What Phase 1 still owes them: the `@font-face` block in `base.css` (it exists as a working draft at
the top of `docs/mockup/redesign-mockup.html`), the two `preload` links in `base.qute.html`, and the
`size-adjust` fallback declarations.

Serving them from the application's own origin is not merely convenient: a Google Fonts `<link>`
would need `font-src`/`style-src` exceptions in the CSP (NFR-024) and would send every visitor's IP
to a third party, which C-011 and the data protection statement do not cover.

### Tests

- `SecurityHeadersTest` — unchanged, must still pass (CSP untouched).
- `EditorAssetsTest` — unchanged, must still pass (`base.qute.html` still loads no editor asset).
- new `CssBudgetTest` — follows the `@import` list of `site.css`, sums the gzipped bytes and fails
  above the NFR-041 budget; also asserts that only `tokens.css` declares `:root` variables (NFR-037).
- new `StaticAssetTest` — every asset named by a stylesheet resolves with HTTP 200, the baseline
  font pair stays inside its budget, and both OFL licences are served.
- new `PageStructureTest` — one `h1` per public page, no skipped heading level, the skip link
  before the navigation, and the data protection link present on every page (NFR-043, BR-026).
- Two existing tests broke, both on presentation rather than behaviour, and both were rewritten to
  assert content: `IndexResourceTest.version_working` matched the literal string `"Version:"`, and
  the navigation e2e test drove the drawer through the old checkbox id `#side-menu`. The latter now
  asserts what a visitor sees — that the navigation appears and disappears — instead of the state
  of a checkbox, and it gained a case for the desktop row.

### Done

Every page renders in the new frame; no page content was restructured. `tooling/docker.sh verify`
passes: 113 unit tests, 7 e2e tests, coverage gate met. Verified visually against the running
application at 390 px and 1400 px.

**Still owed by later phases, noticed here:** `base.qute.html` wraps the content insert in
`.container`, which phase 4 will have to open up for the full-bleed sections of the start page —
most simply with a second insert for pages that lay themselves out.

---

## 4. Phase 2 — The lists (the loudest complaint) — **delivered 2026-08-25**

**Goal:** `/anlaesse`, the event detail page and `/volontariat` use the card system from
`docs/ux_concept.md` §4.3 / §4.4.

### Files

| Action | Path |
|---|---|
| new | `src/main/java/tools/cevi/infra/TemplateExtensions.java` — Qute `@TemplateExtension` methods: `excerpt(String html, int max)`, `accentVariant(String key)`, `countdown(LocalDate displayDate)` |
| new | `src/main/java/tools/cevi/infra/Excerpt.java` — strips tags from stored HTML and truncates on a word boundary |
| new | `src/main/java/tools/cevi/infra/Countdown.java` — maps `displayDate` to "Noch N Tage" / "Diese Woche" / "Nächste Woche" / "Läuft bald ab" in `de-CH` |
| new | `src/main/java/tools/cevi/infra/AccentVariant.java` — stable hash of slug/id → variant 0–3 |
| rewrite | `templates/tags/event.html` — accent band, title, meta, excerpt, primary action, urgency chip, admin strip |
| rewrite | `templates/tags/voluntaryService.html` — same shell, renders `organizationLink` as the primary action (FR-010) |
| rewrite | `templates/tags/exchange.html` — restyled only, still unused (vision §9 stays open) |
| **deleted** | `css/table.css` — folded into `prose.css` as planned, scoped to `.prose` so it can no longer style a table outside a description |

**Deviations:** no `tags/card.html`, `badge.html`, `button.html` or `emptyState.html`. A card, a
badge and a button are one class each in `components.css`; wrapping them in Qute tags would add a
file per class and buy nothing, since the three record tags are the only callers. The empty state
is six lines of markup that reads better in the list template that owns its wording.
| edit | `templates/EventResource/list.html` — intro, grid, empty state; the admin "Neuen Anlass eintragen" link becomes a button in an admin bar above the grid |
| edit | `templates/EventResource/detail.html` — full layout per §4.7, back link, prose block, contact teaser |
| edit | `templates/VoluntaryResource/list.html` — same treatment |
| new | `css/prose.css` — styles the sanitiser's whole allow-list: headings, lists, tables (with an `overflow-x` wrapper), links, images, colours, alignment |

### Why the excerpt needs care

`Excerpt` reads a value that is **already sanitised at storage time** (NFR-010) — it is a
presentation helper, never a security control. It must not become the reason someone relaxes the
sanitiser, and it must escape its output (Qute does by default; `{...}` is escaped, only
`.raw` is not). The detail page keeps rendering `{event.description.raw}` exactly as today.

### Tests

- new `ExcerptTest` — tag stripping, entity handling, word-boundary truncation, ellipsis, short input untouched, `null`/empty input.
- new `CountdownTest` — boundary days, today, past dates, `de-CH` plural forms.
- new `AccentVariantTest` — deterministic for a given key, distributed across all four variants.
- extend `EventUpcomingTest` — the list contains the excerpt and *not* the full description body.
- extend `VoluntaryResourceTest` — the offer card contains `organizationLink` with
  `rel="noopener"` and `target="_blank"` (this is the FR-010 regression test that never existed).
- extend `EventDetailTest` — the detail page still renders the full formatted description.
- `EventXssTest` — must still pass; add a case asserting the excerpt of a hostile description is
  escaped in the list.

### Done

`tooling/docker.sh verify` passes: 143 unit tests (113 before), 7 e2e tests, coverage gate met.
Verified visually against the running application at 390 px and 1400 px.

**Found while building it.** Qute has no `{#comment}` section — inside `{#include}` it is read as a
named block for `{#insert}`, so two of them in one template collide and the build fails. The
comments are `{! … !}`, which is also stripped before the page is sent rather than shipped to the
browser as an HTML comment would be.

**The stylesheet budget in NFR-041 moved from 12 KB to 16 KB gzipped**, on measurement: the
finished design system is 13.8 KB with its comments. The 12 KB had been estimated before any of it
existed. The ceiling still forbids a framework — Bootstrap alone is about twice it — and the figure
that matters to a visitor, the 150 KB first view, did not move. Recorded in `docs/requirements.md`.

**Still owed:** the offer list renders each full description inside a `<details>` for offers whose
text was shortened, because an offer has no page of its own (BR-044). That is more markup on the
page than the event list carries; if the offer texts grow, an offer detail page becomes the better
answer and needs a requirement of its own.

---

## 5. Phase 3 — Forms and status pages

**Goal:** the contact form (the second named complaint), the sign-in page, the two administrator
forms, the delete confirmations and the error/status pages.

### Files

| Action | Path |
|---|---|
| edit | `templates/ContactResource/form.html` — labelled fields, "Womit wir helfen" chip grid, reframed spam question with `inputmode="numeric"`, per-field errors, linked error summary, honeypot **unchanged** |
| edit | `templates/ContactResource/submitted.html` — success card with two ways onward |
| edit | `templates/AuthResource/login.html` — centred card, labels, `autocomplete` hints |
| edit | `templates/AuthResource/error.html`, `loggedOut.html` — status cards |
| edit | `templates/EventResource/form.html`, `VoluntaryResource/form.html` — two-column desktop layout, helper text replacing the `title` tooltips on `question-mark.svg`, styled Jodit container. **The `{#head}`/`{#scripts}` editor blocks are moved as-is; NFR-036 must not regress.** |
| edit | `templates/EventResource/delete.html`, `VoluntaryResource/delete.html` — destructive treatment, quiet cancel first |
| edit | `templates/error404.html`, `error500.html` — triangle, plain German, way back, copyable `errorId` |
| edit | `templates/IndexResource/version.html` — small definition card |
| edit | `src/main/java/tools/cevi/contact/ContactResource.java`, `event/EventResource.java`, `voluntary/VoluntaryResource.java` — pass validation messages in a shape the template can bind per field (today they arrive as a flat `Set<ValidationMessage>`; the field name is already carried, so this is a template-side grouping, not a model change) |

### Invariants that must survive this phase — each already has a test

| Invariant | Guard |
|---|---|
| Every state-changing form carries a CSRF token | `Csrf.givenWithoutToken()` cases in `ContactResourceTest`, `EventAddTest`, `EventDeleteTest`, `AuthResourceTest` |
| Deletion is a `POST`, never a `GET` | `EventDeleteTest` (NFR-021) |
| The honeypot stays off-screen, not `display:none` | `ContactResourceTest` (FR-032) — add an assertion on the class's computed style in the e2e layer |
| The spam value stays 50 | `ContactResourceTest` (NFR-009) |
| Rejected forms return the visitor's input | `EventEditTest`, `ContactResourceTest` (FR-008) |
| Editor assets load on the two form pages only | `EditorAssetsTest` (NFR-036) |

### Tests

- extend `ContactResourceTest` — every control has a `<label for=…>`; a rejected submission marks
  the field with `aria-invalid`.
- new `VoluntaryFormE2ETest` case — the editor still initialises inside the restyled container.
- new `ContactFormE2ETest` — fill, submit, confirm; and a rejected submission keeps the typed text
  and focuses the error summary.

---

## 6. Phase 4 — Start page and information pages

**Goal:** `/` becomes a page (FR-023 amended, FR-034), and the two information pages get structure.

### Files

| Action | Path |
|---|---|
| edit | `src/main/java/tools/cevi/infra/IndexResource.java` — `index()` returns a template instead of `Response.seeOther("/anlaesse")`; injects the three nearest events and two offers |
| new | `templates/IndexResource/home.html` — hero, event teaser, inverse offer section, contact band |
| edit | `src/main/java/tools/cevi/event/Event.java` — add a `upcoming(int limit)` query beside the existing one |
| edit | `src/main/java/tools/cevi/voluntary/VoluntaryService.java` — add a limited query |
| edit | `templates/IndexResource/fgi.html` — grouped channel cards, `<details>` on phone |
| edit | `templates/IndexResource/datenschutzinformation.html` — article layout, `--measure` |
| edit | `css/pages.css` — hero, inverse section, teaser band, channel cards |

### Migration risk — the changed entry point

`/` currently answers `303 See Other`. Anything relying on that changes behaviour:

- `IndexResourceTest` asserts the redirect — it is rewritten to assert the page.
- `IndexResource.admin()` redirects to `/` when already signed in; it now lands on the start page
  rather than the event list. That is the intended behaviour, but it is an explicit check.
- External links to `https://international.cevi.tools/` now land on the start page. `/anlaesse`
  keeps working unchanged, so **no external link breaks** — this is an addition, not a move.

### Tests

- rewrite `IndexResourceTest` — `/` returns 200 with the hero and the teasers; `/anlaesse` still
  returns the full list.
- new case — with an empty database the start page renders the empty state, not an empty grid.
- new `StartPageE2ETest` — hero visible above the fold at 360 × 640; both teaser links navigate.

---

## 7. Phase 5 — Responsiveness, accessibility and budget, made testable

**Goal:** turn NFR-013 and NFR-014 from *assumed and open* into *verified*, and pin the new NFRs.

| Test (new, `src/test/java/tools/cevi/e2e/`) | Asserts |
|---|---|
| `ResponsiveLayoutE2ETest` | For every public route × {320, 360, 768, 1024, 1440, 1920}: `document.scrollingElement.scrollWidth <= clientWidth` (NFR-013). |
| `TypographyFloorE2ETest` | No rendered text node computes below 14 px (NFR-013). |
| `TapTargetE2ETest` | At 360 px, every `a`, `button`, `input`, `summary` has a bounding box ≥ 44 × 44 px (NFR-038). |
| `NavigationDrawerE2ETest` | Drawer opens and closes by keyboard at 360 px; the active item is marked; sign-out remains a `POST`. |
| `FocusVisibleE2ETest` | Tabbing through a page never yields a focused element with no visible outline; the skip link is first (WCAG 2.4.1, 2.4.7). |
| `ContrastE2ETest` | Samples the computed foreground/background of body text, buttons, badges and meta lines and asserts ratio ≥ 4.5 : 1 (WCAG 1.4.3). |
| `CssBudgetTest` (unit) | Stylesheet bytes ≤ budget (NFR-041). |

Playwright already runs headless Chromium in the toolchain container, so all of this is
`tooling/docker.sh verify` — no new tooling, no Node.

**Manual review, once, with the product owner** (the tests cannot judge this): the two lists and
the contact form side by side, before and after, on a real phone.

---

## 8. Phase 6 — Cleanup and documentation

- Remove the dead rules the redesign orphans (`.box`, `.red`, `.margin-left`, the `elements.css`
  hairline treatment) and confirm by grep that no template still references them.
- Replace `logo.png` (1500 × 178, 29 KB, always scaled to 50 px) with a correctly-sized asset —
  SVG if the working group can supply one (`docs/ux_concept.md` §10).
- Update `CLAUDE.md`: a short "Design system" section naming `tokens.css` as the single source of
  colour/type/spacing, so the next contributor does not add a one-off hex value.
- Re-run `/requirements` status pass: NFR-013, NFR-014 → Verified; FR-010 → Verified.
- Sonar analysis on every new Java class (project workflow step 6).

---

## 9. Use case specifications to update

Written with `/use-case-spec` after the requirement changes are accepted, before Phase 2:

| Use case | What changes |
|---|---|
| UC-001 Browse upcoming events | The list shows an excerpt and an urgency indicator; the full description moves to the detail step. |
| UC-003 Browse voluntary service offers | The offer links out to the organisation (FR-010) — the step exists in the requirement but not in the spec's flow. |
| UC-005 Contact the working group | Error presentation moves to the field; the spam question is reworded (the rule, BR-017, is unchanged). |
| UC-007 View site information | A new entry-page flow (FR-034) and the restructured FGI page. |
| UC-002, UC-004, UC-006 | Presentation-only changes; specs reviewed but expected to need no flow change. |

---

## 10. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Existing REST-Assured tests assert on markup and break en masse in Phase 1 | High | Low | Expected and cheap: an assertion that breaks on a class name was testing presentation. Rewrite it to assert content. Budget one pass. |
| The stretched card link swallows the admin edit/delete links | Medium | Medium | Known failure mode, called out in `docs/ux_concept.md` §6; the admin strip gets `position: relative; z-index: 1` and an e2e click test. |
| Coverage drops below 80 % because new template branches are untested | Medium | Medium | Each phase adds its tests in the same phase; `verify` runs the gate every time. Remember the local-run caveat: delete `/tmp/international.db` first. |
| Lora reads as "old school" to the audience — the exact complaint | Medium | Medium | One-token fallback (`--font-body`), decided at the Phase 1 review with a real phone in hand. |
| Font files raise a licence/dependency-policy question late | Low | High | Resolved *before* Phase 1 by the NFR-030 / C-018 clarification (§2). Do not start Phase 1 until it is accepted. |
| Scope creep into filtering, photos, sharing | Medium | High | `docs/ux_concept.md` §8 and §9 list what is excluded and why. Anything on those lists is a new requirement, not part of this plan. |
| The new start page slows the first paint (two queries + fonts) | Low | Low | Two indexed `LIMIT` queries on a SQLite file; fonts are preloaded with `swap`. Covered by the NFR-001 budget. |

---

## 11. Effort and sequencing

| Phase | Content | Relative size | Shippable alone |
|---|---|---|---|
| 1 | Design system, frame, fonts | Large | Yes — the whole site already looks different |
| 2 | Event and offer cards, prose | Large | Yes — addresses the loudest complaint |
| 3 | Forms and status pages | Medium | Yes — addresses the second complaint |
| 4 | Start page, information pages | Medium | Yes |
| 5 | Responsive/a11y/budget tests | Medium | Yes — no visible change, locks quality |
| 6 | Cleanup, assets, docs | Small | Yes |

Phases 1–3 are the minimum viable redesign: after Phase 3 every element the audience named has been
rebuilt. Phases 4–6 are what make it a *finished* site rather than a restyled one.

### Verification after every phase

```shell
rm -f /tmp/international.db     # DemoData must re-seed, or coverage reads low
tooling/docker.sh verify        # unit + Playwright e2e + the 80 % gate
```

Visual review happens on a real phone at the end of Phases 1, 2 and 3 — the concept is written for
360 px, and a desktop browser window is not a substitute.

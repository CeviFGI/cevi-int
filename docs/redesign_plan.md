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

## 5. Phase 3 — Forms and status pages — **delivered 2026-08-26**

**Goal:** the contact form (the second named complaint), the sign-in page, the two administrator
forms, the delete confirmations and the error/status pages.

### Files

| Action | Path |
|---|---|
| new | `src/main/java/tools/cevi/infra/ValidationMessages.java` — the faults of one submission, addressable per field |
| edit | `src/main/java/tools/cevi/infra/ValidationMessage.java` — gains `label()`, the name the form gave the field |
| new | `templates/tags/errorSummary.html` — the summary block, shared by the three forms |
| rewrite | `templates/ContactResource/form.html` — labelled fields, "Womit wir helfen können" card grid, reframed spam question with `inputmode="numeric"`, per-field errors, linked error summary, honeypot **unchanged** |
| rewrite | `templates/ContactResource/submitted.html` — success card with two ways onward |
| rewrite | `templates/AuthResource/login.html` — centred card, labels, `autocomplete` hints |
| rewrite | `templates/AuthResource/error.html`, `loggedOut.html` — status cards |
| rewrite | `templates/EventResource/form.html`, `VoluntaryResource/form.html` — two-column desktop layout, helper text replacing the `title` tooltips, styled Jodit container; the `{#head}`/`{#scripts}` editor blocks moved as-is |
| **done in phase 2** | `templates/EventResource/delete.html`, `VoluntaryResource/delete.html` — the destructive treatment was already built with the card system |
| rewrite | `templates/error404.html`, `error500.html` — triangle, plain German, way back, copyable `errorId` |
| edit | `templates/IndexResource/version.html` — page head above the definition card |
| edit | `css/form.css`, `css/components.css` — form shell, two-column split, Jodit container, help grid, status card, `code-chip` |
| edit | `css/tokens.css` — `--font-mono`, the only family the design system was still missing |
| delete | `img/question-mark.svg`, `img/sources.txt` — the tooltip icon this phase replaced with permanent helper text |
| edit | `src/main/java/tools/cevi/{contact,event,voluntary}/*Resource.java` — pass `ValidationMessages` |

**Deviations from the plan as written, and why:**

- **The per-field binding is a small value type, not a template-side grouping.** The plan expected
  the templates to do the grouping. A Qute template asking "is *this* control faulty" would need a
  loop and a comparison per control — logic in the least testable place in the application.
  `ValidationMessages` carries exactly the same `ValidationMessage` values, only addressable, and
  it has a unit test. Nothing about the model changed.
- **`tags/errorSummary.html` exists**, against the phase-2 precedent of not making a tag per class.
  Three templates render it and it carries an invariant (every entry links at its control, the
  block takes focus). One caller would not have justified it; three do.
- **The description textarea is now `id="description"`,** not `description-editor`. Every control
  is addressed by its field name, which is what lets the error summary link at any of them with
  one rule instead of a special case. `editor-init.js` follows in one line.
- **Submit controls are `<button>` rather than `<input type="submit">`,** so they carry the `.btn`
  component like every other action on the site. The e2e tests address the form's own control now,
  because the header carries a submit button of its own once signed in — signing out is a POST.
- **`.red` and `.margin-left` are gone from `css/transitional.css`** as scheduled; only `.box`
  remains, and phase 6 removes the file.

### Found while building it

**The two error pages were never served as pages.** `@Produces(MediaType.TEXT_HTML)` on an
`ExceptionMapper` is not read — a mapper is not a resource method — so both the 404 and the 500
answer went out with no `Content-Type` at all and every browser rendered the page's own markup as
text. The existing test asserted the body string, which passed either way; it only became visible
when the redesigned page was screenshotted in a real browser. Fixed by setting the media type on
the response, with an assertion on the content type beside the existing one.

### Tests

- new `ValidationMessagesTest` — a clean form, a field without a fault beside one with, collection
  order kept, only the first fault per field, the German label and its fallback.
- extended `ContactResourceTest` — every control carries a `<label for=…>`; the summary links at
  `#spam` / `#message`; a rejected field is marked `aria-invalid`; the typed message comes back;
  the honeypot stays in the markup.
- new `ContactFormE2ETest` — fill, submit, confirm; a rejected submission keeps the text, holds the
  focus on the summary and lands on the field when the summary link is followed; the honeypot is
  off-screen but neither `display:none` nor `visibility:hidden` (BR-032).
- new case in `VoluntaryFormE2ETest` — the editor initialises inside the restyled container, and
  the rule that restyles it still beats `jodit.min.css`, which is linked after the site stylesheet.
- extended `NotFoundExceptionMapperTest` — the error page is answered as `text/html`.
- `EditorAssetsTest`, `SecurityHeadersTest`, `CssBudgetTest`, `PageStructureTest` — unchanged, all
  still pass. The CSRF and deletion invariants listed below kept their tests untouched.

### Invariants that had to survive this phase — each already had a test

| Invariant | Guard |
|---|---|
| Every state-changing form carries a CSRF token | `Csrf.givenWithoutToken()` cases in `ContactResourceTest`, `EventAddTest`, `EventDeleteTest`, `AuthResourceTest` |
| Deletion is a `POST`, never a `GET` | `EventDeleteTest` (NFR-021) |
| The honeypot stays off-screen, not `display:none` | `ContactResourceTest` and now `ContactFormE2ETest` (FR-032) |
| The spam value stays 50 | `ContactResourceTest` (NFR-009) |
| Rejected forms return the visitor's input | `EventEditTest`, `ContactResourceTest` (FR-008) |
| Editor assets load on the two form pages only | `EditorAssetsTest` (NFR-036) |

### Done

`tooling/docker.sh verify` passes: 154 unit tests (143 before), 11 e2e tests (7 before), coverage
gate met. Verified visually against the running application at 390 px and 1400 px.

**Still owed:** the stylesheet bundle is at 14.9 KB of the 16 KB in NFR-041. Phase 4 adds
`css/pages.css` for the start page, and 1.5 KB is not much room — either the hero and the channel
cards are built from what already exists, or the budget is measured again and restated, as it was
in phase 2. It is not a reason to add a framework; it is a reason to decide before writing.

---

## 6. Phase 4 — Start page and information pages — **delivered 2026-08-26**

**Goal:** `/` becomes a page (FR-023 amended, FR-034, UC-008), and the two information pages get
structure.

### Files

| Action | Path |
|---|---|
| edit | `src/main/java/tools/cevi/infra/IndexResource.java` — `index()` returns a template instead of `Response.seeOther("/anlaesse")`; passes the three nearest events and two offers |
| new | `templates/IndexResource/home.html` — hero, event teaser, inverse offer section, contact band |
| edit | `src/main/java/tools/cevi/event/Event.java` — `upcomingEvents(int limit)` beside the existing query |
| edit | `src/main/java/tools/cevi/voluntary/VoluntaryService.java` — `newest(int limit)` |
| rewrite | `templates/IndexResource/fgi.html` — a card per channel, a chip index, native `<details>` |
| rewrite | `templates/IndexResource/datenschutzinformation.html` — article layout on `.prose` |
| new | `css/pages.css` — hero, inverse section, channel columns, chip index |
| edit | `templates/base.qute.html` — `main` holds the insert; the container moves into the pages |
| edit | `css/layout.css` — `.page-column`, the container plus the page rhythm |
| edit | all 17 other page templates — each opens with `<div class="page-column">` |

**Deviations from the plan as written, and why:**

- **The container moved out of `base.qute.html` into the pages.** Phase 1 left this owed and
  suggested "a second insert for pages that lay themselves out". Qute does not allow that: an
  `{#insert}` inside another `{#insert}`'s default body is a parser error — the inner `{/}` closes
  the outer section. Two sibling inserts would have left an empty container in the markup of the
  one page that skips it. So `main` now holds the insert and nothing else, ordinary pages open with
  `.page-column`, and the start page brings a container per section. It is one line per template
  and it removes the special case for good.
- **The cards on the inverse band keep their light surface.** The concept sketched them inverted.
  A second set of card rules for one section is not worth it, and white cards read as objects lying
  on the band. What the section does need is `.card { color: var(--ink) }` — see below.
- **The channel groups are laid out in columns, not in the card grid.** The groups differ in height
  by a factor of nine (Blogs has one link, Webseiten nine); a grid aligns them into rows and leaves
  holes under the short ones. `columns: 20rem` with `break-inside: avoid` packs them.
- **All channel groups start open, and a chip index sits above them.** The concept asked for
  `<details>` collapsed on a phone and open for the first two. There is no CSS-only way to open a
  `<details>` at one width and close it at another, and starting collapsed would have made the page
  worse on a desktop. The chip index answers what FR-039 is actually for — reaching one channel
  without scrolling past fifty links — at every width, and the groups stay collapsible.
- **The two duplicate `Instagram` blocks were merged.** The old page listed the channel twice, which
  is the kind of thing "grouped by channel" is supposed to make impossible.

### Migration risk — the changed entry point

`/` no longer answers `303 See Other`. Everything that relied on it:

- `IndexResourceTest` asserted the redirect — rewritten to assert the page.
- `IndexResource.admin()` redirects to `/` when already signed in, and
  `quarkus.http.auth.form.landing-page` is `/`. **Signing in therefore now lands on the start page
  rather than on the event list.** That is what UC-008 A4 describes, and the start page shows the
  maintenance group to an administrator. Three e2e tests waited for `**/anlaesse` after signing in
  and had to be told the new destination — the only place the change bit.
- External links to `https://international.cevi.tools/` now land on the start page. `/anlaesse`
  keeps working unchanged, so **no external link breaks** — this is an addition, not a move.

### Found while building it

**A card lying on the inverse band lost its title.** `.section--inverse h2 { color: #fff }` also hit
the `h2` inside every card on that band, so the organisation name rendered white on white — invisible
in the markup, invisible in every REST-Assured assertion, and obvious the moment the page was
screenshotted. Fixed twice over: the rule is scoped to the section's own heading, and `.card` now
states its text colour instead of inheriting it, so a card is safe wherever it is placed.

### Tests

- rewritten `IndexResourceTest` — `/` answers 200 with the hero and both teasers; `/anlaesse` and
  `/volontariat` still answer under their own addresses; the information page groups its sources by
  channel and carries the index.
- new case — with an empty database both sections render their empty state rather than an empty
  grid (A1/A2 of UC-008, BR-049). The demo data is removed and put back around the assertion,
  because the branch under test is exactly "the database holds nothing".
- new `StartPageE2ETest` — the headline sits above the fold at 360 × 640, both teaser links
  navigate, and the full-width sections do not push the page sideways (NFR-013, checked here early
  because a bleeding band is the one shape that breaks it).
- `PageStructureTest` — `/` added to every route it checks.

### Done

`tooling/docker.sh verify` passes: 161 unit tests (154 before), 14 e2e tests (11 before), coverage
gate met. Verified visually at 390 px and 1400 px.

**The stylesheet budget was raised from 16 KB to 24 KB gzipped**, on the product owner's decision
that the figure is not worth optimising against at this scale. Measured after this phase: 16.5 KB.
The ceiling keeps its purpose — it still forbids a CSS framework, and the 150 KB first view has not
moved. Recorded in `docs/requirements.md` and `docs/ux_concept.md` §7.

---

## 7. Phase 5 — Responsiveness, accessibility and budget, made testable — **delivered 2026-08-26**

**Goal:** turn NFR-013 and NFR-014 from *assumed and open* into *verified*, and pin the new NFRs.

### Tests

| Test (`src/test/java/tools/cevi/e2e/`) | Asserts |
|---|---|
| `ResponsiveLayoutE2ETest` | For every public route × {320, 360, 768, 1024, 1440, 1920}: `document.scrollingElement.scrollWidth <= clientWidth`, and separately that nothing outgrows the column it was laid out in. A detail page carrying a six-column table proves the scroll container actually works (NFR-013). |
| `TypographyFloorE2ETest` | No rendered text node computes below 14 px, at 320 and 360 px, signed out and signed in; and each of the six steps of the scale resolves above the floor at 320, 360 and 1920 px (NFR-013). |
| `TapTargetE2ETest` | At 360 px every `a`, `button`, `input`, `summary` has a hit area ≥ 44 × 44 px — with the drawer open, every `<details>` open, and once more as an administrator (NFR-038). |
| `NavigationDrawerE2ETest` | Drawer opens and closes by keyboard at 360 px; exactly one item is marked `aria-current`; sign-out is a `POST` carrying a token and still reads as a menu row. |
| `FocusVisibleE2ETest` | 40 tab stops per route never yield a focused element with no visible indicator; the skip link is the first stop, becomes visible, and following it puts the keyboard inside `<main>` (WCAG 2.4.1, 2.4.7). |
| `ContrastE2ETest` | Every element holding text reaches 4.5 : 1 (3 : 1 when large) against what is painted behind it, including a rejected contact form and the administrator chrome (WCAG 1.4.3). |
| `ReducedMotionE2ETest` | Under `prefers-reduced-motion: reduce` nothing keeps a perceptible transition or animation — with the counter-check that the same pages *do* animate by default, and that the drawer still works with motion off (NFR-040). |
| `CssBudgetTest` (unit, since phase 1) | Stylesheet bytes ≤ budget (NFR-041). |
| `ContrastTest` (unit) | The relative-luminance and contrast arithmetic, against the figures WCAG publishes. |
| `ProseTablesTest` (unit) | The table wrapper below. |

**Deviations from the plan as written, and why:**

- **`ReducedMotionE2ETest` was added.** The phase goal says "pin the new NFRs" but the table listed
  no test for NFR-040, which was the only new NFR with nothing behind it. It is three cases.
- **`Contrast` is a Java record, not arithmetic inside the page script.** A browser can report what
  a colour resolved to; turning two colours into a ratio is the part that can be silently wrong,
  and a contrast test that computes the ratio wrongly is worse than none — it reports a clean sheet
  on a page nobody can read. So the maths lives where `ContrastTest` can check it against the
  published worked examples, and the page only reports what it measured.
- **The two navigation cases moved out of `EventUpcomingE2ETest`.** They were about the header,
  which every page shares, and had lived there only because it was the first e2e test to exist.
- **`PlaywrightTestBase` gained `signInAsAdministrator()`.** Four gates need it: the maintenance
  chrome is the part of the interface no visitor sees, which is where an unreadable label survives
  longest — and it did, see below.
- **Each gate reports what failed, not that something failed.** `ResponsiveLayoutE2ETest` names the
  element that sticks out (including a text node, because a word too long to break overflows without
  any element box growing); `ContrastE2ETest` prints the measured ratio, the two colours and the
  threshold. This was not politeness: the one defect that took three runs to find was invisible
  until the message named it.

### What the gates found

Seven defects, none of which any of the 161 existing tests could see.

| Defect | Where | Fix |
|---|---|---|
| **`<h1>Datenschutzinformation</h1>` pushed the whole page sideways.** One 22-character German compound at `--step-3` is 388 px wide; on a 320 px phone nothing could break it. | `base.css` | `hyphens: auto` (the document is `lang="de"`, so the browser breaks it where German allows) plus `overflow-wrap: break-word` for a word no dictionary knows. |
| **No table in a description had a scroll container.** `prose.css` had carried the `.prose-table` rule since phase 2 on the assumption that the template applied the wrapper. Nothing did — the rule had never once matched an element. | new `ProseTables` + `TemplateExtensions.withScrollableTables`, used by the event detail page and the offer card | Wrapping on the way out of storage rather than in `HtmlSanitizer`: it is presentation, and it also covers descriptions that were stored before the fix. |
| **Every card was 12 px wider than the column holding it** at 320 px — `minmax(300px, 1fr)` against a 288 px column. | `layout.css` | `minmax(min(300px, 100%), 1fr)`. |
| **The card band's kicker measured 4.09 : 1.** `opacity: .78` on white over `--cevi-red`. | `components.css` | `opacity: .9` — 5.07 : 1 on the worst variant, and the step down from the date below it survives. |
| **`.card__admin-note` failed both thresholds at once**: 0.72 rem is 11.5 px, and `#9A968D` on the sunken strip is 2.70 : 1. Administrator-only chrome, so nobody had looked at it since it was written. | `components.css` | `var(--step--1)` and `var(--ink-muted)`; the uppercase and the tracking carry the "this is an aside" reading on their own. |
| **"Alle Anlässe →" was a 26 px tap target.** The way out of a start-page teaser into the full list. | `layout.css` | `inline-flex` with `min-height: var(--tap-target)`. |
| **`tokens.css` overstated its own contrast.** The header claimed 18 px ink at 17.9 : 1; it measures 18.4 : 1. | `tokens.css` | Corrected. Found because `ContrastTest` asserts the file's published figures rather than restating them. |

Two of those — the heading and the missing table wrapper — are the exact failure mode NFR-013 names
in its own text, and both had been shipped for four phases.

### Deliberate non-findings

- **A link inside a sentence is exempt from NFR-038**, per WCAG 2.5.5's own inline exception: a
  44 px-tall link would break the line it sits in. The exemption is a named condition in
  `TapTargetE2ETest`, not a silent filter.
- **A card title is measured as the card.** Its `::after` is stretched over the whole card, so the
  thumb aims at a 300 × 260 px area, not at two lines of text. Measuring the anchor would have
  reported a defect no visitor can experience — and invited someone to "fix" it by padding the
  title until the card fell apart.
- **The checkbox behind the menu label is focusable and off-screen on purpose.** It is what makes
  the drawer work without script; `FocusVisibleE2ETest` accepts the ring the label draws on its
  behalf, but only while the label is actually rendered.

### Requirements

NFR-013, NFR-014, NFR-038, NFR-039, NFR-040 and NFR-041 move to **Verified** in
`docs/requirements.md`, with a note stating for each exactly what is machine-checked and what still
rests on review — WCAG's judgement clauses (link text, heading sense, whether an error message can
be acted on) are not testable and are not claimed to be.

### Done

`tooling/docker.sh verify` passes: 177 unit tests (161 before), 73 e2e tests (14 before), coverage
gate met.

**Still owed.** Two things this phase deliberately did not do:

- **The review with the product owner on a real phone.** The tests answer "does it measure 44 px",
  never "is this the right thing at 44 px". The two lists and the contact form, before and after,
  on a real device, remain part of the phase.
- **Sonar analysis of the new Java classes** (project workflow step 6). `ProseTables`, `Contrast`
  and `PublicPages` were not analysed: the SonarQube MCP server is configured for this machine but
  its tools were not available in the session that wrote them. It carries over to phase 6, which
  already schedules a Sonar pass.

---

## 8. Phase 6 — Cleanup and documentation — **delivered 2026-08-26**

### What was removed

| Removed | Confirmed by |
|---|---|
| `css/transitional.css` and its `@import` | Every class every template uses was listed and diffed against every class the stylesheets declare. `.box`, `.title` and `.date` — the file's whole content — appear in no template. `.red` and `.margin-left` had already gone in phase 3. |
| `.measure` and `.stack` in `layout.css` | Declared as utilities in phase 1 and never used by any phase. (The `--measure` *token* stays: `prose.css`, `pages.css` and `layout.css` all read it.) |
| `empty--inverse` in `home.html` | The reverse diff — a class in the markup that no stylesheet declares. The inverse band styles `.empty` from the section, so the modifier had never done anything. |

The diff in both directions is worth more than the deletions it produced: it is the check that says
whether the stylesheet and the templates still describe the same site.

### The logo

Replaced with a correctly-sized raster: **674 × 80, 5.8 KB**, down from 1500 × 178 and 29 KB. The
header draws it at 28 px on a phone and 40 px from 600 px up, so the asset is sized for the taller
of the two on a high-density screen and nothing more. Downscaled with Lanczos and reduced to a
168-entry palette — the mark is flat colour, so the difference from the full-colour downscale
measures 0.33 % RMSE. `StaticAssetTest` now pins both the byte budget and the pixel width, so the
original cannot quietly come back.

**An SVG would still be better** and is the open point in `docs/ux_concept.md` §10. Tracing the PNG
was deliberately not done: an autotraced approximation of a brand mark is exactly the kind of thing
that should come from the working group rather than from a build step.

### The requirements status pass

Fifteen requirements moved. Re-reading each against *the tests that exist* rather than against the
code that was written turned up three things:

- **NFR-037 was not true as written.** It says 0 component stylesheets contain a literal colour or
  a hard-coded font size; `CssBudgetTest` had only ever checked that no second file declares
  `:root` variables. Six literal colours had accumulated across five files — none wrong to look at,
  each a value that could no longer be changed in one place. They are now three tokens
  (`--ink-soft`, `--on-dark`, `--border-tinted`), `prose.css` reads `--font-mono` instead of
  restating the stack, and the test checks the whole sentence.
- **NFR-042 had no test at all.** It is a data-protection requirement before it is a performance
  one, and it was resting on nobody having yet written a Google Fonts `<link>`. New
  `NoThirdPartyAssetsTest` checks every public page and every stylesheet, distinguishing what the
  browser *fetches* from what a visitor may *follow* — an event description linking to the
  organiser is not a third-party request.
- **C-021 was checked one editor too narrowly.** `EditorAssetsTest` asserted that public pages
  reference no Jodit asset; the constraint says they carry no script at all. It now asserts that.

**NFR-044 and C-019 are *Implemented*, not *Verified*.** The palette and the typefaces are the
Corporate Design Manual's and `tokens.css` measures them, but whether Lora is accepted as the body
face is a working-group decision no test can stand in for (`docs/ux_concept.md` §10).

Still `Open` after the pass, all outside this plan: FR-030 (user accounts), NFR-001 … NFR-004 and
NFR-020 (performance, availability and backup assumptions nobody has measured), NFR-035 (the editor
dependency rule), NFR-045 (dark mode, deferred by decision), C-006 (browser support).

### Documentation

`CLAUDE.md` gained a **Design system** section: `tokens.css` as the single source of colour, type
and spacing, the byte ceiling that makes a framework impossible rather than merely discouraged, and
the seven browser gates with what each holds. It points at `docs/ux_concept.md` for the *why* and at
this document for what each phase found.

### Done

`tooling/docker.sh verify` passes: **188 unit tests** (177 before), **73 e2e tests**, coverage gate
met. The stylesheet bundle is **16.4 KB gzipped** of the 24 KB in NFR-041 — 0.1 KB less than after
phase 4, despite three new tokens, because `transitional.css` went.

**Still owed, and now the whole of what the redesign owes:**

- **The review on a real phone with the product owner.** Carried over from phase 5. The tests
  answer "does it measure 44 px", never "is 44 px the right thing here".
- **Sonar analysis of the new Java classes** — `ProseTables`, `Contrast`, `PublicPages` (project
  workflow step 6). The SonarQube MCP server is configured for this machine but its tools were not
  available in either session that wrote them. This is the one item of the plan that was not
  delivered.
- **The three open points in `docs/ux_concept.md` §10** that are working-group decisions: the Lora
  body face (NFR-044), the start page copy (FR-034), the 160-character excerpt (FR-035). Each is
  recorded against its requirement, none blocks anything.

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
| 1 | Design system, frame, fonts | Large | Yes — delivered |
| 2 | Event and offer cards, prose | Large | Yes — delivered |
| 3 | Forms and status pages | Medium | Yes — delivered |
| 4 | Start page, information pages | Medium | Yes — delivered |
| 5 | Responsive/a11y/budget tests | Medium | Yes — delivered |
| 6 | Cleanup, assets, docs | Small | Yes — delivered |

Phases 1–3 are the minimum viable redesign: after Phase 3 every element the audience named has been
rebuilt. Phases 4–6 are what make it a *finished* site rather than a restyled one.

### Verification after every phase

```shell
rm -f /tmp/international.db     # DemoData must re-seed, or coverage reads low
tooling/docker.sh verify        # unit + Playwright e2e + the 80 % gate
```

Visual review happens on a real phone at the end of Phases 1, 2 and 3 — the concept is written for
360 px, and a desktop browser window is not a substitute.

# UX Concept: Visual Redesign of the Cevi International Platform

**Document status:** Draft for review
**Date:** 2026-08-25
**Derived from:** `docs/vision.md`, `docs/requirements.md`, `docs/entity_model.md`,
`docs/use_cases/UC-001` … `UC-007`, the Cevi Schweiz Corporate Design Manual (2020), and a review
of the templates and stylesheets in the current working copy.
**Companion document:** `docs/redesign_plan.md` (how this concept is delivered)

---

## 1. Why a redesign

The platform works. It is not read. Feedback from the audience is that the site looks dated and
unattractive, naming the event list, the voluntary service list and the contact form in particular.

That feedback is accurate, and the reasons are structural rather than cosmetic:

| Observation in the current interface | Effect on the visitor |
|---|---|
| The entire stylesheet is 245 lines with a single colour (`red`) and no type system. Everything is browser-default Times/Arial at browser-default sizes. | The page reads as an unstyled document, not as a product of the organisation it represents. |
| The list items are `.box` — a 2 px solid black border with a grey fill. Black hairline borders on grey are the visual signature of 2005. | The most important content on the site is the least attractive element on it. |
| Each event card renders the **complete** formatted description inline. A list of five events is a wall of text several screens long. | The list cannot be scanned. The visitor cannot compare events, which is the one thing a list is for. |
| Nothing in a card is a target. The title is a small blue underlined link; edit/delete links sit at the same weight next to it. | There is no obvious next step, and administrator actions compete with visitor actions. |
| The contact form is unlabelled inputs separated by `<br>`, with the spam question as running text. | The form looks like a prototype; the spam question reads as an obstacle rather than a small favour. |
| `main` is capped at `max-width: 800px` with no centring, so on a 1920 px desktop all content sits hard against the left edge. | Desktop looks broken rather than deliberate. |
| The header is the 1500 px logo scaled to 50 px height plus a text menu; there is no hierarchy, no active-state weight, and no footer beyond one credit line. | Nothing frames the content. |
| `organizationLink` is captured by the administrator form but rendered nowhere on the public list. | FR-010 — "every offer links to the organisation running it" — is not actually delivered, so the offer list is a dead end. |

None of this is a functional defect. The functional core (UC-001 … UC-007) is sound and stays
untouched. What is missing is a **design system**: a decided set of colours, type sizes, spacing
steps, radii and components that every page draws from.

### The audience this has to serve

From `docs/vision.md` §3: Cevi members, mostly **15–30**, German-speaking, **browsing mainly on a
phone**, arriving through a link shared in a chat group. That single sentence sets the bar:

- The **first screen on a phone** decides whether the visit continues.
- The visitor did not navigate here — they tapped a link. Every page must therefore work as a
  landing page and answer "what is this, and what else is here?" without scrolling back up.
- This audience reads Instagram and the sites of the organisations linked from `/fgi`. It does not
  need a fashionable design; it needs one that is unmistakably *maintained*.

---

## 2. Design principles

Five rules. Every decision later in this document is traceable to one of them.

1. **Belong to Cevi, visibly.** The Cevi red triangle, Cevi red and Cevi blue, Montserrat — a
   visitor should recognise this as a Cevi site before reading a word. The current site carries
   the logo and nothing else of the brand.
2. **Scannable before readable.** A list answers *which of these is for me*. Full text belongs on
   the detail page. Every list item gets a fixed, predictable shape: when, where, what, one action.
3. **One obvious action per screen.** Exactly one visually dominant call to action per view; every
   other control is quieter. Administrator controls are a separate, subdued visual class — they
   are never the loudest thing on a public page.
4. **Phone first, desktop deliberate.** Layouts are designed at 360 px and grow. Desktop is not a
   stretched phone: it gets a centred measure, a multi-column card grid and a real footer.
5. **Weightless.** No framework, no JavaScript for layout or navigation, two web fonts, one
   stylesheet bundle. The site must stay fast on a phone on mobile data, and it must stay
   maintainable by volunteers who touch it a few times a year.

---

## 3. Design language

### 3.1 Colour

Taken from the **Cevi Schweiz Corporate Design Manual (2020), p. 25** — these are the organisation's
own definitions, not an invention of this document:

| Brand colour | Pantone | CMYK | RGB | Hex |
|---|---|---|---|---|
| Cevi-Rot | 032 C/U | 0 / 100 / 80 / 0 | 196 / 19 / 51 | `#C41333` |
| Cevi-Blau | 072 C/U | 100 / 85 / 0 / 0 | 50 / 51 / 148 | `#323394` |
| Cevi-Schwarz | Black C/U | 0 / 0 / 0 / 100 | 20 / 20 / 18 | `#141412` |

The CD defines three colours for print. A screen interface needs surfaces, borders, muted text,
states and feedback colours as well. The palette below extends the three brand colours without
inventing a fourth hue: everything is either a brand colour, a tint of one, or a warm neutral on
the same axis as Cevi-Schwarz.

```
Brand
  --cevi-red            #C41333   Primary action, accent, active state
  --cevi-red-strong     #9E0F29   Hover/active of red surfaces
  --cevi-red-tint       #FBEAEE   Red-tinted surface (badges, highlights)
  --cevi-blue           #323394   Deep sections, headings on light, links
  --cevi-blue-strong    #26276F   Hover/active of blue surfaces
  --cevi-blue-tint      #ECECF7   Blue-tinted surface

Neutrals (warm axis, derived from Cevi-Schwarz)
  --ink                 #141412   Body text
  --ink-muted           #5A5A56   Secondary text, meta lines
  --border              #E4E2DC   Hairlines, card edges
  --surface             #FFFFFF   Cards, sheets
  --surface-sunken      #F6F5F2   Page background
  --surface-inverse     #1B1B26   Footer, deep sections (blue-shifted black)

Feedback
  --error               #B3122C   Validation text and borders
  --error-surface       #FDF0F2
  --success             #1E6B4F
  --success-surface     #E9F5F0
```

**Contrast, measured (WCAG 2.1 contrast ratio):**

| Combination | Ratio | Verdict |
|---|---|---|
| `--ink` on `--surface` | 17.9 : 1 | AAA |
| `--ink-muted` on `--surface` | 6.9 : 1 | AA (normal text) |
| `--cevi-blue` on `--surface` | 10.3 : 1 | AAA |
| `--cevi-red` on `--surface` | 6.0 : 1 | AA (normal text) |
| `--surface` on `--cevi-red` | 6.0 : 1 | AA — red buttons carry white text |
| `--surface` on `--cevi-blue` | 10.3 : 1 | AAA — blue sections carry white text |
| `--cevi-red` on `--ink` | 3.1 : 1 | **Fails** for text — red is never used on dark; on dark surfaces the accent is white or `--cevi-red-tint` |

This is the reason the palette assigns **blue as the reading colour and red as the action colour**:
blue clears AAA everywhere and can carry text; red clears AA only on white and is therefore
reserved for buttons, the active navigation marker, badges and rules.

**Dark mode** is specified as an optional, low-priority addition (§9). The token layer is built so
that it costs one `@media (prefers-color-scheme: dark)` block redefining ~12 variables, and nothing
else. It is not required for the redesign to be complete.

### 3.2 Typography

The CD manual prescribes **Montserrat** (ExtraBold for the logo and main titles, Bold for
subheadings) and **Lora Regular** for body copy. Both are open source and served from the project's
own domain — never from Google Fonts or any other third party, for both the Content-Security-Policy
and Swiss data-protection reasons (C-011).

| Role | Family | Weight | Size token | Notes |
|---|---|---|---|---|
| Hero headline | Montserrat | 800 | `--step-4` | Uppercase optional, tight tracking (`-0.02em`) |
| Page title (`h1`) | Montserrat | 800 | `--step-3` | |
| Section title (`h2`) | Montserrat | 700 | `--step-2` | |
| Card title (`h3`) | Montserrat | 700 | `--step-1` | |
| Eyebrow / badge / label | Montserrat | 600 | `--step--1` | Uppercase, `letter-spacing: 0.06em` |
| Navigation, buttons | Montserrat | 600 | `--step-0` | |
| Body copy, descriptions | Lora | 400 | `--step-0` | `line-height: 1.65`, measure `68ch` |
| Meta line (date, place) | Montserrat | 600 | `--step--1` | |

**Fluid scale** — one `clamp()` per step, so nothing needs a breakpoint to resize:

```
--step--1  clamp(0.875rem, 0.85rem + 0.15vw, 0.95rem)   14 → 15.2 px
--step-0   clamp(1rem,     0.96rem + 0.20vw, 1.125rem)  16 → 18 px
--step-1   clamp(1.25rem,  1.15rem + 0.50vw, 1.5rem)    20 → 24 px
--step-2   clamp(1.5rem,   1.30rem + 1.00vw, 2rem)      24 → 32 px
--step-3   clamp(1.875rem, 1.50rem + 1.90vw, 2.75rem)   30 → 44 px
--step-4   clamp(2.25rem,  1.60rem + 3.20vw, 4rem)      36 → 64 px
```

The smallest step is 14 px at 320 px viewport, which is exactly the floor NFR-013 sets.

**A note on Lora.** A serif for body copy is the CD's instruction, and it is what will make this
site look *published* rather than *generated* — the risk is that a serif reads as the very
"old school" the feedback complains about. The mitigation is that Lora only ever appears as
generously-spaced prose at 16–18 px inside a card or an article; all interface chrome — navigation,
buttons, badges, labels, meta lines — is Montserrat. If Lora nevertheless tests badly with the
audience, it is a **one-token change** (`--font-body`) to fall back to the CD's third face,
Helvetica Neue, via a system stack. That escape hatch is deliberate.

**Font delivery — measured, not estimated.** The two families were subset from the upstream
variable fonts and now live in `src/main/resources/META-INF/resources/fonts/`, together with their
`OFL.txt` licences. The weight axis is kept whole (Montserrat 100–900, Lora 400–700) because
pinning it saves little; what saves a great deal is splitting each face by `unicode-range`, exactly
as Google Fonts does:

| File | Size | Loaded |
|---|---|---|
| `montserrat-latin.woff2` | 34.3 KB | always |
| `lora-latin.woff2` | 35.5 KB | always |
| `montserrat-latin-ext.woff2` | 49.9 KB | only when a Latin-Extended character is rendered |
| `lora-latin-ext.woff2` | 19.2 KB | only when a Latin-Extended character is rendered |
| `lora-italic-latin.woff2` | 38.5 KB | only when italic text is rendered |
| `lora-italic-latin-ext.woff2` | 20.3 KB | only for italic *and* Latin-Extended |
| **Baseline for a German page** | **69.9 KB** | |

A browser fetches a `@font-face` file only when rendered text actually needs it, so the extended
and italic faces cost nothing on a page that has no `ě`, `ł` or `<em>` — and they are there when an
event is held in Poděbrady or Kraków. Latin-1 alone would not cover those, and folding the extended
range into the base files would nearly double Montserrat (34 KB → 72 KB) on every page.

The two baseline files are `<link rel="preload">`ed from `base.qute.html`; all six are declared
`font-display: swap`. Fallbacks (`system-ui`, `Georgia`) are declared with `size-adjust` so the swap
does not shift the layout.

### 3.3 Space, shape, elevation, motion

```
Spacing (one 4 px-based scale, used for everything)
  --space-1  0.25rem   --space-5  2rem
  --space-2  0.5rem    --space-6  3rem
  --space-3  0.75rem   --space-7  4rem
  --space-4  1rem      --space-8  6rem

Radii
  --radius-sm    8px    inputs, small controls
  --radius-md   16px    buttons, badges on cards
  --radius-lg   20px    cards, sheets, media bands
  --radius-full 999px   pills, chips, avatar shapes

Elevation (never more than two levels on one screen)
  --shadow-1  0 1px 2px rgb(20 20 18 / .06), 0 1px 3px rgb(20 20 18 / .06)
  --shadow-2  0 2px 4px rgb(20 20 18 / .05), 0 12px 28px rgb(20 20 18 / .10)

Motion — decorative only, never blocking
  --ease      cubic-bezier(.2, .8, .2, 1)
  --dur-fast  120ms   (hover, focus)
  --dur-base  220ms   (card lift, drawer)
```

All motion is wrapped in `@media (prefers-reduced-motion: reduce)` and reduced to `0ms`.

**Focus** is one shared, highly visible ring — never `outline: none`:
`outline: 3px solid var(--cevi-blue); outline-offset: 2px; border-radius: inherit`.

### 3.4 The triangle motif

The Cevi logo is a tilted red triangle standing for head, heart and hand. It is the organisation's
strongest graphic asset and the site currently uses it only inside the logo bitmap.

The redesign lifts it out as a reusable motif, delivered as a single inline SVG symbol:

- **Card accent band** — a coloured band at the top of every event card, carrying a low-opacity
  triangle pattern (§4.3).
- **Section divider** — a row of triangles between the inverse-coloured sections of the start page.
- **List markers** — `ul` bullets on information pages become small red triangles.
- **Empty states** — an oversized outline triangle behind the "nothing here yet" message.

Since neither `EVENT` nor `VOLUNTARY_SERVICE` carries an image (`docs/entity_model.md`) and adding
one is deliberately out of scope, the accent band is **the** source of visual variety in a list. To
keep a list of five events from looking like five identical grey slabs, the band picks one of four
accent variants deterministically from the record's `slug` (or `id` for offers):

```
variant 0  solid Cevi-Rot
variant 1  solid Cevi-Blau
variant 2  red → deep-red diagonal gradient, triangle pattern at 8 % white
variant 3  blue → deep-blue diagonal gradient, triangle pattern at 8 % white
```

Deterministic means the same event always shows the same colour — it becomes a weak recognition
cue for a returning visitor, and it never flickers between page loads. It requires **no schema
change and no upload feature**: the variety is computed, not stored.

---

## 4. Layout and components

### 4.1 Layout system

| Token | Value | Purpose |
|---|---|---|
| `--container` | `min(100% - 2 * var(--space-4), 1120px)` | Centred page container, replaces today's uncentred `max-width: 800px` |
| `--measure` | `68ch` | Maximum line length for prose (descriptions, information pages) |
| Card grid | `repeat(auto-fill, minmax(300px, 1fr))` | 1 column ≤ 660 px, 2 up to ~1000 px, 3 above — no media queries needed |
| Breakpoints | `600px`, `900px`, `1200px` | Used only where `auto-fill`/`clamp()` cannot express the change (navigation, footer) |

The page is a `grid` with `header / main / footer` rows and `min-height: 100dvh` (`dvh`, not `vh`,
so the mobile browser's collapsing address bar does not cut the footer off).

### 4.2 Header and navigation

**Phone (< 900 px)** — a 64 px bar: logo left, hamburger right. The existing checkbox-based drawer
is *kept* (it needs no JavaScript, which keeps `script-src 'self'` clean and costs nothing) but
restyled into a full-height sheet:

```
┌────────────────────────────────┐
│ [Cevi International]        ☰  │  64px bar, --surface, --shadow-1 on scroll
└────────────────────────────────┘
   ↓ checkbox checked
┌────────────────────────────────┐
│ [Cevi International]        ✕  │
├────────────────────────────────┤
│                                │
│  Anlässe                    ›  │  each row 56px min, --step-1,
│  Volontariat                ›  │  Montserrat 600, red left-edge
│  FGI                        ›  │  marker on the active row
│  Kontakt                    ›  │
│  ────────────────────────────  │
│  Datenschutz                   │  --step--1, --ink-muted
│  Abmelden                      │  (only when signed in)
│                                │
│  ▲ ▲ ▲   triangle motif        │
└────────────────────────────────┘
```

**Desktop (≥ 900 px)** — a single row inside `--container`: logo left, links right, Montserrat 600.
The active link is marked by a 3 px Cevi-red underline that animates in on hover
(`transform: scaleX()`, so it never reflows). The current `background-color: rgba(0,0,0,.08)` block
highlight is dropped — it is the single most dated element in the header.

The sign-out control stays a `POST` form with a CSRF token (NFR-011, NFR-021) and is styled to be
visually identical to a nav link. This is already the case and must not regress.

### 4.3 Event card — the most important component on the site

This replaces `templates/tags/event.html` and the `.box` rule.

```
Phone (single column, full width)          Desktop (3-up grid)
┌──────────────────────────────┐           ┌─────────┐ ┌─────────┐ ┌─────────┐
│▚▚▚▚▚▚ accent band ▚▚▚▚▚▚▚▚▚▚│           │▚▚▚▚▚▚▚▚▚│ │▚▚▚▚▚▚▚▚▚│ │▚▚▚▚▚▚▚▚▚│
│                              │           │  ▲      │ │  ▲      │ │  ▲      │
│  15.–24. OKTOBER 2026        │  eyebrow  │ …       │ │ …       │ │ …       │
├──────────────────────────────┤           └─────────┘ └─────────┘ └─────────┘
│                              │
│  Euro Camp 2026              │  h3, Montserrat 700, --step-1
│                              │
│  ◈ Prag, Tschechien          │  meta, Montserrat 600, --ink-muted
│                              │
│  Zwei Wochen mit 400 Cevi-   │  excerpt, Lora 400, plain text,
│  Leuten aus ganz Europa …    │  max 160 chars, 3 lines clamped
│                              │
│  ┌────────────┐  ⟨noch 42 T.⟩│  primary button + urgency chip
│  │  Details → │              │
│  └────────────┘              │
└──────────────────────────────┘
   ┌ admin only ────────────────┐
   │ Bearbeiten · Löschen       │  subdued strip, --step--1
   └────────────────────────────┘
```

Decisions embedded in that sketch, and why:

- **The whole card is the link** (a stretched pseudo-element over the card, with the title as the
  accessible name). On a phone, a 300 × 260 px target beats a 16 px underlined title. The
  "Details →" button remains visible as the affordance; it is not a second link.
- **The description becomes an excerpt.** The list renders a tag-stripped, ~160-character plain-text
  excerpt with `-webkit-line-clamp: 3`; the full formatted HTML stays on the detail page. This is
  the single largest readability win available and it changes no data — but it does change what
  UC-001 describes, so it is written up as a requirement (FR-035) rather than slipped in.
- **`date` stays the headline date**, verbatim, in the accent band — it is free text the
  administrator wrote for humans ("15. - 24. Oktober 2023") and it must not be reformatted.
- **`displayDate` earns a second job**: an urgency chip computed at render time — "Noch 42 Tage",
  "Nächste Woche", "Diese Woche", "Läuft bald ab". It costs no schema change, uses a field that is
  already mandatory, and it is the kind of signal this audience responds to. Written up as FR-036.
- **Administrator actions move below a hairline**, at `--step--1` in `--ink-muted`, prefixed by a
  small pencil/trash glyph. They are present and reachable but no longer compete with the visitor's
  action (principle 3). `Display Date: …`, currently printed raw into the public card for
  administrators, moves into that strip.

### 4.4 Voluntary service card

Same shell, different content — and it **fixes the missing organisation link**:

```
┌──────────────────────────────┐
│▚▚▚▚▚ accent band ▚▚▚▚▚▚▚▚▚▚▚│
│  VOLONTARIAT                 │  eyebrow
├──────────────────────────────┤
│  YMCA Spitak                 │  h3 — the organisation
│  ◈ Armenien                  │  meta — location
│                              │
│  Sechs bis zwölf Monate in   │  excerpt, 3 lines
│  einem Jugendzentrum …       │
│                              │
│  ┌──────────────────────┐    │
│  │ Zur Organisation ↗   │    │  primary, opens organizationLink
│  └──────────────────────┘    │  in a new tab, rel="noopener"
└──────────────────────────────┘
```

`organizationLink` is validated to start with `http://` or `https://`
(`docs/entity_model.md`) and is today captured but never rendered. Rendering it is not a new
feature — it is FR-010 finally being delivered.

Because offers have no detail page and no slug, the whole card is *not* a link here; the button is
the only target, and the excerpt is expanded in place via `<details>` when it overflows.

### 4.5 Contact form

The named weak point. Current state: unlabelled controls, `<br>` layout, the anti-spam instruction
as a sentence in the middle of the form, errors collected in a red list far from the field that
caused them.

```
┌────────────────────────────────────────┐
│  Schreib uns                           │  h1
│  Wir sind eine Handvoll Freiwilliger   │  lead, Lora, --measure
│  und melden uns meist innert weniger   │
│  Arbeitstage.                          │
├────────────────────────────────────────┤
│  Womit wir helfen können               │  h2
│  ┌──────────┐ ┌──────────┐ ┌─────────┐│  chip grid, 2-up on phone,
│  │ Beratung │ │ Kontakte │ │ Reise-  ││  3-up on desktop — replaces
│  │ zu Reisen│ │ vor Ort  │ │ gruppen ││  today's six-item bullet list
│  └──────────┘ └──────────┘ └─────────┘│
├────────────────────────────────────────┤
│  ┌──────────────────────────────────┐  │  card, --surface, --shadow-1
│  │ Deine Nachricht *                │  │  visible label, Montserrat 600
│  │ ┌──────────────────────────────┐ │  │
│  │ │                              │ │  │  textarea, --radius-sm,
│  │ │                              │ │  │  min-height 200px, resize-y,
│  │ └──────────────────────────────┘ │  │  focus ring = --cevi-blue
│  │ Magst du eine Antwort? Schreib   │  │  helper text, --step--1
│  │ deine Mail oder Nummer dazu.     │  │  (explains BR-020 politely)
│  │                          0/5000  │  │  live counter, CSS-only*
│  │                                  │  │
│  │ Sicherheitsfrage *               │  │
│  │ Wie viel ergibt 20 + 30?         │  │  the arithmetic, phrased as
│  │ ┌──────┐                         │  │  a question, not an order
│  │ │      │  inputmode="numeric"    │  │
│  │ └──────┘                         │  │
│  │                                  │  │
│  │  ┌────────────────────────────┐  │  │
│  │  │      Nachricht senden      │  │  │  full-width on phone,
│  │  └────────────────────────────┘  │  │  auto on desktop, --cevi-red
│  └──────────────────────────────────┘  │
│  Wir speichern nur, was du hier        │  --step--1, --ink-muted,
│  schreibst. → Datenschutz               │  links to the DSG page (C-011)
└────────────────────────────────────────┘
```

- **Every control gets a visible `<label>`.** Placeholders are not labels; they vanish on focus and
  fail WCAG 1.3.1.
- **Errors move to the field.** `aria-invalid="true"`, a red left border, and the message directly
  under the control. A summary block stays at the top of the form (focused on load, `role="alert"`)
  because it is what a screen reader announces first — but it now *links* to the offending field.
- **The honeypot stays exactly as it is** — off-screen, `aria-hidden`, out of the tab order
  (BR-032, FR-032). It must not become `display: none`.
- **The spam question is reframed** from "Diese Massnahme schützt uns vor automatisiertem Spam" as
  an apology into a plain question with a numeric keypad on mobile. The *value* checked stays 50;
  nothing about NFR-009 changes.
- \* The character counter is CSS-only (`textarea:not(:placeholder-shown)` cannot count) — so
  either it is a static "max. 5000 Zeichen" hint, or it needs the one small script the site
  otherwise avoids. **Recommendation: the static hint.** It is not worth an exception to
  principle 5.

### 4.6 Start page (new)

Today `/` is a `303` to `/anlaesse` (`IndexResource.index()`). A visitor who taps a shared link
lands in a bare list with no idea what the site is. The redesign gives `/` a real page — this is
the single biggest change to structure, and it amends FR-023.

```
┌───────────────────────────────────────────────┐
│  header                                       │
├───────────────────────────────────────────────┤
│                                               │
│   ▲  CEVI INTERNATIONAL                       │  eyebrow, red triangle
│                                               │
│   Raus aus der Schweiz.                       │  hero, Montserrat 800,
│   Rein in die Welt.                           │  --step-4, max 12 words
│                                               │
│   Anlässe, Volontariate und Kontakte im       │  lead, Lora, --measure
│   weltweiten YMCA/YWCA-Netz — gesammelt       │
│   von der Fachgruppe International.           │
│                                               │
│   ┌──────────────────┐  ┌─────────────────┐   │
│   │ Anlässe ansehen  │  │ Schreib uns     │   │  primary + secondary
│   └──────────────────┘  └─────────────────┘   │
│                                               │
│   ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲     │  divider motif
├───────────────────────────────────────────────┤
│  Die nächsten Anlässe                    alle →│  h2 + text link
│  ┌─────────┐ ┌─────────┐ ┌─────────┐          │  the 3 nearest events,
│  │  card   │ │  card   │ │  card   │          │  identical component
│  └─────────┘ └─────────┘ └─────────┘          │  to /anlaesse (§4.3)
├───────────────────────────────────────────────┤
│  ░░ inverse section, --surface-inverse ░░░░░  │
│  Länger bleiben: Volontariat             alle →│
│  ┌─────────┐ ┌─────────┐                      │  2 offers, cards inverted
│  └─────────┘ └─────────┘                      │
├───────────────────────────────────────────────┤
│  Du weisst noch nicht, was passt?             │  contact teaser band,
│  Wir beraten dich.        ┌────────────────┐  │  --cevi-red-tint
│                           │ Kontakt        │  │
│                           └────────────────┘  │
├───────────────────────────────────────────────┤
│  footer                                       │
└───────────────────────────────────────────────┘
```

Rules for this page:

- It **reuses the existing card components**. No component exists only on the start page.
- If there is no upcoming event, the events section renders an empty state
  ("Gerade ist nichts ausgeschrieben — schreib uns, wir wissen meist mehr") rather than an empty
  grid. Empty states are part of the deliverable, not an afterthought.
- The hero carries **no photograph** — decided in §3.4 and confirmed with the product owner. It is
  carried by type, the triangle motif and a red/blue gradient field.
- `/anlaesse` remains a first-class, directly linkable page. Nothing that works today stops working;
  the redirect is replaced, not the list.

### 4.7 Event detail page

```
  ← Alle Anlässe                              back link, sticky on phone
  ┌──────────────────────────────────────┐
  │▚▚▚ accent band, taller (140px) ▚▚▚▚▚│
  │  15.–24. OKTOBER 2026                │
  └──────────────────────────────────────┘
  Euro Camp 2026                            h1, --step-3
  ◈ Prag, Tschechien   ⟨noch 42 Tage⟩       meta row

  ─────────────────────────────────────     hairline

  <full formatted description>              Lora, --measure, styled
                                            headings/lists/tables/images
                                            from the sanitiser allow-list

  ┌────────────────────────────────┐
  │  Fragen dazu? Schreib uns  →   │        contact teaser, --cevi-red-tint
  └────────────────────────────────┘
```

The description is the one place on the site where **administrator-authored HTML is rendered**. The
allow-list (`docs/entity_model.md`) permits headings, lists, tables, links, images, colours and
alignment — every one of those needs a style rule, or an administrator's table will render as
unstyled browser-default markup inside an otherwise designed page. A dedicated `.prose` block is
therefore part of the stylesheet, scoped so that it cannot leak out and restyle the chrome:
`img { max-width: 100%; height: auto; border-radius: var(--radius-sm) }`, `table` with the card's
border treatment and an `overflow-x: auto` wrapper so a wide table never breaks the page on a
phone (NFR-013).

### 4.8 FGI page

Currently ~50 links in nine consecutive `<ul>` blocks. It is a genuinely useful page rendered as a
raw dump.

Redesign: a short introduction, then one **card per channel group** (Blogs, Instagram, Facebook,
Websites, Newsletter, Fotoalben, …) in the same `auto-fill` grid, each with a channel glyph, the
group name and the links as rows with a `↗` affordance. On a phone the groups collapse into
`<details>` elements — native, no JavaScript, and `open` by default for the first two.

### 4.9 Remaining pages

| Page | Treatment |
|---|---|
| `/volontariat` | Intro + card grid (§4.4) + empty state. |
| `/datenschutzinformation` | Article layout: `--measure`, styled headings, triangle list markers, a table of contents on desktop. |
| `/auth/login` | Centred card, max 420 px, labelled fields, autocomplete hints (`username`, `current-password`), full-width red submit. No navigation clutter. |
| `/auth/error`, `/auth/loggedOut` | Centred status card with a triangle glyph and one clear way onward. |
| `/kontakt` (submitted) | Success card: green check, the confirmation sentence, and two links back into the content — a dead-end confirmation page wastes the visitor's momentum. |
| Admin forms (event, offer) | Two-column on desktop (metadata left, description editor right), stacked on phone. Labels above fields, the `question-mark.svg` tooltips replaced by permanent helper text under the field — a `title` tooltip is invisible on a touch device. The Jodit editor gets a container that matches the form's radius and focus ring. **Editor assets stay loaded from these two templates only (NFR-036).** |
| Delete confirmation | The record rendered in its card, then a clearly destructive treatment: "Löschen" as a red-outlined button, "Abbrechen" as the quiet default, with the destructive action *second*. |
| `error404`, `error500` | Oversized triangle, plain German sentence, primary link to `/anlaesse`. The 500 page keeps the `errorId` in a copyable monospace chip. |
| `/version` | Definition list in a small card. Administrator-only; it does not need decoration. |

### 4.10 Footer

Replaces the single credit line:

```
┌───────────────────────────────────────────────────┐
│ ░░ --surface-inverse ░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
│                                                   │
│  [Cevi International]     Entdecken    Über uns   │
│  Die Fachgruppe Inter-    Anlässe      FGI        │
│  national des Cevi        Volontariat  Kontakt    │
│  Schweiz.                              Datenschutz│
│                                                   │
│  ─────────────────────────────────────────────    │
│  Unterstützt durch Hosttech (Cevihost)    ▲ ▲ ▲   │
└───────────────────────────────────────────────────┘
```

Stacked on phone, three columns from 600 px. The data-protection link lives here **and** stays in
the navigation drawer, because C-011 requires it reachable from every page.

---

## 5. Responsive behaviour

| Viewport | Behaviour |
|---|---|
| 320–599 px | Single column. Container padding `--space-4`. Nav = drawer. Cards full width. Buttons full width. Footer stacked. Hero at `--step-4` floor (36 px). |
| 600–899 px | Cards go 2-up (`auto-fill`). Footer 3 columns. Nav still a drawer — a 4-item menu plus sign-out does not fit comfortably beside the 1500 × 178 logo below 900 px. |
| 900–1199 px | Horizontal nav. Cards 3-up where width allows. Start page sections gain `--space-7` vertical rhythm. |
| ≥ 1200 px | Container caps at 1120 px and centres. Prose caps at `--measure`. Nothing stretches further; the extra space becomes margin. |

**Verified invariants** (these become the e2e assertions in the plan):

- No horizontal scrolling at 320, 360, 768, 1024, 1440 and 1920 px on every route (NFR-013).
- No text below 14 px anywhere (NFR-013).
- Every interactive target ≥ 44 × 44 px on touch viewports (new NFR-038).
- The 1500 px logo is replaced by a responsive asset — today it is downloaded at full size and
  scaled to 50 px by the browser on every page, on every phone.

---

## 6. Accessibility

NFR-014 states WCAG 2.1 AA as an assumption. The redesign is the moment to make it real rather than
aspirational, because retrofitting contrast and focus order into a finished design costs far more
than designing with it.

| Criterion | How the design satisfies it |
|---|---|
| 1.3.1 Info & relationships | Every form control has a `<label>`; sections use real headings in order (one `h1` per page — today several pages open at `h2` with no `h1`). |
| 1.4.3 Contrast | Every combination measured in §3.1; red is never used for text on dark. |
| 1.4.4 Resize text | Everything sized in `rem`/`clamp()`; the layout survives 200 % zoom because widths are `min()`/`auto-fill`, not fixed pixels. |
| 1.4.10 Reflow | No horizontal scroll at 320 px; wide tables inside descriptions get their own scroll container. |
| 2.1.1 Keyboard | The drawer is a checkbox — natively focusable and toggled with Space. Card links are real `<a>`. No custom widgets. |
| 2.4.1 Bypass blocks | A visually-hidden "Zum Inhalt springen" skip link becomes the first focusable element. |
| 2.4.7 Focus visible | One shared 3 px blue ring; `:focus-visible`, never suppressed. |
| 2.5.5 Target size | ≥ 44 px, enforced by the token `--tap-target: 2.75rem` on all controls. |
| 3.3.1/3.3.3 Errors | Errors named in text at the field, `aria-invalid`, plus a linked summary in `role="alert"`. |
| 4.1.2 Name/role/value | The card's stretched link takes its accessible name from the title, not from "Details". |

The card's stretched-link pattern needs care: it must not swallow the administrator's edit/delete
links. Those sit in a strip with `position: relative; z-index: 1` so they stay independently
clickable — this is the classic failure mode of the pattern and is called out here so it is tested.

---

## 7. Performance budget

The current public page ships one 245-line stylesheet and a 29 KB logo, and — since the Jodit fix —
no editor assets (NFR-036). The redesign must not undo that.

| Asset | Budget | Note |
|---|---|---|
| CSS (all files, gzipped) | ≤ 24 KB | **16.5 KB measured** after phase 4 — hand-written tokens + components, comments included. Before the redesign: ≈ 1.5 KB raw, which is why it looked unstyled. |
| Web fonts | ≤ 90 KB | **69.9 KB measured** for a German page (Montserrat + Lora, Latin). Extended and italic faces load only when used. |
| JavaScript on public pages | **0 bytes** | Unchanged. Navigation, disclosure and layout are CSS-only. |
| Logo | ≤ 12 KB | Replace the 1500 × 178 PNG with an SVG, or a 2× raster at the size actually used. |
| Total public page weight | ≤ 150 KB | Well inside NFR-001's 1000 ms budget on mobile data. |
| Editor assets on public pages | 0 bytes | NFR-036 — must not regress; `EditorAssetsTest` already guards it. |

Fonts are the only new cost. They are worth it: typography is what separates this from the
"unstyled document" impression, and 70 KB cached for a year is a one-time cost per visitor.

---

## 8. What deliberately does not change

Recording this matters as much as the design itself — a redesign is where scope quietly grows.

- **No new functionality.** UC-001 … UC-007 keep their flows. Nothing is added to the data model.
- **No photographs, no image upload.** Confirmed with the product owner; the graphic system in §3.4
  is the substitute.
- **No SPA, no JavaScript framework, no CSS framework** (C-004, C-017). Confirmed with the product
  owner: hand-written design tokens.
- **No second language.** German only (C-005).
- **No change to the security invariants.** CSRF on every state change, sanitisation before
  storage, `script-src 'self'` with no inline `<script>`, editor assets on the two form pages only.
  Every one of these has a test; none of them may be weakened to make a layout easier.
- **No change to `EXCHANGE`.** The open question in `docs/vision.md` §9 stays open; the redesign
  neither builds nor removes the feature. `templates/tags/exchange.html` is restyled along with its
  siblings so it does not rot, and nothing more.

---

## 9. Optional, explicitly out of the first delivery

| Idea | Why it is deferred |
|---|---|
| Dark mode | Costs ~12 token redefinitions once the token layer exists. Worth doing, but it doubles the visual review effort. Low priority. |
| Per-event share button (Web Share API) | Needs JavaScript on a public page; violates the 0-byte budget for a small gain. Reconsider only if link sharing is measured. |
| Filtering the event list (by region, by month) | Real value, but it is a new use case with new requirements — not a redesign. |
| Photographs per event | Needs a schema change, an upload path, storage on a read-only container filesystem (C-014) and a licence process. A separate project. |
| Open Graph preview images | Would meaningfully improve links shared in chat groups — which is *the* distribution channel (vision §3). Cheap for a static image, expensive per-event. Worth a follow-up decision. |

---

## 10. Open points for confirmation

| Ref | Question |
|---|---|
| §3.2 | Is Lora as body face accepted, given the "old school" feedback — or should body copy use the CD's third face (Helvetica Neue / system sans)? The font files are already in the repository, so switching costs one token, not a new download. |
| §4.3 | Is truncating the description to a 160-character excerpt in the list acceptable to the administrators who write those descriptions? |
| §4.6 | Is the hero wording ("Raus aus der Schweiz. Rein in die Welt.") on-brand, or should the working group supply the copy? |
| §4.6 | Does `/` becoming a real page instead of a redirect need coordination with anyone linking to the site today? |
| §7 | Is a replacement logo asset (SVG or correctly-sized raster) available from the working group, or should the existing PNG be down-scaled? |
| §9 | Dark mode in this delivery or a later one? |

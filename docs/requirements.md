# Requirements: Cevi International Platform

**Document status:** Draft for review
**Date:** 2026-08-25 (revision 5 — visual redesign: design system, brand conformity, entry page,
scannable lists, form usability, responsiveness and accessibility made testable)
**Source:** `docs/vision.md`, `docs/use_cases/UC-001` … `UC-008`, `docs/entity_model.md`,
security review of 2026-08-15, `docs/ux_concept.md` and `docs/redesign_plan.md` of 2026-08-25,
Cevi Schweiz Corporate Design Manual (2020)

Requirement IDs are unique across all three tables. Business rules referenced as `BR-xxx` are
defined in the use case specifications under `docs/use_cases/`.

Thresholds marked **(assumed)** were not derivable from the existing documents; they are proposed
here so the requirement is testable and are listed for confirmation in
[Open Points for Confirmation](#open-points-for-confirmation).

## Functional Requirements

| ID     | Title                          | User Story                                                                                                                                                          | Priority | Status       |
|--------|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------|
| FR-001 | List Upcoming Events           | As a visitor, I want to see all events whose display date has not passed so that I only spend time on events I can still attend.                                      | High     | Verified     |
| FR-002 | Order Events Chronologically   | As a visitor, I want the event list ordered by display date with the nearest event first so that the most urgent opportunity is at the top.                           | High     | Verified     |
| FR-003 | Open an Event on Its Own Page  | As a visitor, I want to open an event on its own page under a stable readable address so that I can read the full details and share a link that still works later.    | High     | Verified     |
| FR-004 | Create Event                   | As an administrator, I want to add an event with title, date text, location, display date and formatted description so that the working group can announce it publicly. | High     | Verified     |
| FR-005 | Edit Event                     | As an administrator, I want to change an already published event so that corrections reach visitors without a developer being involved.                               | High     | Verified     |
| FR-006 | Delete Event with Confirmation | As an administrator, I want to remove an event only after confirming on a separate page so that outdated content disappears but a misclick deletes nothing.           | High     | Verified     |
| FR-007 | Derive Unique Short Name       | As an administrator, I want the system to derive a unique short name from the title when I supply none so that I do not have to invent public addresses myself.       | Medium   | Verified     |
| FR-008 | Preserve Rejected Form Entries | As an administrator, I want a rejected form returned with my values and the faulty fields marked so that I do not have to retype a long description.                  | High     | Verified     |
| FR-009 | Browse Voluntary Service Offers| As a visitor, I want to see all recorded voluntary service offers with organisation, location and description so that I can decide which organisation to approach.    | High     | Verified     |
| FR-010 | Link Offer to Organisation     | As a visitor, I want every offer to link to the website of the organisation running it so that I can apply directly with that organisation.                           | High     | Verified     |
| FR-011 | Create Voluntary Service Offer | As an administrator, I want to add an offer with organisation, organisation link, location and description so that visitors learn about a new opportunity.            | High     | Verified     |
| FR-012 | Edit Voluntary Service Offer   | As an administrator, I want to change an existing offer so that the published information stays accurate.                                                            | High     | Verified     |
| FR-013 | Delete Offer with Confirmation | As an administrator, I want to remove an offer only after confirming on a separate page so that offers that ended disappear without accidental deletions.            | High     | Verified     |
| FR-014 | Send Contact Message           | As a visitor, I want to send a free-text message to the working group without creating an account so that I can ask a question with no barrier.                       | High     | Verified     |
| FR-015 | Answer Spam Protection         | As an administrator, I want the contact form to accept a message only when the spam protection field contains the expected number so that automated submissions do not reach the working group. | High     | Verified     |
| FR-016 | Notify Working Group by E-Mail | As an administrator, I want an e-mail notification at the configured address whenever a message arrives so that I can answer without checking the site.               | High     | Verified     |
| FR-017 | Record Contact Message         | As an administrator, I want every accepted message also recorded in the database so that a message survives a failed e-mail delivery.                                 | Medium   | Verified     |
| FR-018 | Confirm Message Submission     | As a visitor, I want a confirmation page after sending so that I know the message left my browser.                                                                    | High     | Verified     |
| FR-019 | Sign In as Administrator       | As an administrator, I want to sign in with user name and password so that the maintenance functions for events and offers become available.                          | High     | Verified     |
| FR-020 | Sign Out                       | As an administrator, I want to sign out so that the maintenance functions are no longer reachable from my browser.                                                    | High     | Verified     |
| FR-021 | Redirect Unauthenticated Access| As an administrator, I want to be sent to the sign-in page when I open a maintenance function without a session so that I can sign in instead of seeing an error.     | High     | Verified     |
| FR-022 | Hide Maintenance Links         | As a visitor, I want edit and delete links hidden unless a verified administrator session is present so that the public pages show only what concerns me.             | Medium   | Verified     |
| FR-023 | Reach the Event List in One Step | As a visitor, I want the list of upcoming events reachable in one step from the start page and under its own stable address so that I get to the main content without navigating a hierarchy. | High     | Verified     |
| FR-024 | View Working Group Presentation| As a visitor, I want a page presenting the international working group so that I know who is behind the platform before I contact them.                               | Medium   | Implemented  |
| FR-025 | View Data Protection Page      | As a visitor, I want the data protection information reachable from every page so that I can check how my data is handled before I write a message.                   | High     | Implemented  |
| FR-026 | View Version Page              | As an operator, I want a page naming the application version and the database schema version, reachable only with an administrator session, so that I can tell whether a deployment and its migrations match without publishing that detail to anonymous visitors. | Medium   | Verified     |
| FR-027 | Show Not-Found Page            | As a visitor, I want an explanatory page with a way back when I open an unknown address so that a stale link does not end my visit.                                   | Medium   | Verified     |
| FR-028 | Show General Error Page        | As a visitor, I want a plain error page when the system cannot produce the requested page so that I never see technical internals.                                    | Medium   | Implemented  |
| FR-029 | Browse Exchange Offers         | As a visitor, I want to see exchange opportunities with partner organisations so that I can consider an exchange alongside events and voluntary service.              | Low      | Needs review |
| FR-030 | Manage User Accounts           | As an operator, I want to create further administrator accounts and let administrators change their own password from within the application so that access does not depend on direct database edits.                | Medium   | Open         |
| FR-031 | Bootstrap Initial Administrator| As an operator, I want the first administrator account created from a deployment secret when the user table is empty so that a fresh or restored production database never carries publicly known credentials.        | High     | Implemented  |
| FR-032 | Reject Automated Submissions   | As an administrator, I want the contact form to discard submissions that fill a field no human can see so that simple spam bots do not reach the working group even though the arithmetic question is public.         | Medium   | Verified     |
| FR-033 | Format Description in the Browser | As an administrator, I want to format a description in the browser with headings, bold, italic, underlined and struck-through text, superscript and subscript, font family and size, text colour, ordered and unordered lists, paragraph alignment, line height, tables, links, inline pictures and a source-code view so that I can lay out an announcement without writing HTML by hand. | High     | Implemented  |
| FR-034 | Understand the Site on Arrival | As a visitor arriving on a link shared in a chat, I want the start page to tell me in one screen what this site offers and lead me on to events, voluntary service and the working group so that I can decide to stay before scrolling. | High     | Verified     |
| FR-035 | Scan a List Before Reading     | As a visitor, I want every entry in the event and offer lists reduced to its title, date, place, a short excerpt and one action so that I can compare several entries on a phone screen instead of scrolling through full descriptions. | High     | Verified     |
| FR-036 | See How Urgent an Event Is     | As a visitor, I want each event to show how much time is left before it drops off the list so that I notice an opportunity that is about to pass.                     | Medium   | Verified     |
| FR-037 | Fill the Contact Form with Guidance | As a visitor, I want every field of the contact form to carry a visible label, an explanation of why it is asked and — when rejected — an error message at the field itself so that I can correct a mistake without hunting for it. | High     | Verified     |
| FR-038 | Tell Visitor and Maintenance Actions Apart | As a visitor, I want maintenance controls, when an administrator session shows them, presented as a clearly separate and quieter group so that the action meant for me stays the obvious one on the page. | Medium   | Verified     |
| FR-039 | Find a Link on the Information Page | As a visitor, I want the collected external sources grouped by channel and collapsible on a phone so that I can find one link without scrolling past fifty.          | Medium   | Verified     |

**Note on FR-006, FR-013 — method change.** Both deletions keep the confirmation page, but the
confirmed deletion itself is submitted as a form `POST` and no longer as a link `GET` (NFR-021).

**Note on FR-029 — conflict.** The `EXCHANGE` table exists in the schema and a display fragment
exists in the user interface, but no application code reads or writes it and no use case describes
it (`docs/entity_model.md`). Either FR-029 is taken into scope and a use case written, or the table
and fragment are removed. This must be resolved by the product owner; the requirement is recorded
rather than dropped so the decision is not lost.

**Note on FR-033 — why it is written down now.** The formatting the editor offers had never been
recorded as a requirement; it existed only as a toolbar configuration. It is stated here because it
is the acceptance criterion for exchanging the editor component (NFR-035): a replacement that drops
tables, colours or the source view would silently reduce what administrators can publish. The list
matches what the stored-value allow-list accepts (`docs/entity_model.md`, NFR-010) — offering a
button whose result the sanitiser discards is a defect, not a feature.

**Note on FR-010 — status corrected from Verified to Open.** `organizationLink` is captured by the
administrator form and validated (`docs/entity_model.md`), but no template renders it: the public
offer list shows organisation, location and description and nothing else. The requirement was never
delivered, so its status was wrong rather than its content. The redesign delivers it
(`docs/redesign_plan.md` §4) together with the regression test that was missing.

**Note on FR-023 — amended, not dropped.** The requirement previously demanded that the start page
*redirect* to the event list, which is how `IndexResource.index()` answers `/` today. FR-034 gives
the start page content of its own; FR-023 is therefore restated as what actually matters — that the
event list stays one step away and keeps its own stable address `/anlaesse`. No external link
breaks: `/anlaesse` is unchanged and the redirect is replaced, not moved.

**Note on FR-034 — covered by its own use case.** Entering the platform became a flow of its own
once the start page stopped forwarding, so it is specified as UC-008 *Enter the Platform* rather
than folded into UC-007. UC-007 keeps the information pages; its BR-025, which made the event list
the entry point of the site, is replaced accordingly.

**Note on FR-034 … FR-039 — why presentation is written as requirements.** The platform's audience
is 15–30 and arrives on a phone through a shared link (`docs/vision.md` §3), and the feedback that
triggered this revision is that the site is unattractive — naming the event list, the offer list and
the contact form. Presentation is therefore not decoration here; it decides whether the content is
read at all. These six requirements state the *outcomes* the redesign must produce. How they are
produced is `docs/ux_concept.md`; when, is `docs/redesign_plan.md`.

**Note on FR-030.** The only documented way an administrator account comes into existence today is
the demo seed (`admin`/`admin`). No use case covers account management. The requirement is stated
as the gap it is; scope and role model need confirmation.

## Non-Functional Requirements

| ID      | Title                          | Requirement                                                                                                                                              | Category        | Priority | Status       |
|---------|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|----------|--------------|
| NFR-001 | Public Page Response Time      | Every public page (event list, event detail, offer list, information pages) responds within 1000 ms at the 95th percentile on the production host. **(assumed)** | Performance     | High     | Open         |
| NFR-002 | Concurrent Visitors            | The system serves 50 concurrent visitors with a 95th-percentile page response time of at most 2000 ms. **(assumed)**                                          | Scalability     | Medium   | Open         |
| NFR-003 | Availability                   | The site is reachable at least 99.0 % of the minutes of each calendar month. **(assumed)**                                                                    | Availability    | Medium   | Open         |
| NFR-004 | Startup Readiness              | After the container starts, the application answers HTTP requests within 10 seconds, including Flyway schema migration. **(assumed)**                         | Availability    | Medium   | Open         |
| NFR-005 | Password Storage               | Passwords are stored exclusively as bcrypt hashes; 0 code paths write a password or a password hash to a log, a rendered page or an object's `toString()` (BR-022). | Security        | High     | Implemented  |
| NFR-006 | Write Access Authorisation     | 100 % of endpoints that create, change or delete data reject requests without the `admin` role server-side, verified by an automated test per endpoint (BR-004, BR-014, BR-021). | Security        | High     | Verified     |
| NFR-007 | Session Cookie Protection      | The session cookie is flagged HTTP-only, `Secure` and `SameSite=Strict`, and is encrypted with a key supplied per environment; the repository contains 0 keys usable in production, and a production start without a supplied key aborts instead of falling back. | Security        | High     | Implemented  |
| NFR-008 | Transport Encryption           | 100 % of production traffic is served over HTTPS with TLS 1.2 or higher; plain HTTP requests are redirected to HTTPS in a single hop. **(assumed)**           | Security        | High     | Implemented  |
| NFR-009 | Spam Submission Rejection      | The contact form rejects 100 % of submissions whose spam protection field is empty or differs from the expected value (BR-017).                               | Security        | High     | Verified     |
| NFR-010 | Rich Text Sanitisation         | HTML submitted through the rich-text editor is reduced to a documented allow-list of tags, attributes and URL schemes before it is stored, so that 0 stored descriptions can execute script when rendered unescaped. | Security        | High     | Verified     |
| NFR-011 | Cross-Site Request Forgery     | 100 % of state-changing form submissions are rejected with HTTP 400 unless they carry a CSRF token matching the token cookie of the same browser session.     | Security        | High     | Verified     |
| NFR-012 | Interface Language and Locale  | 100 % of visitor-facing text is German and all dates and numbers are formatted for the `de-CH` locale.                                                        | Usability       | High     | Implemented  |
| NFR-013 | Mobile Layout                  | At viewport widths of 320, 360, 768, 1024, 1440 and 1920 px, every page renders with 0 horizontal scrolling and 0 rendered text below 14 px; content wider than the viewport (tables and images inside descriptions) scrolls inside its own container instead of the page. | Usability       | High     | Verified     |
| NFR-014 | Accessibility                  | All public pages conform to WCAG 2.1 level AA; in particular every text and interface element reaches a contrast ratio of at least 4.5 : 1 (3 : 1 for text at 24 px or above), every form control has a programmatically associated visible label, and every function is reachable by keyboard. | Usability       | High     | Verified     |
| NFR-015 | Atomic Content Changes         | A failed create, change or delete leaves the stored data exactly as it was before the attempt — 0 partially written records.                                  | Maintainability | High     | Verified     |
| NFR-016 | E-Mail Failure Isolation       | A failed e-mail notification never fails the visitor's request: the visitor still receives the confirmation and the failure is logged at level ERROR (BR-018). | Availability    | High     | Implemented  |
| NFR-017 | Automated Test Coverage        | The build fails when line coverage over the merged unit and e2e runs falls below 80 %.                                                                        | Maintainability | High     | Verified     |
| NFR-018 | Automatic Schema Migration     | Schema migrations are applied automatically at startup with 0 manual steps, and the applied schema version is readable from the version page (BR-027).         | Maintainability | High     | Implemented  |
| NFR-019 | Deployment Footprint           | The application deploys as a single OCI image needing only a mounted SQLite file and environment variables — 0 additional services required.                   | Portability     | High     | Implemented  |
| NFR-020 | Backup and Restore             | The database file is backed up at least daily and a backup can be restored within 4 hours, with at most 24 hours of data loss. **(assumed)**                   | Availability    | Medium   | Open         |
| NFR-021 | Safe HTTP Methods              | 0 endpoints change stored data in response to a `GET`, `HEAD` or `OPTIONS` request; every create, change and delete is reachable only by `POST`.               | Security        | High     | Verified     |
| NFR-022 | No Seeded Credentials in Production | The demo seed does not run under the `prod` build profile, so a production database receives 0 accounts with credentials that appear in the repository.    | Security        | High     | Implemented  |
| NFR-023 | Transport Security Headers     | Every response carries `Strict-Transport-Security` with a `max-age` of at least 31 536 000 seconds, so a browser that has seen the site once sends 0 further plain-HTTP requests to it. | Security        | High     | Verified     |
| NFR-024 | Content Security Policy        | Every response carries `Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options` and `Referrer-Policy`; the policy contains 0 occurrences of `unsafe-inline` in `script-src`, and 0 inline `<script>` blocks remain in the templates. | Security        | High     | Verified     |
| NFR-025 | Authentication Attempt Logging | 100 % of failed and successful sign-in attempts are logged with user name, source address and outcome, at level WARN for failures.                             | Security        | Medium   | Implemented  |
| NFR-026 | Request Rate Limiting          | The reverse proxy limits the sign-in endpoint to at most 5 requests per source address per 10 seconds and the contact form to at most 1 request per source address per 10 seconds, answering excess requests with HTTP 429. | Security        | Medium   | Implemented  |
| NFR-027 | Persisted Text Field Limits    | Every persisted text field carries an explicit maximum length that is validated server-side before storage — at most 255 characters for single-line fields, 65 535 for descriptions and 5 000 for a contact message. | Security        | Medium   | Verified     |
| NFR-028 | Log Data Minimisation          | Application logs contain 0 contact message bodies and 0 unescaped line breaks originating from user input; a stored message is referenced by its database id instead. | Security        | Medium   | Implemented  |
| NFR-029 | Session Lifetime               | An administrator session expires at most 30 minutes after the last request, limiting the usefulness of a captured session cookie.                              | Security        | Medium   | Implemented  |
| NFR-030 | Managed Frontend Dependencies  | 0 executable frontend libraries (scripts, stylesheets belonging to a library) are checked into the repository as binaries; all are resolved as build dependencies so the existing dependency scanner reports their vulnerabilities. Static brand assets — the logo, icons and web font files — are not libraries and are held in the repository. | Security        | Medium   | Implemented  |
| NFR-031 | Access Log Minimisation        | Client IP addresses are logged at level DEBUG only, so a production system running at INFO records 0 IP addresses in the application log.                      | Security        | Low      | Implemented  |
| NFR-032 | Toolchain Parity Local and CI  | The container image used by the development tooling and the build pipeline provision the same JDK major version, so a contributor and the pipeline compile with 0 differing toolchain versions.                                  | Maintainability | Medium   | Implemented  |
| NFR-033 | Host File Ownership            | Every file a tooling container writes into the working copy (`target/`, test reports, traces) is owned by the invoking host user — 0 root-owned artefacts that the IDE, git or a later run cannot delete.                        | Maintainability | Medium   | Implemented  |
| NFR-034 | Warm Dependency Cache          | The tooling reuses the host Maven repository and the downloaded browser binary across invocations, so a repeated `verify` on an unchanged working copy downloads 0 dependencies and 0 browsers.                                  | Maintainability | Medium   | Implemented  |
| NFR-035 | Frontend Dependency Maintenance | Every frontend library delivered to the browser has an upstream release not older than 12 months at the time of a release build, so that a reported vulnerability can be answered by upgrading instead of by patching a dead component locally. | Security        | High     | Open         |
| NFR-036 | Editor Assets Only Where Edited | The pages reachable without an administrator session (start, event list, event detail, offer list, information pages, contact form, sign-in) transfer 0 bytes of rich-text editor script and stylesheet; those assets are requested only by the two administrator form pages. | Performance     | Medium   | Verified     |
| NFR-037 | Single Design System           | Every colour, type size, spacing step, corner radius and shadow used by the interface is declared once as a custom property in a single token stylesheet; 0 templates and 0 component stylesheets contain a literal colour value or a hard-coded font size. | Maintainability | High     | Verified     |
| NFR-038 | Touch Target Size              | At a 360 px viewport, 100 % of interactive elements (links, buttons, form controls, disclosure summaries) present a hit area of at least 44 × 44 CSS pixels.       | Usability       | High     | Verified     |
| NFR-039 | Visible Keyboard Focus         | 100 % of focusable elements show a focus indicator with a contrast ratio of at least 3 : 1 against the adjacent background; 0 stylesheet rules suppress the focus outline without replacing it. | Usability       | High     | Verified     |
| NFR-040 | Reduced Motion                 | When the browser reports `prefers-reduced-motion: reduce`, 100 % of transitions and animations are disabled; 0 animations are required to understand or operate the interface. | Usability       | Medium   | Verified     |
| NFR-041 | Stylesheet and Font Budget     | A public page transfers at most 24 KB of stylesheet (gzipped) and at most 90 KB of web fonts, and executes 0 bytes of JavaScript; the complete first view stays below 150 KB. | Performance     | High     | Verified     |
| NFR-042 | No Third-Party Asset Requests  | A public page issues 0 requests to hosts other than the application's own origin — fonts, styles, scripts and images are served from it; this keeps the Content-Security-Policy of NFR-024 free of external sources and sends 0 visitor IP addresses to third parties. | Security        | High     | Verified     |
| NFR-043 | Consistent Page Structure      | 100 % of pages render exactly one `h1`, a heading order with no skipped level, a skip link as the first focusable element, and the data protection link reachable without scrolling the navigation. | Usability       | Medium   | Verified     |
| NFR-044 | Brand Conformity               | 100 % of brand colours and typefaces in the interface are those defined by the Cevi Schweiz Corporate Design Manual (2020): Cevi-Rot `#C41333`, Cevi-Blau `#323394`, Cevi-Schwarz `#141412`, Montserrat for titles and interface text, Lora for body copy; 0 further brand hues are introduced. | Usability       | Medium   | Implemented  |
| NFR-045 | Dark Colour Scheme             | When the browser reports `prefers-color-scheme: dark`, every page renders in a dark palette that meets the same contrast thresholds as NFR-014, implemented solely by redefining the tokens of NFR-037. | Usability       | Low      | Open         |

**Note on NFR-035 — the requirement the current editor fails.** Summernote 0.9.1 is the newest
release and dates from October 2024; the jQuery 4 incompatibility reported against it in February
2026 is still open (`docs/jquery4_evaluation.md`). The component therefore cannot satisfy NFR-035
and is to be replaced by a maintained, dependency-free editor, which also removes jQuery from the
application. NFR-035 is stated as a standing rule rather than as a one-off migration task so that
the next component going quiet is caught by the same check.

**Note on NFR-030, NFR-035, C-018 — why the web fonts are not treated as libraries.** NFR-044
requires Montserrat and Lora, and NFR-042 forbids loading them from Google Fonts. That leaves the
files in the repository, which reads at first glance like a breach of NFR-030. It is not: NFR-030
exists so that a *dependency scanner* can report vulnerabilities in code the browser executes. A
`woff2` file is data, not code, and no scanner tracks it — the rule would gain nothing and the
alternative is worse. It was also checked whether the fonts could come from a WebJar as Jodit does:
`org.webjars.npm:fontsource__montserrat` last released in 2022 and would itself violate NFR-035,
and no Lora WebJar exists at all. NFR-030 is therefore narrowed to executable libraries, NFR-035
does not apply to static brand assets, and C-018 is extended to admit SIL OFL-1.1 for fonts. The
same reasoning already covers `logo.png`, which has been in the repository since the beginning.

**Note on NFR-041 — the budget is a ceiling on a redesign, not on the site.** Today a public page
transfers roughly 1.5 KB of CSS and no fonts, which is why it looks unstyled. The budget concedes
the cost of a real design system and two typefaces, and refuses everything beyond it: no CSS
framework, no icon font, no JavaScript on public pages. It is the requirement that keeps a
redesign from becoming a page-weight regression, and it is measured, not estimated.

**Note on NFR-041 — the stylesheet ceiling was raised from 12 KB to 16 KB, on measurement.** The
12 KB was estimated before a line of the design system existed. The finished token layer, frame,
components and prose rules measure 13.8 KB gzipped, comments included — those comments are the
reason the next contributor can tell a decision from an accident, and they are worth their share of
the transfer. The ceiling keeps its purpose either way: Bootstrap alone is roughly twice it, so
NFR-041 still forbids a framework (C-020) and still leaves room for phases 3 and 4. The figure that
did not move is the one that matters to a visitor on mobile data — the complete first view stays
under 150 KB.

**Note on NFR-041 — the stylesheet ceiling was raised again, from 16 KB to 24 KB, after phase 4.**
The 16 KB was set when phases 3 and 4 were still unwritten; the finished interface — design system,
frame, cards, prose, forms, status pages, start page and information pages — measures 16.5 KB
gzipped with its comments. The product owner decided that the stylesheet's own size is not a figure
worth optimising against at this scale, and 24 KB leaves the dark scheme of NFR-045 and the phase-5
work room without another revision. What the ceiling is actually for is unchanged: it still forbids
a CSS framework (C-020) — Bootstrap alone is about 27 KB gzipped before a single override — and the
figure that matters to a visitor on mobile data, the 150 KB first view, has not moved.

**Note on NFR-041 — measured against the delivered font files.** The two typefaces were subset and
placed in the repository while this revision was written. Split by `unicode-range`, a German page
loads 69.9 KB of font (Montserrat Latin 34.3 KB + Lora Latin 35.5 KB); the Latin-Extended and
italic faces are fetched only when a page actually renders such a character, which keeps place
names like Poděbrady correct without charging every visitor for them. The 90 KB ceiling therefore
holds with room to spare, and the figure in it is now an observation rather than an estimate.

**Note on NFR-045 — deliberately Low.** A dark scheme costs about twelve token redefinitions once
NFR-037 exists, and nothing else. It is stated so that the token layer is built to allow it, and
prioritised Low so that it never blocks the redesign.

**Note on NFR-013, NFR-014 — no longer assumptions.** Both thresholds were marked *(assumed)* since
revision 1 and never verified, which is how the current interface came to fail them. They are now
stated as measurable conditions and are verified by browser tests (`docs/redesign_plan.md` §7)
rather than by inspection.

**Note on NFR-013, NFR-014, NFR-038 … NFR-041 — what "Verified" covers, as of phase 5.** Each is
checked by a real browser on every public route, and each found something (`docs/redesign_plan.md`
§7). What the tests assert:

| Requirement | Checked by | What is not machine-checked |
|---|---|---|
| NFR-013 | `ResponsiveLayoutE2ETest` at all six widths, `TypographyFloorE2ETest` at 320 and 360 px | — |
| NFR-014 | `ContrastE2ETest` (contrast), `FocusVisibleE2ETest` (focus order, skip link), `NavigationDrawerE2ETest` (keyboard operation), `ContactResourceTest` (labels) | The judgement clauses of WCAG 2.1 AA — sensible link text, a heading structure that describes the page, an error message a person can act on. Those stay a review question. |
| NFR-038 | `TapTargetE2ETest` at 360 px, signed in and signed out | — |
| NFR-039 | `FocusVisibleE2ETest`, 40 tab stops per route | The 3 : 1 ratio of the ring itself, which is a single token (`--cevi-blue` on `--surface`, 10.3 : 1) rather than a per-element measurement. |
| NFR-040 | `ReducedMotionE2ETest`, with the counter-check that the same pages do animate by default | — |
| NFR-041 | `CssBudgetTest` (stylesheet bytes, gzipped), `StaticAssetTest` (font bytes), `EditorAssetsTest` (no script on a public page) | The 150 KB first view as a whole, which no test currently sums. |

A "Verified" here therefore means *the stated numbers are enforced by the build*, not that the
interface is beyond criticism. The review with the product owner on a real phone remains part of
the phase.

**Note on the phase-6 status pass (2026-08-26).** Every requirement the redesign delivered was
re-read against the tests that exist rather than against the code that was written. Three results
are worth recording:

- **NFR-037 was not true as written.** It says 0 component stylesheets contain a literal colour or
  a hard-coded font size. `CssBudgetTest` had only ever checked the first half of the sentence —
  that no second file declares `:root` variables — and six literal colours had accumulated across
  five files by the end of phase 4. None was wrong to look at; each was a value that could no
  longer be changed in one place. They are now three tokens (`--ink-soft`, `--on-dark`,
  `--border-tinted`), and the test checks the whole sentence.
- **NFR-042 had no test at all.** It is a data-protection requirement before it is a performance
  one, and it was resting on the fact that nobody had yet written a Google Fonts `<link>`.
  `NoThirdPartyAssetsTest` now checks every public page and every stylesheet.
- **NFR-044 and C-019 are *Implemented*, not *Verified*.** The palette and the typefaces are the
  Corporate Design Manual's, and `tokens.css` states and measures them — but the open question in
  §10 of `docs/ux_concept.md`, whether Lora is accepted as the body face, is a working-group
  decision that no test can stand in for.

**Note on NFR-036 — why it appears with the editor exchange.** Today the editor is loaded from
`base.qute.html` on every page, so every visitor downloads it although only two administrator pages
use it. A maintained editor is larger than the current one; loading it where it is used keeps the
public pages faster than they are today instead of slower.

## Constraints

| ID    | Title                        | Constraint                                                                                                                    | Category    | Priority | Status      |
|-------|------------------------------|---------------------------------------------------------------------------------------------------------------------------------|-------------|----------|-------------|
| C-001 | Runtime Platform             | The application must run on Java 25 and the Quarkus 3.x platform.                                                               | Technical   | High     | Implemented |
| C-002 | Database Platform            | The application must use a single file-based SQLite database; no separate database server may be required.                      | Technical   | High     | Implemented |
| C-003 | Schema Migration Tool        | All schema changes must be delivered as versioned Flyway scripts under `src/main/resources/db/migration`.                        | Technical   | High     | Implemented |
| C-004 | Server-Side Rendering        | Pages must be rendered server-side with Qute; no single-page-application framework may be introduced.                           | Technical   | Medium   | Implemented |
| C-005 | Interface Language           | The visitor-facing interface must be German (`de-CH`); no further languages are offered in this release.                        | Business    | High     | Implemented |
| C-006 | Browser Support              | The interface must work on the latest two versions of Chrome, Firefox, Safari and Edge, on desktop and mobile. **(assumed)**     | Technical   | High     | Open        |
| C-007 | Build and Delivery Pipeline  | Every push to `main` must be built and tested by GitHub Actions, which publishes the OCI image to `ghcr.io` and `registry.cevi.tools`. | Operational | High     | Implemented |
| C-008 | Secret Handling              | Session encryption key, mail password, initial administrator password and database URL must be supplied as deployment secrets; no production secret may be committed to the repository, and a missing secret must abort the container start rather than fall back to a built-in value. | Operational | High     | Implemented |
| C-009 | No Licence Budget            | Only free and open-source components may be used; the project has no budget for paid licences or paid hosting services.         | Business    | High     | Implemented |
| C-010 | Volunteer Operation          | Routine operation (publishing content, deploying a release, checking the version) must be possible without specialist database or system administration knowledge. | Business    | High     | Implemented |
| C-011 | Data Minimisation            | The contact form must not request personal data beyond the free-text message; a Swiss data protection statement must be reachable from every page. | Regulatory  | High     | Implemented |
| C-012 | Single Administrator Role    | The authorisation model is limited to one role, `admin`; finer-grained roles are out of scope for this release.                 | Technical   | Medium   | Implemented |
| C-013 | REST Layer                   | HTTP endpoints must be built on Quarkus REST; the deprecated RESTEasy Classic stack must not be used, because the CSRF extension required by NFR-011 is only available for Quarkus REST. | Technical   | High     | Implemented |
| C-014 | Container Hardening          | The container must run as a non-root user with all Linux capabilities dropped, `no-new-privileges` set, a read-only root filesystem, explicit CPU/memory limits, and a mount narrowed to the database directory. | Operational | Medium   | Implemented |
| C-015 | Documented Production Deployment | Every production-only setting (secrets, rate limiting, TLS termination, backup, log level) must be documented in `docs/deployment.md` so that an operator can reproduce the deployment without reading application code. | Operational | High     | Implemented |
| C-016 | Container-Based Development Tooling | Every local development command (compile, unit tests, `verify` incl. the Playwright e2e tests, dev mode, arbitrary Maven goals) must be executable through a single wrapper script `tooling/docker.sh` that runs it inside a container; a contributor must need only Docker and git — no locally installed JDK, Maven or browser. | Operational | Medium   | Implemented |
| C-017 | No Frontend Build Step       | Frontend libraries must be consumable as prebuilt browser bundles resolved as Maven dependencies; the build must not require Node.js, npm or a JavaScript bundler. | Technical   | High     | Implemented |
| C-018 | Permissive Frontend Licences | Every frontend library delivered to a visitor's browser must be under a permissive licence (MIT, BSD or Apache-2.0), and every web font under a permissive font licence (SIL OFL-1.1, MIT, Apache-2.0); copyleft-licensed components must not be shipped, so that publishing the application never depends on satisfying copyleft obligations. | Regulatory  | High     | Implemented |
| C-019 | Corporate Design Conformity  | The interface must follow the Cevi Schweiz Corporate Design Manual (2020) for logo, brand colours and typefaces; no alternative brand identity may be introduced for this platform.                                        | Business    | High     | Implemented |
| C-020 | No CSS Framework             | The visual layer must be built from the project's own stylesheets; no CSS framework may be introduced, because every framework would have to be overridden to reach C-019, adds a dependency subject to NFR-035, and costs more transferred bytes than NFR-041 allows. | Technical   | Medium   | Verified    |
| C-021 | No Client-Side Scripting on Public Pages | Navigation, disclosure and layout on pages reachable without an administrator session must work without JavaScript; scripting may be delivered only to the two administrator form pages that host the rich-text editor. | Technical   | High     | Verified    |

**Note on C-019, C-020, C-021 — decisions taken with the product owner on 2026-08-25.** A CSS
framework, photographic imagery per record and a deeper restructuring of the navigation were all
offered and all declined in favour of the narrower option: own design tokens, a graphic rather than
photographic visual system, and a new start page with the existing page structure otherwise intact.
C-021 records what has in fact been true since the editor was scoped to the two form pages
(NFR-036); it is stated now because the redesign is where a JavaScript-driven menu or carousel
would otherwise creep in. C-019 makes the brand binding, which also settles which fonts NFR-044
names.

**Note on C-017, C-018 — what they rule out.** Both were applied implicitly and are recorded now
because they decide the editor replacement: C-017 excludes CKEditor 5 and TipTap, which are
distributed as ES modules meant for a bundler, and C-018 excludes TinyMCE, which has been GPLv2+
since version 7 and refuses to start self-hosted unless the application declares GPL use. C-018 is
narrower than C-009, which only demands that a component be free of charge.

## Open Points for Confirmation

The following thresholds and scope decisions are proposals, not facts derived from the existing
documents. Please confirm or replace them:

| Ref                      | Question                                                                                      |
|--------------------------|-----------------------------------------------------------------------------------------------|
| FR-029                   | Complete exchange offers, or drop the `EXCHANGE` table and its display fragment?               |
| FR-030                   | Should administrators manage accounts themselves, or does the operator do it at deployment level? |
| NFR-001, NFR-002         | Are 1000 ms p95 and 50 concurrent visitors the right targets for the expected audience?         |
| NFR-003, NFR-004, NFR-020| Which availability, startup and backup expectations are binding — the platform has no monitoring or documented backup today? |
| NFR-026                  | Are 5 sign-in attempts and 1 contact submission per 10 seconds and source address the right limits? |
| C-006                    | Which browsers must be supported, and down to which version?                                    |
| FR-034                   | Should the working group supply the start page's headline and lead copy, or is a proposal enough? |
| FR-035                   | Is a 160-character excerpt in the list acceptable to the administrators who write the descriptions? |
| NFR-044                  | Is Lora as body typeface accepted, or should body copy use the CD's third face (Helvetica Neue / system sans)? |
| NFR-045                  | Dark colour scheme in this redesign, or a later one?                                            |

**Decided since revision 4.** NFR-013 and NFR-014 are binding, not aspirational, and are verified by
browser tests. C-019/C-020: the Cevi Corporate Design is binding and no CSS framework is
introduced. The visual system is graphic rather than photographic — no image field is added to
`EVENT` or `VOLUNTARY_SERVICE` (`docs/entity_model.md` is unchanged by this revision). FR-023 is
amended rather than removed. FR-010's status was corrected to Open after review of the templates.

**Decided since revision 1.** NFR-008: TLS is terminated by the surrounding Traefik infrastructure;
the application contributes the HSTS header and the `Secure` cookie flag (NFR-007, NFR-023).
NFR-010: rich text is sanitised server-side, because CSRF (NFR-011) lets an unauthenticated attacker
reach the administrator-only write path. FR-030/FR-031: the initial account comes from a deployment
secret; in-application account management stays open.

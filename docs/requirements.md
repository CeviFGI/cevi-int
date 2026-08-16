# Requirements: Cevi International Platform

**Document status:** Draft for review
**Date:** 2026-08-13
**Source:** `docs/vision.md`, `docs/use_cases/UC-001` … `UC-007`, `docs/entity_model.md`

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
| FR-022 | Hide Maintenance Links         | As a visitor, I want edit and delete links hidden while nobody is signed in so that the public pages show only what concerns me.                                      | Medium   | Implemented  |
| FR-023 | Enter Site via Event List      | As a visitor, I want the start page to lead directly to the list of upcoming events so that I reach the main content without navigating.                              | High     | Verified     |
| FR-024 | View Working Group Presentation| As a visitor, I want a page presenting the international working group so that I know who is behind the platform before I contact them.                               | Medium   | Implemented  |
| FR-025 | View Data Protection Page      | As a visitor, I want the data protection information reachable from every page so that I can check how my data is handled before I write a message.                   | High     | Implemented  |
| FR-026 | View Version Page              | As an operator, I want a page naming the application version and the database schema version so that I can tell whether a deployment and its migrations match.        | Medium   | Implemented  |
| FR-027 | Show Not-Found Page            | As a visitor, I want an explanatory page with a way back when I open an unknown address so that a stale link does not end my visit.                                   | Medium   | Verified     |
| FR-028 | Show General Error Page        | As a visitor, I want a plain error page when the system cannot produce the requested page so that I never see technical internals.                                    | Medium   | Implemented  |
| FR-029 | Browse Exchange Offers         | As a visitor, I want to see exchange opportunities with partner organisations so that I can consider an exchange alongside events and voluntary service.              | Low      | Needs review |
| FR-030 | Manage User Accounts           | As an operator, I want to create administrator accounts and let administrators change their own password so that access does not depend on a seeded demo account.     | Medium   | Open         |

**Note on FR-029 — conflict.** The `EXCHANGE` table exists in the schema and a display fragment
exists in the user interface, but no application code reads or writes it and no use case describes
it (`docs/entity_model.md`). Either FR-029 is taken into scope and a use case written, or the table
and fragment are removed. This must be resolved by the product owner; the requirement is recorded
rather than dropped so the decision is not lost.

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
| NFR-005 | Password Storage               | Passwords are stored exclusively as bcrypt hashes; no code path stores or logs a password in readable form (BR-022).                                          | Security        | High     | Implemented  |
| NFR-006 | Write Access Authorisation     | 100 % of endpoints that create, change or delete data reject requests without the `admin` role server-side, verified by an automated test per endpoint (BR-004, BR-014, BR-021). | Security        | High     | Verified     |
| NFR-007 | Session Cookie Protection      | The session cookie is flagged HTTP-only and encrypted with a key supplied per environment; no default key is used in production.                              | Security        | High     | Implemented  |
| NFR-008 | Transport Encryption           | 100 % of production traffic is served over HTTPS with TLS 1.2 or higher; plain HTTP requests are redirected to HTTPS in a single hop. **(assumed)**           | Security        | High     | Open         |
| NFR-009 | Spam Submission Rejection      | The contact form rejects 100 % of submissions whose spam protection field is empty or differs from the expected value (BR-017).                               | Security        | High     | Verified     |
| NFR-010 | Rich Text Sanitisation         | HTML submitted through the rich-text editor is reduced to a documented allow-list of tags and attributes before it is stored, so no submitted markup can execute script. **(assumed scope)** | Security        | High     | Open         |
| NFR-011 | Cross-Site Request Forgery     | 100 % of state-changing form submissions are rejected without a valid per-session CSRF token.                                                                 | Security        | High     | Open         |
| NFR-012 | Interface Language and Locale  | 100 % of visitor-facing text is German and all dates and numbers are formatted for the `de-CH` locale.                                                        | Usability       | High     | Implemented  |
| NFR-013 | Mobile Layout                  | All pages are usable between 320 px and 1920 px viewport width with no horizontal scrolling and no text smaller than 14 px. **(assumed)**                     | Usability       | High     | Open         |
| NFR-014 | Accessibility                  | All public pages conform to WCAG 2.1 level AA. **(assumed)**                                                                                                  | Usability       | Medium   | Open         |
| NFR-015 | Atomic Content Changes         | A failed create, change or delete leaves the stored data exactly as it was before the attempt — 0 partially written records.                                  | Maintainability | High     | Verified     |
| NFR-016 | E-Mail Failure Isolation       | A failed e-mail notification never fails the visitor's request: the visitor still receives the confirmation and the failure is logged at level ERROR (BR-018). | Availability    | High     | Implemented  |
| NFR-017 | Automated Test Coverage        | The build fails when line coverage over the merged unit and e2e runs falls below 80 %.                                                                        | Maintainability | High     | Verified     |
| NFR-018 | Automatic Schema Migration     | Schema migrations are applied automatically at startup with 0 manual steps, and the applied schema version is readable from the version page (BR-027).         | Maintainability | High     | Implemented  |
| NFR-019 | Deployment Footprint           | The application deploys as a single OCI image needing only a mounted SQLite file and environment variables — 0 additional services required.                   | Portability     | High     | Implemented  |
| NFR-020 | Backup and Restore             | The database file is backed up at least daily and a backup can be restored within 4 hours, with at most 24 hours of data loss. **(assumed)**                   | Availability    | Medium   | Open         |

## Constraints

| ID    | Title                        | Constraint                                                                                                                    | Category    | Priority | Status      |
|-------|------------------------------|---------------------------------------------------------------------------------------------------------------------------------|-------------|----------|-------------|
| C-001 | Runtime Platform             | The application must run on Java 21 and the Quarkus 3.x platform.                                                               | Technical   | High     | Implemented |
| C-002 | Database Platform            | The application must use a single file-based SQLite database; no separate database server may be required.                      | Technical   | High     | Implemented |
| C-003 | Schema Migration Tool        | All schema changes must be delivered as versioned Flyway scripts under `src/main/resources/db/migration`.                        | Technical   | High     | Implemented |
| C-004 | Server-Side Rendering        | Pages must be rendered server-side with Qute; no single-page-application framework may be introduced.                           | Technical   | Medium   | Implemented |
| C-005 | Interface Language           | The visitor-facing interface must be German (`de-CH`); no further languages are offered in this release.                        | Business    | High     | Implemented |
| C-006 | Browser Support              | The interface must work on the latest two versions of Chrome, Firefox, Safari and Edge, on desktop and mobile. **(assumed)**     | Technical   | High     | Open        |
| C-007 | Build and Delivery Pipeline  | Every push to `main` must be built and tested by GitHub Actions, which publishes the OCI image to `ghcr.io` and `registry.cevi.tools`. | Operational | High     | Implemented |
| C-008 | Secret Handling              | Session encryption key, mail configuration and database URL must be supplied as environment variables at deployment; no production secret may be committed to the repository. | Operational | High     | Open        |
| C-009 | No Licence Budget            | Only free and open-source components may be used; the project has no budget for paid licences or paid hosting services.         | Business    | High     | Implemented |
| C-010 | Volunteer Operation          | Routine operation (publishing content, deploying a release, checking the version) must be possible without specialist database or system administration knowledge. | Business    | High     | Implemented |
| C-011 | Data Minimisation            | The contact form must not request personal data beyond the free-text message; a Swiss data protection statement must be reachable from every page. | Regulatory  | High     | Implemented |
| C-012 | Single Administrator Role    | The authorisation model is limited to one role, `admin`; finer-grained roles are out of scope for this release.                 | Technical   | Medium   | Implemented |

## Open Points for Confirmation

The following thresholds and scope decisions are proposals, not facts derived from the existing
documents. Please confirm or replace them:

| Ref                      | Question                                                                                      |
|--------------------------|-----------------------------------------------------------------------------------------------|
| FR-029                   | Complete exchange offers, or drop the `EXCHANGE` table and its display fragment?               |
| FR-030                   | Should administrators manage accounts themselves, or does the operator do it at deployment level? |
| NFR-001, NFR-002         | Are 1000 ms p95 and 50 concurrent visitors the right targets for the expected audience?         |
| NFR-003, NFR-004, NFR-020| Which availability, startup and backup expectations are binding — the platform has no monitoring or documented backup today? |
| NFR-008                  | Is HTTPS termination in scope of this application, or of the surrounding infrastructure?        |
| NFR-010                  | Should submitted rich text be sanitised even though only administrators can enter it?           |
| NFR-013, NFR-014         | Are the mobile breakpoints and WCAG 2.1 AA binding, or aspirational?                            |
| C-006                    | Which browsers must be supported, and down to which version?                                    |

# Vision: Cevi International Platform

**Document status:** Draft for review
**Date:** 2026-08-13
**Derived from:** `docs/use_cases.puml`, `docs/use_cases/UC-001` … `UC-007`, `docs/entity_model.md`

## 1. Purpose

Cevi International (`international.cevi.tools`) is the public web presence of the international
working group of Cevi Schweiz. It answers one question for a young person in a Swiss Cevi:
*what can I actually do internationally, and who do I talk to about it?*

The platform gathers the international opportunities that are otherwise scattered across
newsletters, chat groups and personal contacts — international events, voluntary service abroad —
and puts them on one page that is current, publicly readable and reachable by a shareable link.
A contact form connects the interested visitor to a real person in the working group.

## 2. Problem Statement

| Problem                                                                                  | Consequence today                                            |
|------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| International events are announced through informal channels only.                        | Interested members hear about an event after registration closed, or not at all. |
| Voluntary service opportunities are known to a few working group members.                 | Knowledge is lost when a member leaves the working group.     |
| There is no low-threshold way to reach the working group.                                 | Questions are never asked; interest does not turn into participation. |
| Announcements have no stable address.                                                     | A link forwarded in a chat cannot be trusted to still work.   |

## 3. Target Users

| Role                    | Description                                                                                  | Primary interest                                                        |
|-------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| Besucher (Visitor)      | Cevi member, mostly 15–30, German-speaking, browsing mainly on a phone. No account, no login. | Find upcoming events and voluntary service offers; ask a question.       |
| Administrator           | Member of the international working group who maintains the published content.                | Publish and correct announcements quickly, without technical assistance. |
| Operator (Betreiber)    | Person running the deployment (currently the same small circle as the administrators).        | Know which version and schema are live; keep the site available.         |
| Mail Service            | External e-mail infrastructure that delivers contact notifications.                           | (System actor — no interest of its own.)                                 |

The working group is a volunteer body. Every hour spent on the platform is an hour not spent on
international work, which shapes every goal below.

## 4. Product Goals

1. **One current overview.** A visitor sees, on the entry page, exactly the international events
   that are still ahead — nothing expired, nothing missing.
2. **Publishing without a gatekeeper.** A working group member can publish or correct an
   announcement themselves, from a browser, in a few minutes, without a developer.
3. **A reachable working group.** A visitor can send a question in one step without creating an
   account and without exposing an e-mail address to scrapers.
4. **Links that survive.** Every announcement has a stable, readable address that can be shared in
   a chat and still resolves months later.
5. **Cheap to keep running.** A single container, a single file-based database, automatic schema
   migration on start — an operator should not need a database administrator.

## 5. Scope

### In scope (implemented today)

| Capability                        | Use case |
|-----------------------------------|----------|
| Browse upcoming international events, with an own page per event | UC-001 |
| Maintain events (create, change, remove)                          | UC-002 |
| Browse voluntary service offers, each linking to its organisation | UC-003 |
| Maintain voluntary service offers                                 | UC-004 |
| Contact the working group by form, with spam protection and e-mail notification | UC-005 |
| Sign in and out as administrator                                  | UC-006 |
| Information pages (working group presentation, data protection) and a version page | UC-007 |

### Out of scope (deliberately not offered)

- **Registration for events.** The platform announces; registration happens with the organiser.
  Announcements link out rather than collect participants.
- **Applications for voluntary service.** The platform recommends organisations and links to them;
  it does not administer placements.
- **Visitor accounts.** Only administrators sign in. Visitors never register.
- **Sender data on the contact form.** No name, no e-mail field. A visitor who wants a reply
  writes their contact details into the message text (BR-020) — deliberate data minimisation.
- **Content in languages other than German.** The audience is German-speaking Swiss Cevi members.

### Undecided

- **Exchange offers.** The database holds an `EXCHANGE` table and the user interface holds a
  display fragment for it, but no code reads or writes it and no use case describes it. Either the
  feature is completed or the table is dropped — this is a product decision, recorded as a
  requirement flagged for review rather than silently resolved.

## 6. Quality Goals

| Goal                    | Why it matters here                                                                            |
|-------------------------|------------------------------------------------------------------------------------------------|
| **Mobile-first usability** | The typical visitor arrives from a phone through a link shared in a chat group.               |
| **Content integrity**   | A half-written event must never become visible; a rejected form must never lose what was typed. |
| **Security of the write path** | Anyone may read; only an authenticated administrator may change anything.                |
| **Operability by volunteers** | Deployment, migration and version checking must work without specialist knowledge.        |
| **Maintainability**     | The codebase is worked on sporadically; automated tests must catch regressions after months of no activity. |

## 7. Context and Constraints

- Runs on Quarkus (Java 25) with Qute templates, Panache/Hibernate ORM and Flyway migrations.
- Stores data in a single file-based SQLite database mounted into the container.
- Interface language and formatting locale are German/Swiss (`de-CH`).
- Deployed as an OCI image published to `ghcr.io` and `registry.cevi.tools`, built by GitHub Actions.
- Built and maintained by volunteers with no budget for licences or paid infrastructure.

## 8. Success Measures

| Measure                                                                    | Target                          |
|-----------------------------------------------------------------------------|---------------------------------|
| Expired events visible on the public list                                   | 0 at any time                   |
| Time for an administrator to publish a new event, from sign-in to visible   | ≤ 5 minutes                     |
| Announcements published without developer involvement                       | 100 %                           |
| Contact messages that reach the working group (recorded or mailed)          | 100 % of accepted submissions   |
| Automated line coverage enforced on every build                             | ≥ 80 %                          |

## 9. Open Questions

1. Should exchange offers be built out, or should the `EXCHANGE` table be dropped? (see §5)
2. What availability and backup expectations apply — the platform currently has no monitoring and
   no documented backup of the SQLite file.
3. Should administrators be able to manage accounts (create users, change passwords) themselves,
   or does the operator keep doing that at deployment level?
4. Which browsers and which accessibility level are binding? Both are assumed in
   `docs/requirements.md` and marked for confirmation.

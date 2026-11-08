# Glossary

**Source:** `docs/vision.md`, `docs/requirements.md`, `docs/entity_model.md`, `docs/use_cases.puml`,
`docs/use_cases/UC-001` … `UC-013`

One name per domain concept. The specifications are written in English; where the German
interface uses its own label, the definition names it so the screens can be matched to the terms.
A word in the **Avoid** column must not be used for that concept in requirements, use cases or
test cases.

## Roles

| Term          | Definition                                                                                                                                                                         | Avoid                    |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| Visitor       | Anyone reading the public pages without signing in, typically a Cevi member aged 15–30 on a phone. Named *Besucher* as the actor of the use case diagram. Never holds a User Account. | Guest, Customer          |
| Administrator | Member of the International Working Group who holds a User Account with the `admin` role and maintains Events and Voluntary Service Offers.                                          | Moderator, Maintainer    |
| Operator      | Person running the deployment who checks application and schema version on the Version Page; inherits from the Administrator because that page requires an Administrator Session.  | Sysadmin                 |
| Mail Service  | External e-mail infrastructure that delivers the E-Mail Notification; a system actor without goals of its own.                                                                      | Mail server, SMTP server |

## Organisation

| Term                         | Definition                                                                                                                                                         | Avoid             |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|
| Cevi                         | The Swiss YMCA/YWCA youth association (Cevi Schweiz) whose members form the audience of the platform and whose Corporate Design Manual fixes the brand colours.     |                   |
| International Working Group  | The volunteer body of Cevi Schweiz that runs the platform, publishes its content and receives Contact Messages; short form *working group*.                         | Team, Committee   |
| Organisation                 | An outside body that runs a voluntary service or an exchange; the Visitor applies there directly, never through the platform.                                       | Provider          |

## Content

| Term                    | Definition                                                                                                                                                                                  | Avoid                      |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| Event                   | An international Cevi gathering announced on the platform, with title, Date Text, location, Display Date and Description, and optionally a First Day and a Last Day. Labelled *Anlass* in the interface. Registration happens with the organiser, not here. | Camp                       |
| Upcoming Event          | An Event whose Display Date is today or later; only Upcoming Events appear on the Event List (UC-001 BR-001).                                                                                      | Current event, Future event |
| Date Text               | The free-text period of an Event as Visitors read it, e.g. "15. – 24. Oktober 2023"; never compared by the system.                                                                          |                            |
| Display Date            | The single calendar date until which an Event stays on the Event List; the system compares it with today and derives from it how much time is left (UC-001 BR-005).                               | Expiry date, End date      |
| First Day               | The optional calendar date on which an Event begins; together with the Last Day it forms the period of the Calendar Entry. Labelled *Erster Tag* in the event form. | Start date                 |
| Last Day                | The optional calendar date on which an Event ends, never before its First Day; independent of the Display Date, which decides only the listing. Labelled *Letzter Tag* in the event form. |                            |
| Calendar Entry          | An iCalendar file (`.ics`) delivered from an Event Page that holds the Event as one all-day entry from its First Day to its Last Day; the Visitor's device opens it in its calendar app or saves it (FR-047). | Calendar invite |
| Short Name              | The unique, readable identifier of an Event or a Voluntary Service Offer that forms its Public Address; derived when the Administrator supplies none — for an Event from its title (FR-007), for an offer from its Organisation and location (FR-044). Stored as the attribute `slug`. | Permalink                  |
| Public Address          | The stable, shareable URL of a page — for an Event or a Voluntary Service Offer built from its Short Name — that still resolves months later, as long as the Administrator does not change the Short Name (UC-001 BR-003, UC-004 BR-008). | Permalink                  |
| Voluntary Service Offer | A voluntary service opportunity abroad that an Organisation runs and the International Working Group recommends, with Organisation, Organisation Link, location and Description. Short form *offer*. | Job, Internship            |
| Organisation Link       | The `http://` or `https://` address of the website of the Organisation behind a Voluntary Service Offer.                                                                 |                            |
| Description             | The formatted text of an Event or offer, written in the rich-text editor and reduced to the Allow-List before it is stored.                                                                |                            |
| Allow-List              | The documented set of HTML tags, attributes and URL schemes a Description may keep; everything else is removed on the server before storage (NFR-010).                                     | Whitelist                  |
| Excerpt                 | At most 160 characters of a Description's visible text shown instead of the full text: in a list entry its beginning (UC-001 BR-004); on the Search Results Page the passage around the earliest occurrence of the Search Term, or the beginning when it does not occur there (UC-010 BR-008). | Teaser                     |
| Last Change             | The moment an Event or a Voluntary Service Offer was last created or changed by an Administrator; set by the system on every save that changes it, never entered by hand. Entries that existed before the News Feed carry the moment of the migration that introduced it. | Timestamp |

## Contact

| Term                     | Definition                                                                                                                                                              | Avoid               |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| Contact Form             | The public form through which a Visitor writes to the International Working Group without a User Account; it asks for no sender details (UC-005 BR-004).                       |                     |
| Contact Message          | The free text of at most 5 000 characters a Visitor submits through the Contact Form; recorded in the database and forwarded by E-Mail Notification (UC-005 BR-002, UC-005 BR-006).    | Inquiry             |
| Spam Protection Question | The visible arithmetic question on the Contact Form whose expected answer a submission must contain to be accepted (UC-005 BR-001).                                            | Captcha             |
| Hidden Field             | A Contact Form field no human can see; a submission that fills it is discarded as automated (UC-005 BR-005).                                                                    | Honeypot            |
| E-Mail Notification      | The e-mail the system sends through the Mail Service to one fixed recipient address for every accepted Contact Message (UC-005 BR-003); a failed delivery never fails the Visitor's submission. |                     |

## Access

| Term                  | Definition                                                                                                                                                                  | Avoid                   |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| User Account          | A stored identity with user name, bcrypt-hashed password and role that may sign in; only Administrators have one.                                                           |                         |
| Sign In               | Proving identity with user name and password to open an Administrator Session. The interface page is the login page at `/auth/login`.                                      | Log in, Log on          |
| Sign Out              | Ending the Administrator Session through a `POST` with CSRF token.                                                                                                          | Log out, Log off        |
| Administrator Session | The verified, encrypted session established by Sign In; it expires 30 minutes after the last request (UC-006 BR-006) and unlocks the Maintenance Functions.                         |                         |
| Maintenance Function  | Any create, change or delete operation on Events or Voluntary Service Offers; reachable only within an Administrator Session.                                               | Admin function, Backend |
| Maintenance Link      | A link or button to a Maintenance Function on a public page; shown only during an Administrator Session and visually set apart from the Visitor's own action (UC-006 BR-003, UC-001 BR-006). |                         |
| Confirmation Page     | The separate page on which an Administrator confirms the deletion of an Event or offer before it is removed (FR-006, FR-013).                                               |                         |
| Initial Administrator | The first User Account, created at start-up from a deployment secret when the user table is empty (UC-006 BR-008).                                                                 | Default admin           |
| Demo Seed             | The demo Events and offers inserted on first start with an empty database outside the `prod` profile; never runs in production (NFR-022).                                  | Test data               |

## Pages

| Term                       | Definition                                                                                                                                                       | Avoid                  |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| Start Page                 | The page at `/` that says in one screen what the platform offers and leads on to Events, Voluntary Service Offers and the working group (UC-008).                | Landing page, Home page |
| Event List                 | The page under `/anlaesse` that lists all Upcoming Events in order of Display Date, nearest first.                                                                 |                        |
| Event Page                 | The own page of one Event under its Public Address, showing the complete Description; the page the Visitor shares (FR-003, FR-040).                                |                        |
| Share Dialog               | The operating system's own dialog for passing a link on to an app or contact, opened by the browser on request (Web Share API); offered on most phones and on some desktop browsers. | Share sheet            |
| Offer List                 | The page under `/volontariat` that lists all Voluntary Service Offers, each entry leading to its Offer Page.                                                       |                        |
| Offer Page                 | The own page of one Voluntary Service Offer under its Public Address, showing the complete Description and the Organisation Link; the page the Visitor shares (FR-043, FR-045). |                        |
| Search Term                | The words a Visitor enters to search the Upcoming Events and Voluntary Service Offers; matched against the text a Visitor reads, not against stored markup, and cut to 100 characters (NFR-047). | Keyword                |
| Search Results Page        | The page under `/suche` that lists the Upcoming Events and Voluntary Service Offers matching a Search Term; its address carries the Search Term, so it can be bookmarked and shared (FR-041). |                        |
| Live Results               | The matches shown below the search field while the Visitor is still typing, where the browser runs scripts; a convenience on top of the Search Results Page, never a replacement (FR-042). | Autocomplete, Suggestions |
| Information Page           | A public page about the platform itself: the Working Group Presentation and the Data Protection Page.                                                             |                        |
| Working Group Presentation | The Information Page that introduces the International Working Group and groups its external sources by channel (UC-007 BR-004).                                         |                        |
| Data Protection Page       | The Information Page explaining how personal data is handled; reachable from every page (UC-007 BR-002).                                                                 | Privacy policy         |
| Version Page               | The page naming the application version and the database schema version; requires an Administrator Session (UC-007 BR-003, UC-007 BR-005).                                      | Status page            |
| Schema Version             | The version of the last Flyway migration applied to the database, shown on the Version Page.                                                                      |                        |
| Not-Found Page             | The explanatory page with a way back that answers an unknown address (FR-027).                                                                                     | 404 page               |
| Error Page                 | The plain page shown when the system cannot produce the requested page, without technical details (FR-028).                                                       |                        |
| News Feed                  | The RSS 2.0 document under its own Public Address that lists the Voluntary Service Offers and Upcoming Events as Feed Items for a feed reader to subscribe to (FR-048). Labelled *RSS-Feed* in the interface. | |
| Feed Item                  | One entry of the News Feed: one Event or Voluntary Service Offer in the state of its Last Change, with an identity that changes whenever the Last Change does, so a feed reader shows a change as a new item. | |

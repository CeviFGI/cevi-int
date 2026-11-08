# Release Notes

## 1.0.2 — 2026-11-08

First release.

### New

- **Browse Upcoming Events** — A visitor sees every Upcoming Event on the Event List, nearest first, each with an Excerpt and how much time is left, and reads the complete Description on the Event Page under its Public Address. From there the visitor finds the way to the International Working Group for questions about the event. (UC-001)
- **Manage Events** — An administrator adds, corrects and deletes Events. Deletion needs approval on a separate Confirmation Page. If the administrator supplies no Short Name, the system derives one from the title. (UC-002)
- **Browse Voluntary Service Offers** — A visitor gets an overview of the Voluntary Service Offers the International Working Group recommends, ordered by Organisation. The visitor reads the complete offer on its Offer Page and follows the Organisation Link to the Organisation to approach. (UC-003)
- **Manage Voluntary Service Offers** — An administrator adds, corrects and deletes Voluntary Service Offers. Deletion needs approval on a separate Confirmation Page. If the administrator supplies no Short Name, the system derives one from the Organisation and the location. (UC-004)
- **Contact the International Working Group** — A visitor writes a Contact Message through the Contact Form and reaches a real person without a User Account. Every field explains why it is asked, and a rejected submission names the fault at the field that caused it. (UC-005)
- **Sign In as Administrator** — An administrator signs in with user name and password to reach the Maintenance Functions and signs out again when finished. (UC-006)
- **View Site Information** — A visitor learns who runs the platform on the Working Group Presentation, with its external sources grouped by channel, and reads on the Data Protection Page how personal data is handled. An operator checks the application version and Schema Version on the Version Page. (UC-007)
- **Enter the Platform** — A visitor arriving on the Start Page, usually from a link shared in a chat, sees in one screen what the platform offers and reaches the Events, the Voluntary Service Offers or the International Working Group from there. (UC-008)
- **Share an Event** — A visitor passes an Event on from its Event Page through the Share Dialog, to a chat, a message or an e-mail, without copying its address by hand. (UC-009)
- **Search Events and Offers** — A visitor enters a Search Term and finds the Upcoming Events and Voluntary Service Offers that mention it. The Search Results Page can be bookmarked and shared, and Live Results appear while the visitor is still typing. (UC-010)
- **Share a Voluntary Service Offer** — A visitor passes a Voluntary Service Offer on from its Offer Page through the Share Dialog without copying its address by hand. (UC-011)

### Requirements delivered

| ID      | Requirement                                |
|---------|--------------------------------------------|
| FR-001  | List Upcoming Events                       |
| FR-002  | Order Events Chronologically               |
| FR-003  | Open an Event on Its Own Page              |
| FR-004  | Create Event                               |
| FR-005  | Edit Event                                 |
| FR-006  | Delete Event with Confirmation             |
| FR-007  | Derive Unique Short Name                   |
| FR-008  | Preserve Rejected Form Entries             |
| FR-009  | Browse Voluntary Service Offers            |
| FR-010  | Link Offer to Organisation                 |
| FR-011  | Create Voluntary Service Offer             |
| FR-012  | Edit Voluntary Service Offer               |
| FR-013  | Delete Offer with Confirmation             |
| FR-014  | Send Contact Message                       |
| FR-015  | Answer Spam Protection                     |
| FR-016  | Notify Working Group by E-Mail             |
| FR-017  | Record Contact Message                     |
| FR-018  | Confirm Message Submission                 |
| FR-019  | Sign In as Administrator                   |
| FR-020  | Sign Out                                   |
| FR-021  | Redirect Unauthenticated Access            |
| FR-022  | Hide Maintenance Links                     |
| FR-023  | Reach the Event List in One Step           |
| FR-024  | View Working Group Presentation            |
| FR-025  | View Data Protection Page                  |
| FR-026  | View Version Page                          |
| FR-027  | Show Not-Found Page                        |
| FR-028  | Show General Error Page                    |
| FR-031  | Bootstrap Initial Administrator            |
| FR-032  | Reject Automated Submissions               |
| FR-033  | Format Description in the Browser          |
| FR-034  | Understand the Site on Arrival             |
| FR-035  | Scan a List Before Reading                 |
| FR-036  | See How Urgent an Event Is                 |
| FR-037  | Fill the Contact Form with Guidance        |
| FR-038  | Tell Visitor and Maintenance Actions Apart |
| FR-039  | Find a Link on the Information Page        |
| FR-040  | Share an Event                             |
| FR-041  | Search Events and Offers                   |
| FR-042  | See Results While Typing                   |
| FR-043  | Open an Offer on Its Own Page              |
| FR-044  | Derive Unique Offer Short Name             |
| FR-045  | Share an Offer                             |
| NFR-006 | Write Access Authorisation                 |
| NFR-009 | Spam Submission Rejection                  |
| NFR-011 | Cross-Site Request Forgery                 |
| NFR-021 | Safe HTTP Methods                          |
| NFR-041 | Stylesheet, Font and Script Budget         |
| NFR-046 | Search Response Time                       |
| NFR-047 | Search Term Limits                         |
| C-021   | Script Only as Progressive Enhancement     |

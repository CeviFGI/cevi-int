# Use Case: View Site Information

## Overview

**Use Case ID:** UC-007
**Use Case Name:** View Site Information
**Primary Actor:** Besucher
**Goal:** A visitor learns who runs the platform and how their data is handled, and an operator can check which version of the application is running.
**Status:** Approved
**Revised:** 2026-08-25 for requirements revision 5 (FR-023, FR-034, FR-039). Entering the site is no longer part of this use case: the start page gained content of its own and is specified separately as UC-008. The collected external sources are grouped by channel so that one link can be found without reading all of them.

## Preconditions

- The visitor has opened the public website.

## Main Success Scenario

1. The visitor chooses one of the information pages from the navigation — the presentation of the international working group or the data protection information.
2. The system presents the requested information page.
3. On the presentation of the working group, the system groups the collected external sources by the channel they belong to, so that the visitor can go to one group instead of reading the whole collection.
4. The visitor finds the information or the external source they were looking for.

## Alternative Flows

### A1: Check the running version

**Trigger:** An operator with an established administrator session opens the version page (step 1)
**Flow:**

1. The system presents the application version together with the version of the current database schema.
2. Use case ends.

### A4: Version page opened without an administrator session

**Trigger:** Anyone without an established administrator session opens the version page (step 1)
**Flow:**

1. The system leads them to the sign-in page instead of naming the versions.
2. Use case ends.

### A2: Unknown page requested

**Trigger:** The visitor opens an address the system does not know (step 1)
**Flow:**

1. The system presents a page explaining that the requested content does not exist and offers the way back to the site.
2. Use case ends.

### A3: Unexpected failure

**Trigger:** The system cannot produce the requested page (step 2)
**Flow:**

1. The system presents a general error page.
2. Use case ends.

## Postconditions

### Success Postconditions

- The visitor has seen the requested information.
- No data is changed.

### Failure Postconditions

- No data is changed; the visitor sees an explanatory page instead of the requested content.

## Business Rules

### BR-025: Information pages are reached from the navigation, not from the entry point

The information pages are reached through the navigation, which is present on every page. This rule replaces the earlier one that made the event list the entry point of the site: since the start page carries content of its own (UC-008), entering the site is no longer the same act as opening a list.

### BR-026: Data protection information is publicly reachable

The data protection information is reachable from every page without signing in.

### BR-027: The version page names application and schema version

The version page states both the version of the application and the version of the database schema in use, so an operator can tell whether a deployment and its migrations match.

### BR-046: Collected sources are grouped by channel

The external sources on the presentation of the working group are grouped by the kind of channel they are — blogs, social networks, websites, newsletters, picture collections. The collection is long enough that an ungrouped list forces a visitor to read all of it to find one entry, and on a phone the groups are presented collapsed so that the page stays navigable.

### BR-039: Version information is not public

The version page is reachable only with an established administrator session. It is written for operators, and naming the exact versions to anyone who asks makes it easier to look up which published weaknesses apply to the running system.

# Use Case: View Site Information

## Overview

**Use Case ID:** UC-007
**Use Case Name:** View Site Information
**Primary Actor:** Besucher
**Goal:** A visitor learns who runs the platform and how their data is handled, and an operator can check which version of the application is running.
**Status:** Implemented

## Preconditions

- The visitor has opened the public website.

## Main Success Scenario

1. The visitor opens the start page.
2. The system leads the visitor to the list of upcoming events as the entry point of the site.
3. The visitor chooses one of the information pages from the navigation — the presentation of the international working group or the data protection information.
4. The system presents the requested information page.

## Alternative Flows

### A1: Check the running version

**Trigger:** An operator with an established administrator session opens the version page (step 3)
**Flow:**

1. The system presents the application version together with the version of the current database schema.
2. Use case ends.

### A4: Version page opened without an administrator session

**Trigger:** Anyone without an established administrator session opens the version page (step 3)
**Flow:**

1. The system leads them to the sign-in page instead of naming the versions.
2. Use case ends.

### A2: Unknown page requested

**Trigger:** The visitor opens an address the system does not know (step 3)
**Flow:**

1. The system presents a page explaining that the requested content does not exist and offers the way back to the site.
2. Use case ends.

### A3: Unexpected failure

**Trigger:** The system cannot produce the requested page (step 4)
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

### BR-025: The event list is the entry point

Opening the start page always leads to the list of upcoming events, because the events are the primary purpose of the site.

### BR-026: Data protection information is publicly reachable

The data protection information is reachable from every page without signing in.

### BR-027: The version page names application and schema version

The version page states both the version of the application and the version of the database schema in use, so an operator can tell whether a deployment and its migrations match.

### BR-039: Version information is not public

The version page is reachable only with an established administrator session. It is written for operators, and naming the exact versions to anyone who asks makes it easier to look up which published weaknesses apply to the running system.

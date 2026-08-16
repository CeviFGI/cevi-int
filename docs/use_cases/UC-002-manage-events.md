# Use Case: Manage Events

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Manage Events
**Primary Actor:** Administrator
**Goal:** An administrator keeps the published list of international events current by adding new events, correcting existing ones and removing events that should no longer be shown.
**Status:** Implemented

## Preconditions

- The administrator is signed in (see UC-006).
- The administrator has the administrator role.

## Main Success Scenario

1. The administrator opens the event list and chooses to add a new event.
2. The system presents an empty event form with today's date pre-filled as the display date.
3. The administrator enters the title, the date text as it should be read by visitors, the location, the display date and a formatted description, and optionally a short name for the event address.
4. The administrator submits the form.
5. The system confirms that the submission comes from a form it handed out to this administrator's own session.
6. The system derives a short name from the title if none was entered.
7. The system checks that all mandatory information is present and that the short name is not already used by another event.
8. The system reduces the formatted description to the formatting it permits, discarding anything else.
9. The system records the event and returns the administrator to the event list, where the new event is visible.

## Alternative Flows

### A1: Change an existing event

**Trigger:** The administrator chooses to edit an event instead of adding one (step 1)
**Flow:**

1. The system presents the form filled with the current values of that event.
2. The administrator changes the values and submits the form.
3. The system validates the entry as in steps 5 to 8 and records the changed event.
4. Use case ends.

### A2: Incomplete or invalid entry

**Trigger:** A mandatory value is missing, a value is too long, or the short name is already used by another event (step 7)
**Flow:**

1. The system rejects the entry, keeps the values the administrator typed and marks the fields that need correction.
2. Use case continues at step 3.

### A3: Remove an event

**Trigger:** The administrator chooses to delete an event (step 1)
**Flow:**

1. The system presents the event and asks the administrator to confirm the removal on a confirmation form.
2. The administrator confirms by submitting that form.
3. The system confirms that the confirmation comes from a form it handed out to this administrator's own session.
4. The system removes the event and returns to the event list.
5. Use case ends.

### A4: Removal not confirmed

**Trigger:** The administrator leaves the confirmation page without confirming (A3 step 2)
**Flow:**

1. The event remains unchanged.
2. Use case ends.

### A5: Event no longer exists

**Trigger:** The administrator opens the edit or delete page of an event that has meanwhile been removed (step 1)
**Flow:**

1. The system informs the administrator that the requested event does not exist.
2. Use case ends.

### A6: Event cannot be stored

**Trigger:** The event cannot be recorded (step 9)
**Flow:**

1. The system discards the incomplete change so that the previous state is preserved.
2. The system presents the form again with the entered values.
3. Use case continues at step 3.

### A7: Submission does not come from the system's own form

**Trigger:** A change arrives that the system cannot trace back to a form it handed out to this session (step 5)
**Flow:**

1. The system refuses the change without inspecting the submitted values.
2. Use case ends.

## Postconditions

### Success Postconditions

- The event is added, changed or removed, and the public event list reflects the change immediately.

### Failure Postconditions

- No event is added, changed or removed; the stored data is exactly as it was before the attempt.

## Business Rules

### BR-004: Event maintenance is restricted to administrators

Only a signed-in administrator may add, change or remove events. Visitors can only read them.

### BR-005: Mandatory event information

Title, short name, date text, location, display date and description must all be present for an event to be stored.

### BR-006: Length limits for event information

Title, short name, date text and location are limited to 255 characters each; the description is limited to 65535 characters.

### BR-007: Short names are unique

No two events may share the same short name, because the short name forms the public address of the event.

### BR-008: Short names are derived from the title

If the administrator does not supply a short name, the system derives one from the title. If that name is already taken, a counter is appended until the name is free.

### BR-009: Derived short names are shortened

A short name derived from the title is truncated to at most 252 characters before uniqueness is established.

### BR-010: Removal must be confirmed

An event is only removed after the administrator has explicitly confirmed the removal on a separate page.

### BR-011: An invalid display date is ignored

If the supplied display date cannot be read as a date, the previously stored display date is kept; for a new event this means the entry is rejected as incomplete.

### BR-028: Changes are accepted only from the system's own forms

Every addition, change and removal must be traceable to a form the system handed out to the same signed-in session, and must be submitted as a form rather than by following a link. A request that merely carries a valid session, without that origin, is refused. This prevents a foreign website from performing changes in the name of an administrator who happens to be signed in.

### BR-029: Descriptions keep only permitted formatting

Before a description is stored, it is reduced to the formatting the platform permits — text emphasis, headings, lists, tables, links, images and colours. Everything else, in particular anything that could run as a program in a visitor's browser, is discarded. The reduction happens when the system receives the description; what the editor in the browser allows is a convenience for the administrator, not the boundary that is relied upon.

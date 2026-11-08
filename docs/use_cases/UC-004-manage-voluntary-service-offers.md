# Use Case: Manage Voluntary Service Offers

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Manage Voluntary Service Offers
**Primary Actor:** Administrator
**Goal:** An administrator keeps the published voluntary service offers accurate by adding new offers, correcting existing ones and removing offers that are no longer available.
**Status:** Done

**Requirements:** [FR-008, FR-011, FR-012, FR-013, FR-033, FR-044, NFR-006, NFR-011, NFR-021](../requirements.md)

## Preconditions

- The administrator is signed in (see UC-006).
- The administrator has the administrator role.

## Main Success Scenario

1. The administrator opens the Offer List and chooses to add a new offer.
2. The system presents an empty offer form.
3. The administrator enters the Organisation, the Organisation Link, the location and a formatted Description, and optionally a Short Name for the offer's Public Address.
4. The administrator submits the form.
5. The system confirms that the submission comes from a form it handed out to the browser the administrator works in.
6. The system derives a Short Name from the Organisation and the location if none was entered.
7. The system checks that all mandatory information is present, that no value exceeds its permitted length, that the Organisation Link is a web address and that the Short Name is not already used by another offer; a Short Name the administrator entered is taken as entered (BR-009).
8. The system reduces the formatted Description to the formatting it permits, discarding anything else.
9. The system records the offer and presents the updated Offer List.

## Alternative Flows

### A1: Change an existing offer

**Trigger:** The administrator chooses to edit an offer instead of adding one (step 1)
**Flow:**

1. The system presents the form filled with the current values of that offer, including its Short Name.
2. The administrator changes the values and submits the form.
3. The system validates the entry as in steps 5 to 8 and records the changed offer.
4. Use case ends.

### A2: Incomplete or invalid entry

**Trigger:** A mandatory value is missing, a value is too long, the Organisation Link is not a web address, or the Short Name the administrator entered is already used by another offer (step 7)
**Flow:**

1. The system rejects the entry, keeps the values the administrator typed and marks the fields that need correction.
2. Use case continues at step 3.

### A3: Remove an offer

**Trigger:** The administrator chooses to delete an offer (step 1)
**Flow:**

1. The system presents the offer and asks the administrator to confirm the removal on a Confirmation Page.
2. The administrator confirms by submitting that form.
3. The system confirms that the confirmation comes from a form it handed out to the browser the administrator works in.
4. The system removes the offer and presents the updated Offer List.
5. Use case ends.

### A4: Removal not confirmed

**Trigger:** The administrator chose to delete (A3) but leaves the Confirmation Page without confirming (step 1)
**Flow:**

1. The offer remains unchanged.
2. Use case ends.

### A5: Offer no longer exists

**Trigger:** The administrator opens the edit or delete page of an offer, or submits a change or removal of an offer, that has meanwhile been removed (step 1)
**Flow:**

1. The system informs the administrator that the requested offer does not exist.
2. Use case ends.

### A6: Offer cannot be stored

**Trigger:** The offer cannot be recorded (step 9)
**Flow:**

1. The system discards the incomplete change so that the previous state is preserved.
2. The system presents the form again with the entered values.
3. Use case continues at step 3.

### A7: Submission does not come from the system's own form

**Trigger:** A change arrives that the system cannot trace back to a form it handed out to this browser (step 5)
**Flow:**

1. The system refuses the change without inspecting the submitted values.
2. Use case ends.

### A8: Offer cannot be removed

**Trigger:** The removal the administrator confirmed in A3 cannot be completed (step 1)
**Flow:**

1. The system discards the incomplete removal so that the offer is preserved.
2. The system presents the Confirmation Page again and informs the administrator that the offer could not be removed.
3. Use case ends.

## Postconditions

### Success Postconditions

- The offer is added, changed or removed, and the Offer List and the offer's Offer Page reflect the change immediately.
- Every stored offer has a Short Name that no other offer uses.

### Failure Postconditions

- No offer is added, changed or removed; the stored data is exactly as it was before the attempt.

## Business Rules

### BR-001: Offer maintenance is restricted to administrators

Only a signed-in administrator may add, change or remove voluntary service offers.

### BR-002: Mandatory offer information

Organisation, Organisation Link, location, Short Name and Description must all be present for an offer to be stored; the Short Name may be left to the system (BR-007).

### BR-003: Removal must be confirmed

An offer is only removed after the administrator has explicitly confirmed the removal on a separate page.

### BR-004: Length limits for offer information

Organisation, Organisation Link, location and Short Name are limited to 255 characters each; the Description is limited to 65535 characters. The same limits apply as for events (UC-002 BR-003), so both kinds of published content behave alike.

### BR-005: The Organisation Link must be a web address

The Organisation Link is accepted only if it points at a website reachable over the web. Any other kind of address is rejected, so a stored link can never turn into something that runs in a visitor's browser when the offer is displayed as a hyperlink.

### BR-006: Short Names are unique among the offers

No two offers may share the same Short Name, because the Short Name forms the Public Address of the offer's Offer Page. An offer may carry the same Short Name as an event: Offer Pages and Event Pages have addresses of their own, so the two never collide.

### BR-007: Short Names are derived from Organisation and location

If the administrator does not supply a Short Name, the system derives one from the Organisation followed by the location, in the same readable form as for an event (UC-002 BR-005), and truncates it to at most 252 characters (as UC-002 BR-006 does). If that name is already taken by another offer, a counter is appended until the name is free.

### BR-008: A Short Name stays until the administrator changes it

Changing the Organisation or the location of an existing offer does not change its Short Name, so a link to the Offer Page that visitors have already passed on keeps working. Only an administrator who edits the Short Name itself changes the Public Address — the previous address then no longer leads to the offer. An administrator who empties the Short Name while editing changes it, too: the system derives a new one as in BR-007, and the previous address no longer leads to the offer.

### BR-009: An entered Short Name is taken as entered

A Short Name the administrator enters is stored as entered and only checked for uniqueness (BR-006) and length (BR-004), as for an event (UC-002 step 7). Only a derived Short Name is brought into the readable form of BR-007.

Offers are also covered by UC-002 BR-009 (changes accepted only from the system's own forms) and UC-002 BR-010 (descriptions keep only permitted formatting).

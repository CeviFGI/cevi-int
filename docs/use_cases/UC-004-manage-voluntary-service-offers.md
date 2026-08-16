# Use Case: Manage Voluntary Service Offers

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Manage Voluntary Service Offers
**Primary Actor:** Administrator
**Goal:** An administrator keeps the published voluntary service offers accurate by adding new offers, correcting existing ones and removing offers that are no longer available.
**Status:** Implemented

## Preconditions

- The administrator is signed in (see UC-006).
- The administrator has the administrator role.

## Main Success Scenario

1. The administrator opens the list of voluntary service offers and chooses to add a new offer.
2. The system presents an empty offer form.
3. The administrator enters the organisation, the link to the organisation, the location and a formatted description.
4. The administrator submits the form.
5. The system checks that all mandatory information is present.
6. The system records the offer and presents the updated list of offers.

## Alternative Flows

### A1: Change an existing offer

**Trigger:** The administrator chooses to edit an offer instead of adding one (step 1)
**Flow:**

1. The system presents the form filled with the current values of that offer.
2. The administrator changes the values and submits the form.
3. The system validates the entry as in step 5 and records the changed offer.
4. Use case ends.

### A2: Incomplete entry

**Trigger:** A mandatory value is missing (step 5)
**Flow:**

1. The system rejects the entry, keeps the values the administrator typed and marks the fields that need correction.
2. Use case continues at step 3.

### A3: Remove an offer

**Trigger:** The administrator chooses to delete an offer (step 1)
**Flow:**

1. The system presents the offer and asks the administrator to confirm the removal.
2. The administrator confirms.
3. The system removes the offer and presents the updated list.
4. Use case ends.

### A4: Removal not confirmed

**Trigger:** The administrator leaves the confirmation page without confirming (A3 step 2)
**Flow:**

1. The offer remains unchanged.
2. Use case ends.

### A5: Offer no longer exists

**Trigger:** The administrator opens the edit or delete page of an offer that has meanwhile been removed (step 1)
**Flow:**

1. The system informs the administrator that the requested offer does not exist.
2. Use case ends.

### A6: Offer cannot be stored

**Trigger:** The offer cannot be recorded (step 6)
**Flow:**

1. The system discards the incomplete change so that the previous state is preserved.
2. The system presents the form again with the entered values.
3. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The offer is added, changed or removed, and the public list reflects the change immediately.

### Failure Postconditions

- No offer is added, changed or removed; the stored data is exactly as it was before the attempt.

## Business Rules

### BR-014: Offer maintenance is restricted to administrators

Only a signed-in administrator may add, change or remove voluntary service offers.

### BR-015: Mandatory offer information

Organisation, organisation link, location and description must all be present for an offer to be stored.

### BR-016: Removal must be confirmed

An offer is only removed after the administrator has explicitly confirmed the removal on a separate page.

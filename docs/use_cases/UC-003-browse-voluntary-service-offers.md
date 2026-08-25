# Use Case: Browse Voluntary Service Offers

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Browse Voluntary Service Offers
**Primary Actor:** Besucher
**Goal:** A visitor gets an overview of the voluntary service opportunities the working group recommends, so they can decide which organisation to approach.
**Status:** Approved
**Revised:** 2026-08-25 for requirements revision 5 (FR-010, FR-035, FR-038). The link to the offering organisation, previously described in this specification but presented on no page, becomes the offer's own control; descriptions are shortened the same way as in the event list.

## Preconditions

- The visitor has opened the public website.

## Main Success Scenario

1. The visitor opens the voluntary service section of the site.
2. The system presents all recorded offers, each with the offering organisation, the location, a shortened excerpt of the description and a control leading to the organisation's own website.
3. The visitor compares the presented offers and picks the one that interests them.
4. The visitor follows the control of that offer to the website of the organisation.
5. The system opens the organisation's website separately, so the visitor keeps the overview they came from.
6. The visitor knows which organisation to approach and how to reach it.

## Alternative Flows

### A1: No offers recorded

**Trigger:** No voluntary service offer exists (step 2)
**Flow:**

1. The system explains that no offer is currently recorded and offers the visitor the way to the working group instead.
2. Use case ends.

### A2: The visitor wants the full description

**Trigger:** The visitor wants to read a description that was shortened (step 3)
**Flow:**

1. The system presents the complete description of that offer in place, without leaving the overview.
2. Use case continues at step 4.

### A3: Description too short to shorten

**Trigger:** An offer's description is already shorter than the excerpt length (step 2)
**Flow:**

1. The system presents the description in full, without any indication that something was left out.
2. Use case continues at step 3.

### A4: The visitor is a signed-in administrator

**Trigger:** An administrator session exists when the list is presented (step 2)
**Flow:**

1. The system additionally offers the maintenance controls for each offer, presented as a separate and quieter group than the control meant for visitors.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The visitor has seen all published voluntary service offers and has reached the website of the organisation they chose.
- No data is changed.

### Failure Postconditions

- No data is changed; the visitor sees an explanation instead of an empty overview.

## Business Rules

### BR-012: All offers are shown

Voluntary service offers are not filtered by date or any other criterion — every recorded offer is visible to every visitor.

### BR-013: Every offer points to its organisation

Each offer names the organisation that runs it and links to that organisation's own website, because the application only advertises the offer and does not administer applications.

### BR-043: The organisation is the offer's own control

The link to the organisation is presented as the offer's primary control, and it opens the organisation's site separately from the overview. An offer has no page of its own on this platform, so without that control the offer is a dead end — the visitor would learn that an opportunity exists but not where to pursue it.

### BR-044: Offers are shortened like events

An offer's description is shortened in the overview in the same way as an event's, so that offers can be compared rather than read one after another. Because an offer has no page of its own, the complete description is made available in place instead.

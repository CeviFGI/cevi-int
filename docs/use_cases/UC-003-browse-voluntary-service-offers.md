# Use Case: Browse Voluntary Service Offers

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Browse Voluntary Service Offers
**Primary Actor:** Besucher
**Goal:** A visitor gets an overview of the voluntary service opportunities the working group recommends, reads the complete offer that interests them, and learns which Organisation to approach.
**Status:** Done

**Requirements:** [FR-009, FR-010, FR-022, FR-027, FR-035, FR-038, FR-043](../requirements.md)

## Preconditions

- The visitor has opened the public website.

## Main Success Scenario

1. The visitor opens the Offer List.
2. The system presents all recorded offers in alphabetical order of their Organisation, offers of the same Organisation in the order they were recorded, each entry with its Organisation, its location and an Excerpt of its Description; the whole entry leads to the offer's Offer Page.
3. The visitor compares the presented offers and selects the one that interests them.
4. The system presents the selected offer on its Offer Page under its Public Address: the Organisation, the location, the complete Description as the administrator formatted it, and a control leading to the Organisation's website.
5. The visitor follows that control to the website of the Organisation.
6. The system opens the Organisation's website separately, so the visitor keeps the Offer Page they came from.
7. The visitor knows which Organisation to approach and how to reach it.

## Alternative Flows

### A1: No offers recorded

**Trigger:** No voluntary service offer exists (step 2)
**Flow:**

1. The system explains that no offer is currently recorded and offers the visitor the way to the working group instead.
2. Use case ends.

### A2: Description too short to shorten

**Trigger:** An offer's Description is already shorter than the Excerpt (step 2)
**Flow:**

1. The system presents the Description in full in the entry, without any indication that something was left out; the entry still leads to the Offer Page.
2. Use case continues at step 3.

### A3: The visitor is a signed-in administrator

**Trigger:** An Administrator Session exists when the Offer List is presented (step 2)
**Flow:**

1. The system additionally offers the Maintenance Links for each offer in the Offer List, presented as a separate and quieter group than the action meant for visitors.
2. Use case continues at step 3.

### A4: The visitor opens an Offer Page directly

**Trigger:** The visitor follows a shared or bookmarked Public Address of an Offer Page, or picks an offer among the search results (UC-010 step 5) or in the extract on the start page (UC-008 step 4), instead of opening the Offer List (step 1)
**Flow:**

1. The system presents the offer on its Offer Page as in step 4, with the way to the complete Offer List.
2. Use case continues at step 5.

### A5: Offer Page no longer available

**Trigger:** The visitor opens an Offer Page whose Short Name is unknown to the system (step 4)
**Flow:**

1. The system informs the visitor that the requested page does not exist and offers the way to the Offer List.
2. Use case ends.

### A6: The visitor on an Offer Page is a signed-in administrator

**Trigger:** An Administrator Session exists when an Offer Page is presented (step 4)
**Flow:**

1. The system additionally offers the Maintenance Links for that offer on its Offer Page, presented as a separate and quieter group than the control leading to the Organisation.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The visitor has read the complete offer on its Offer Page and has reached the website of the Organisation running it.
- No data is changed.

### Failure Postconditions

_None — the use case changes no data._

## Business Rules

### BR-001: All offers are shown

Voluntary service offers are not filtered by date or any other criterion — every recorded offer is visible to every visitor.

### BR-002: Every offer points to its Organisation

Each offer names the Organisation that runs it, and its Offer Page links to that Organisation's own website, because the application only advertises the offer and does not administer applications.

### BR-003: The entry leads to the Offer Page, the Offer Page to the Organisation

Each entry of the Offer List has exactly one action for the visitor: selecting anywhere on the entry opens the offer's Offer Page. The link to the Organisation is presented only there, as the Offer Page's primary control, and opens the Organisation's site separately from the Offer Page. A visitor therefore reads the complete offer before leaving the platform.

### BR-004: The list shows an Excerpt, the Offer Page the whole text

An offer's Description is shortened in the Offer List in the same way as an event's on the Event List (UC-001 BR-004), so that offers can be compared rather than read one after another. The complete Description is presented only on the Offer Page.

### BR-005: Every offer has a stable Public Address

Each Offer Page is reachable under the offer's Short Name, which identifies the offer uniquely among the offers (UC-004 BR-006), so a link to an offer stays valid as long as the offer exists and its Short Name is unchanged. Sharing that address is UC-011.

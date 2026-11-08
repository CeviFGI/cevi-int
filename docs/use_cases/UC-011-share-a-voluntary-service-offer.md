# Use Case: Share a Voluntary Service Offer

## Overview

**Use Case ID:** UC-011
**Use Case Name:** Share a Voluntary Service Offer
**Primary Actor:** Besucher
**Goal:** A visitor passes a Voluntary Service Offer on to friends — in a chat, a message or an e-mail — without copying its address by hand, so that the people they want to tell about it land directly on the offer.
**Trigger:** The visitor asks to share the offer they are reading on its Offer Page.
**Status:** Tested
**Introduced:** 2026-11-07 with requirements revision 8 (FR-045). It is the sharing of UC-009 on the second page that has a Public Address of its own; it behaves the same way and differs only in what the Share Dialog shows, because an offer has neither title nor Date Text.

**Requirements:** [FR-045, NFR-041, C-021](../requirements.md)
**Layers:** UI

## Preconditions

- The visitor is reading an offer on its Offer Page (UC-003, step 4).

## Main Success Scenario

1. The visitor asks to share the offer.
2. The system opens the Share Dialog of the visitor's device, filled with the offer's Organisation, its location and its Public Address.
3. The visitor chooses the app or contact to pass the offer to and sends it.
4. The system returns the visitor to the Offer Page as it was before.

## Alternative Flows

### A1: The browser offers no Share Dialog

**Trigger:** The visitor's browser offers no Share Dialog, or opening it fails for a reason other than the visitor cancelling (step 2)
**Flow:**

1. The system places the offer's Public Address on the visitor's clipboard.
2. The system confirms beside the share control that the address was copied; the confirmation is announced to assistive technology and disappears again on its own.
3. The visitor pastes the address wherever they want to pass the offer on.
4. Use case ends.

### A2: The clipboard refuses the address

**Trigger:** The visitor's browser offers no Share Dialog, and it refuses to place the Public Address on the clipboard (step 2)
**Flow:**

1. Instead of confirming a copy, the system shows the Public Address as selected text next to the share control, with the hint to copy it by hand.
2. The visitor copies the address themselves.
3. Use case ends.

### A3: The visitor cancels the Share Dialog

**Trigger:** The visitor closes the Share Dialog without choosing an app or contact (step 3)
**Flow:**

1. The system leaves the Offer Page unchanged and shows no message.
2. Use case ends.

### A4: The browser runs no script

**Trigger:** The visitor's browser has scripting switched off or did not load the share script (step 1)
**Flow:**

1. Instead of the share control, the Offer Page shows the offer's Public Address as selectable text, labelled as the link to the offer; the system builds it from the address under which the visitor's request reached the platform (BR-003).
2. The visitor copies the address themselves.
3. Use case ends.

## Postconditions

### Success Postconditions

- The offer's Public Address has been handed to the app the visitor chose, or lies on the visitor's clipboard.
- The visitor is still on the Offer Page; no data is stored or changed.

### Failure Postconditions

_None — the use case changes no data._

## Business Rules

### BR-001: What the browser offers decides, not the device

The system opens the Share Dialog whenever the visitor's browser offers one, and copies to the clipboard otherwise. The device type plays no part, as for an event (UC-009 BR-001).

### BR-002: The Share Dialog carries Organisation, location and address; the clipboard only the address

The Share Dialog receives the offer's Organisation, its location and its Public Address, so the recipient sees what the link is about before opening it — an offer has no title, and the Organisation together with the location is what names it in the Offer List. The clipboard receives the Public Address alone, so that pasting it yields a clean link that every app recognises.

### BR-003: The shared address is the offer's Public Address

The address passed on is always the offer's Public Address: the scheme, host and port under which the visitor opened the Offer Page, followed by the address of the Offer Page as the Offer List links to it — the Offer Page with the offer's Short Name as its one and only parameter (`/volontariat/detail?slug=<Short Name>`). Nothing else is added: no further parameter, no fragment, no session and nothing about the visitor who shared it, even when the address the browser shows carries any of these. Where the share control runs, the host is the one the visitor's browser actually loaded the page from. Where it does not (A4), the system names the scheme, host and port under which the visitor's request reached the platform, as the platform's own hosting in front of it reports them, and takes no host from anything the visitor's request merely claims — the same rule as UC-009 BR-003. A shared link therefore resolves for the recipient wherever it resolved for the visitor, as long as the offer exists and its Short Name is unchanged (UC-003 BR-005, UC-004 BR-008).

### BR-004: Sharing reveals nothing and reaches no third party

The system records nothing about a share: no counter, no log entry of who shared which offer. The share control issues no request to another host and loads nothing from one; the visitor's own device decides where the offer goes.

### BR-005: The Offer Page does not depend on sharing

Sharing is an enhancement of the Offer Page, not part of reading it (C-021): without the share script the Offer Page shows the Public Address as selectable text, and every other part of the page — the complete Description and the Organisation Link — reads and works exactly as before. The Offer Page uses the same share script as the Event Page and no further one.

### BR-006: Copying is confirmed, cancelling is not

Copying and cancelling on the Offer Page are answered exactly as on the Event Page (UC-009 BR-006): a completed copy is confirmed, a cancelled Share Dialog is not.

# Use Case: Share an Event

## Overview

**Use Case ID:** UC-009
**Use Case Name:** Share an Event
**Primary Actor:** Besucher
**Goal:** A visitor passes an event on to friends — in a chat, a message or an e-mail — without copying its address by hand, so that the people they want to bring along land directly on the event.
**Trigger:** The visitor asks to share the event they are reading on its Event Page.
**Status:** Draft
**Introduced:** 2026-11-06 with requirements revision 6 (FR-040). Until then a visitor could pass an event on only by copying the address from the browser's address bar, which most visitors on a phone never see.

**Requirements:** [FR-040, NFR-041, C-021](../requirements.md)
**Layers:** UI

## Preconditions

- The visitor is reading an event on its Event Page (UC-001, step 5).

## Main Success Scenario

1. The visitor asks to share the event.
2. The system opens the Share Dialog of the visitor's device, filled with the event's title, its date text and its Public Address.
3. The visitor chooses the app or contact to pass the event to and sends it.
4. The system returns the visitor to the Event Page as it was before.

## Alternative Flows

### A1: The browser offers no Share Dialog

**Trigger:** The visitor's browser offers no Share Dialog, or opening it fails for a reason other than the visitor cancelling (step 2)
**Flow:**

1. The system places the event's Public Address on the visitor's clipboard.
2. The system confirms beside the share control that the address was copied; the confirmation is announced to assistive technology and disappears again on its own.
3. The visitor pastes the address wherever they want to pass the event on.
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

1. The system leaves the Event Page unchanged and shows no message.
2. Use case ends.

### A4: The browser runs no script

**Trigger:** The visitor's browser has scripting switched off or did not load the share script (step 1)
**Flow:**

1. Instead of the share control, the Event Page shows the event's Public Address as selectable text, labelled as the link to the event.
2. The visitor copies the address themselves.
3. Use case ends.

## Postconditions

### Success Postconditions

- The event's Public Address has been handed to the app the visitor chose, or lies on the visitor's clipboard.
- The visitor is still on the Event Page; no data is stored or changed.

### Failure Postconditions

_None — the use case changes no data._

## Business Rules

### BR-001: What the browser offers decides, not the device

The system opens the Share Dialog whenever the visitor's browser offers one, and copies to the clipboard otherwise. It does not guess from screen width or device type whether the visitor is on a phone: a desktop browser that offers a Share Dialog shows it, a phone browser that offers none copies.

### BR-002: The Share Dialog carries title, date text and address; the clipboard only the address

The Share Dialog receives the event's title, its date text and its Public Address, so the recipient sees what the link is about before opening it. The clipboard receives the Public Address alone, so that pasting it yields a clean link that every app recognises.

### BR-003: The shared address is the event's Public Address

The address passed on is always the event's Public Address: the scheme, host and port under which the visitor opened the Event Page, followed by the event's path built from its Short Name — with no parameters, no fragment, no session and nothing else about the visitor who shared it, even when the address the browser shows carries any of these. The host is the one the visitor's browser actually loaded the page from; the system takes no host from anything the request merely claims. A shared link therefore resolves for the recipient wherever it resolved for the visitor, as long as the event exists (UC-001 BR-003). No platform-wide base address needs to be configured for this.

### BR-004: Sharing reveals nothing and reaches no third party

The system records nothing about a share: no counter, no log entry of who shared which event. The share control issues no request to another host and loads nothing from one; the visitor's own device decides where the event goes.

### BR-005: The page does not depend on sharing

The share control is the only part of a public page that needs a script, and its absence takes nothing away but the convenience: without the script the Event Page shows the Public Address as selectable text, and every other part of the page reads and works exactly as before.

### BR-006: Copying is confirmed, cancelling is not

A completed copy is confirmed visibly and to assistive technology, because the clipboard otherwise gives no sign that anything happened. A cancelled Share Dialog is the visitor's own decision and is answered with nothing — no message, no error.

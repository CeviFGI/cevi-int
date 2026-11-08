# Use Case: Add an Event to the Calendar

## Overview

**Use Case ID:** UC-012
**Use Case Name:** Add an Event to the Calendar
**Primary Actor:** Besucher
**Goal:** A visitor takes an event they want to attend into their own calendar without typing its dates, place and address in by hand, so that the event is not forgotten until it begins.
**Trigger:** The visitor asks to take the event they are reading on its Event Page into their calendar.
**Status:** Approved

**Requirements:** [FR-047, NFR-042, NFR-048, C-021](../requirements.md)
**Layers:** UI

## Preconditions

- The visitor is reading an event on its Event Page (UC-001, step 5).

## Main Success Scenario

1. The visitor asks to take the event into their calendar.
2. The system delivers the event as a Calendar Entry: one all-day entry from the event's First Day to its Last Day, with its title, location, Date Text, the text of its Description and its Public Address.
3. The visitor opens the Calendar Entry with the calendar app of their device, which offers to add the event.
4. The visitor adds the event to their calendar.

## Alternative Flows

### A1: The event has no period

**Trigger:** The event has no First Day and Last Day recorded (step 1)
**Flow:**

1. The system presents the Event Page without the offer to take the event into a calendar; everything else on the page reads as before (BR-001).
2. Use case ends.

### A2: The event or its period no longer exists

**Trigger:** The visitor asks for the Calendar Entry of an event that has meanwhile been removed, whose Short Name has changed, or whose First Day and Last Day have been removed (step 2)
**Flow:**

1. The system delivers no Calendar Entry and presents the Not-Found Page with the way back to the Event List.
2. Use case ends.

### A3: The device opens no calendar app

**Trigger:** The visitor's device has no calendar app for the Calendar Entry, or the visitor chooses to keep it as a file (step 3)
**Flow:**

1. The device keeps the Calendar Entry as a file.
2. The visitor can add it to a calendar later, on this or another device, by opening the file there.
3. Use case ends.

## Postconditions

### Success Postconditions

- The event is in the visitor's calendar as an all-day entry from its First Day to its Last Day, carrying its title, location, Date Text, Description and Public Address.
- No data is stored or changed on the platform.

### Failure Postconditions

_None — the use case changes no data._

## Business Rules

### BR-001: Offered only for an event with a period

The Event Page offers the Calendar Entry only while the event has both a First Day and a Last Day (UC-002 BR-012). The system never derives a period from the Date Text or the Display Date: the Date Text is free text, and the Display Date only decides how long the event stays on the Event List (UC-001 BR-001). Whether the Display Date has passed does not matter; an event reachable on its Event Page offers its Calendar Entry as long as its period is recorded.

### BR-002: What the Calendar Entry holds

The Calendar Entry holds exactly one event: its title as the name of the entry, its location as the place, and as the notes its Date Text, the visible text of its Description without formatting, and its Public Address. The entry is all-day and lasts from the First Day through the Last Day, both included. It names no time of day and no time zone, so it covers the same calendar days wherever the visitor's calendar is set. The entry carries no reminder and no invitation of other people.

### BR-003: The address in the entry is the event's Public Address

The Public Address in the Calendar Entry is built as for sharing without script (UC-009 BR-003): the scheme, host and port under which the visitor's request reached the platform, as the platform's own hosting reports them, followed by the address of the Event Page with the event's Short Name as its one and only parameter. Nothing about the visitor is added.

### BR-004: The same event is always the same entry

Every Calendar Entry of one event carries the same identity, also after the administrator has changed the event's title, period or Short Name, and the identities of two different events never coincide. This is all the system guarantees: whether a calendar app that receives the event a second time updates the entry it already holds or adds a second one is decided by that app. That the calendar apps named in NFR-048 accept the entry is checked once at acceptance, not by the system.

### BR-005: A Calendar Entry is a copy, not a subscription

The Calendar Entry reflects the event at the moment it is delivered. A later change by an administrator does not reach the visitor's calendar on its own; a visitor who takes the event in again receives the current details under the same identity (BR-004). A removed event stays in the visitor's calendar until the visitor removes it.

### BR-006: Taking an event into the calendar reveals nothing and needs no script

Delivering a Calendar Entry is logged like any other page view and nothing more (NFR-031): no counter and no record of its own, and the visitor's address only at the detail level NFR-031 allows for every page view. The Calendar Entry comes from the platform itself; no request is issued to another host and no online calendar service is involved (NFR-042). The offer is a plain link that works without script (C-021), and the Event Page reads and works exactly as before when the offer is absent.

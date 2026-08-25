# Use Case: Browse Upcoming Events

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Browse Upcoming Events
**Primary Actor:** Besucher
**Goal:** A visitor finds out which international Cevi events are still ahead and reads the details of the ones that interest them.
**Status:** Approved
**Revised:** 2026-08-25 for requirements revision 5 (FR-035, FR-036, FR-038). The list now presents a shortened excerpt and how much time is left, so that several events can be compared on a phone screen; the full description moved to the event's own page. What is stored and what is published is unchanged.

## Preconditions

- The visitor has opened the public website.
- At least one event has been published by an administrator (otherwise the list is simply empty).

## Main Success Scenario

1. The visitor opens the events section of the site.
2. The system determines all events whose display date has not yet passed.
3. The system presents the events ordered by display date, each with its title, date text, location, a shortened excerpt of its description and how much time is left before it leaves the list.
4. The visitor compares the presented events and selects the one that interests them.
5. The system presents the selected event on its own page with the complete description as the administrator formatted it.
6. The visitor has the information needed to decide whether to attend, and can pass the address of the event on to others.

## Alternative Flows

### A1: No upcoming events

**Trigger:** No event has a display date of today or later (step 2)
**Flow:**

1. The system explains that nothing is currently announced and offers the visitor the way to the working group instead.
2. Use case ends.

### A2: Event page no longer available

**Trigger:** The visitor opens an event page whose short name is unknown to the system (step 4)
**Flow:**

1. The system informs the visitor that the requested page does not exist and offers the way back to the event list.
2. Use case ends.

### A3: Description too short to shorten

**Trigger:** An event's description is already shorter than the excerpt length (step 3)
**Flow:**

1. The system presents the description in full in the list, without any indication that something was left out.
2. Use case continues at step 4.

### A4: The visitor is a signed-in administrator

**Trigger:** An administrator session exists when the list is presented (step 3)
**Flow:**

1. The system additionally offers the maintenance controls for each event, presented as a separate and quieter group than the control meant for visitors.
2. Use case continues at step 4.

## Postconditions

### Success Postconditions

- The visitor has seen the events that are still ahead and the full details of the one they chose.
- No data is changed.

### Failure Postconditions

- No data is changed; the visitor sees an explanatory page instead of the requested content.

## Business Rules

### BR-001: Only future events are listed

An event appears in the public event list only if its display date is today or later. Past events remain stored but are hidden from the list.

### BR-002: Events are ordered chronologically

The event list is sorted by display date, closest event first.

### BR-003: Every event has a stable public address

Each event is reachable under a short, readable name that identifies it uniquely, so a link to an event stays valid.

### BR-040: The list shows an excerpt, the event page the whole text

In a list, an event's description is shortened to a short plain-text excerpt that breaks on a word boundary. The complete formatted description is presented only on the event's own page. A list answers which event is worth opening; presenting every description in full makes that comparison impossible on a phone.

### BR-041: Every listed event states how much time is left

Each event in the list states how long it will remain announced, derived from the same display date that decides whether it is listed at all. An opportunity that is about to pass must be recognisable without opening it.

### BR-042: Maintenance controls never outrank the visitor's control

Where maintenance controls are shown to a signed-in administrator, they are presented as a separate, visually quieter group. The control meant for the visitor stays the most prominent one on the page, because the public pages are read overwhelmingly by visitors.

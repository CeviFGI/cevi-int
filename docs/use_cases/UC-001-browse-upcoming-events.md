# Use Case: Browse Upcoming Events

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Browse Upcoming Events
**Primary Actor:** Besucher
**Goal:** A visitor finds out which international Cevi events are still ahead and reads the details of the ones that interest them.
**Status:** Implemented

## Preconditions

- The visitor has opened the public website.
- At least one event has been published by an administrator (otherwise the list is simply empty).

## Main Success Scenario

1. The visitor opens the events section of the site.
2. The system determines all events whose display date has not yet passed.
3. The system presents the events ordered by display date, each with its title, date text, location and description.
4. The visitor selects one event to see it on its own page.
5. The system presents the full details of the selected event.

## Alternative Flows

### A1: No upcoming events

**Trigger:** No event has a display date of today or later (step 2)
**Flow:**

1. The system presents an empty event list.
2. Use case ends.

### A2: Event page no longer available

**Trigger:** The visitor opens an event page whose short name is unknown to the system (step 4)
**Flow:**

1. The system informs the visitor that the requested page does not exist.
2. Use case ends.

## Postconditions

### Success Postconditions

- The visitor has seen the events that are still ahead.
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

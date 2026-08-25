# Use Case: Enter the Platform

## Overview

**Use Case ID:** UC-008
**Use Case Name:** Enter the Platform
**Primary Actor:** Besucher
**Goal:** A visitor who arrives on the platform, usually by following a link shared in a chat, understands within one screen what the site offers and reaches the part of it that concerns them.
**Status:** Approved
**Introduced:** 2026-08-25 with requirements revision 5 (FR-034, FR-023). Until then the start page forwarded to the event list and had no content of its own, so a visitor arriving from a shared link was given a bare list and never learned what the site was.

## Preconditions

- The visitor has opened the start page of the public website.

## Main Success Scenario

1. The visitor opens the start page.
2. The system states in one screen what the platform is and who runs it.
3. The system presents an extract of the events that are still ahead, with the way to the complete list.
4. The system presents an extract of the recorded voluntary service offers, with the way to the complete overview.
5. The system offers the way to the working group for a visitor who does not yet know what suits them.
6. The visitor chooses the part of the site that concerns them and continues there.

## Alternative Flows

### A1: Nothing is currently announced

**Trigger:** No event has a display date of today or later (step 3)
**Flow:**

1. The system explains in place of the extract that nothing is currently announced and offers the way to the working group instead.
2. Use case continues at step 4.

### A2: No voluntary service offers recorded

**Trigger:** No voluntary service offer exists (step 4)
**Flow:**

1. The system explains in place of the extract that no offer is currently recorded and offers the way to the working group instead.
2. Use case continues at step 5.

### A3: The visitor arrived at a section directly

**Trigger:** The visitor opened the event list, the offer overview or an information page instead of the start page (step 1)
**Flow:**

1. The system presents that page directly, unchanged and under its own address.
2. Use case ends, and the use case of that page applies instead.

### A4: The visitor is a signed-in administrator

**Trigger:** An administrator session exists when the start page is presented (step 2)
**Flow:**

1. The system additionally offers the way to the maintenance functions, presented as a separate and quieter group than the controls meant for visitors.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The visitor knows what the platform offers and has reached the section that concerns them.
- No data is changed.

### Failure Postconditions

- No data is changed; where a section has nothing to show, the visitor is given an explanation and another way onward rather than an empty area.

## Business Rules

### BR-047: The start page answers instead of forwarding

Opening the start page presents content, not a forwarding to another page. The typical visitor arrives by following a link shared in a chat group and has not chosen this site from a menu — the first screen therefore has to say what the platform is before it offers anything to do.

### BR-048: The start page shows extracts, never the whole list

The start page presents only a small extract of the events and of the offers, and each extract names the way to its complete list. The complete lists keep their own addresses and remain reachable directly, so that a link to a list shared earlier still leads where it did before.

### BR-049: A section with nothing to show explains itself

Where an extract has nothing to present, the section states why and offers a way onward. The platform announces what volunteers happen to have collected, so an empty section is a normal state, not a fault — but an empty area on the first screen tells the visitor the site is abandoned.

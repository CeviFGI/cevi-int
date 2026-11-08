# Use Case: Subscribe to the News Feed

## Overview

**Use Case ID:** UC-013
**Use Case Name:** Subscribe to the News Feed
**Primary Actor:** Besucher
**Goal:** A visitor subscribes to the News Feed in their feed reader, so that new and changed Voluntary Service Offers and Upcoming Events reach them without visiting the site.
**Trigger:** The visitor wants to follow the platform from their feed reader.
**Status:** Tested

**Requirements:** [FR-048, FR-049, NFR-031, NFR-042, C-021](../requirements.md)
**Layers:** UI

## Preconditions

- The visitor is reading any public page of the platform.

## Main Success Scenario

1. The visitor asks for the News Feed through the link to it that every public page shows.
2. The system delivers the News Feed under its own Public Address (BR-001).
3. The visitor hands the address of the News Feed to their feed reader and subscribes.
4. The feed reader fetches the News Feed; the system delivers one Feed Item for every Voluntary Service Offer and every Upcoming Event, the most recent Last Change first (BR-002, BR-003, BR-004).
5. The feed reader shows the Feed Items; the visitor opens one.
6. The system presents the Event Page or the Offer Page of the Feed Item under its Public Address.

## Alternative Flows

### A1: The feed reader finds the News Feed by itself

**Trigger:** The visitor hands the address of a public page instead of the News Feed to their feed reader, or uses a browser that offers to subscribe on its own (step 1)
**Flow:**

1. The feed reader or the browser learns the address of the News Feed from the page itself (BR-001).
2. Use case continues at step 4.

### A2: There is nothing to report

**Trigger:** The platform holds no Voluntary Service Offer and no Upcoming Event (step 4)
**Flow:**

1. The system delivers the News Feed without Feed Items.
2. The feed reader keeps the subscription and shows the Feed Items of later fetches.
3. Use case ends.

### A3: An entry has changed since the last fetch

**Trigger:** An administrator has created or changed an Event or a Voluntary Service Offer since the feed reader last fetched the News Feed (step 4)
**Flow:**

1. The system delivers the entry as a Feed Item with a new identity, carrying the entry's current details; a changed entry is marked as updated (BR-005).
2. The feed reader shows the Feed Item as new and unread.
3. Use case continues at step 5.

### A4: The entry behind a Feed Item no longer exists

**Trigger:** The visitor opens a Feed Item whose Event or Voluntary Service Offer has meanwhile been removed or whose Short Name has changed (step 6)
**Flow:**

1. The system presents the Not-Found Page with the way back to the Event List or the Offer List.
2. Use case ends.

## Postconditions

### Success Postconditions

- The visitor's feed reader holds a subscription to the News Feed and shows a Feed Item for every Voluntary Service Offer and every Upcoming Event the News Feed lists.
- No data is stored or changed on the platform, and the platform does not know who subscribed.

### Failure Postconditions

_None — the use case changes no data._

## Business Rules

### BR-001: The News Feed is named on every public page

Every public page names the News Feed in two ways: to the browser and the feed reader in a form they detect without the visitor's help, and to the visitor as a visible link labelled *RSS-Feed* in the footer of the page. The News Feed has one fixed Public Address (`/feed.xml`), built from the scheme, host and port under which the visitor's request reached the platform, as for sharing without script (UC-009 BR-003). Both work without script (C-021).

### BR-002: What the News Feed lists

The News Feed lists every Voluntary Service Offer and every Upcoming Event (UC-001 BR-001) as one Feed Item each, the most recent Last Change first, and at most 50 Feed Items. An Event whose Display Date has passed is no longer listed, and a removed entry is no longer listed, both from the next fetch on; a feed reader may still show what it fetched before.

### BR-003: What a Feed Item holds

A Feed Item holds, for an Event, its title as the title of the item, and its Date Text and location before its complete Description as the text of the item; for a Voluntary Service Offer, its Organisation and location as the title of the item, and its complete Description as the text. Every Feed Item names its kind — *Anlass* or *Volontariat* — and its Last Change as the moment of publication, and links to its Event Page or Offer Page under its Public Address. The Description appears as it is stored, already reduced to the Allow-List (NFR-010).

### BR-004: The Last Change is kept by the system

The system sets the Last Change of an Event or a Voluntary Service Offer to the moment it is created, and moves it on every save in UC-002 or UC-004 that changes at least one of its details; a save that changes nothing leaves it as it is. The administrator never enters the Last Change. An entry that existed before the News Feed carries the moment the News Feed was introduced, both as its creation and as its Last Change, so that it appears once as new.

### BR-005: A change appears as a new Feed Item

The identity of a Feed Item is made of the entry and its Last Change: it stays the same between fetches as long as the entry does not change, and is new after every change, so a feed reader shows a changed entry again as unread. A Feed Item whose Last Change lies after the creation of its entry is marked as updated by the prefix *Aktualisiert:* in its title. Only the current state of an entry is listed; the Feed Item of its previous state leaves the News Feed.

### BR-006: Subscribing reveals nothing

Delivering the News Feed is logged like any other page view and nothing more (NFR-031): no counter of subscribers and no record of who fetched it. The News Feed and its Feed Items refer only to the platform's own addresses and issue no request to another host (NFR-042), apart from pictures that an administrator has placed in a Description.

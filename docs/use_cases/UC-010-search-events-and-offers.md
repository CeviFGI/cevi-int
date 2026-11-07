# Use Case: Search Events and Offers

## Overview

**Use Case ID:** UC-010
**Use Case Name:** Search Events and Offers
**Primary Actor:** Besucher
**Goal:** A visitor finds the Upcoming Events and Voluntary Service Offers that mention what they are looking for — a country, a topic, an organisation — without reading through the Event List and the Offer List one entry after another.
**Trigger:** The visitor enters a Search Term in the search field and starts the search.
**Status:** Approved
**Introduced:** 2026-11-06 with requirements revision 7 (FR-041, FR-042). Until then a visitor could find an event or offer only by reading both lists, and the full Description of an offer only by opening each one in place.
**Revised:** 2026-11-07 for requirements revision 8 (FR-043): every offer has an Offer Page of its own, so a match on an offer now leads to that page, as a match on an event leads to its Event Page, instead of to the offer's entry in the Offer List.

**Requirements:** [FR-041, FR-042, NFR-041, NFR-046, NFR-047, C-021](../requirements.md)
**Layers:** UI

## Preconditions

- The visitor has opened a public page of the platform.

## Main Success Scenario

1. The visitor enters a Search Term in the search field and starts the search.
2. The system searches the Upcoming Events and all Voluntary Service Offers for the Search Term.
3. The system presents the Search Results Page: the Search Term in the search field, the number of matches, and the matching Upcoming Events and the matching Voluntary Service Offers in two separate groups, each entry with its title or Organisation, its location, for an event its Date Text, and an excerpt of its Description that shows where the Search Term occurs in it (BR-008).
4. The visitor picks the entry that interests them.
5. The system leads the visitor to that entry: for an event to its Event Page, for an offer to its Offer Page (UC-003 A4).
6. The visitor reads the event or offer they were looking for.

## Alternative Flows

### A1: Live Results while typing

**Trigger:** The visitor's browser runs scripts and the visitor has typed at least two letters or digits in the search field without starting the search yet (step 1)
**Flow:**

1. As soon as the visitor pauses typing, the system shows Live Results below the search field: at most five matching Upcoming Events and at most five matching Voluntary Service Offers, each with its title or Organisation and location, and a link to all results on the Search Results Page.
2. The system announces the number of Live Results to assistive technology and lets the visitor move through them by keyboard.
3. When the visitor keeps typing, the system replaces the Live Results with those for the changed Search Term; when fewer than two letters or digits remain in the field, or the visitor leaves it, the system removes them.
4. When the visitor picks a Live Result, use case continues at step 5; when the visitor starts the search instead, use case continues at step 2.

### A2: No match

**Trigger:** Neither an Upcoming Event nor a Voluntary Service Offer matches the Search Term (step 3)
**Flow:**

1. The system presents the Search Results Page with the Search Term in the search field, states that nothing matched it, and offers the way to the Event List, to the Offer List and to the Contact Form.
2. The visitor changes the Search Term and starts the search again, or leaves the search.
3. Use case continues at step 2 when the visitor searches again; otherwise use case ends.

### A3: Search Term too short or empty

**Trigger:** The Search Term contains fewer than two letters or digits (step 1)
**Flow:**

1. The system does not search and presents the Search Results Page with the search field and the hint that a Search Term needs at least two letters or digits. A Search Term of spaces and punctuation only, such as `--`, counts as empty.
2. Use case continues at step 1.

### A4: Search Term too long

**Trigger:** The Search Term is longer than 100 characters (step 1)
**Flow:**

1. The system searches for the first 100 characters of the Search Term only and shows the shortened Search Term in the search field.
2. Use case continues at step 2.

### A5: Matches only in one group

**Trigger:** Only Upcoming Events or only Voluntary Service Offers match the Search Term (step 3)
**Flow:**

1. The system presents the group that has matches and leaves out the other group entirely instead of showing it empty.
2. Use case continues at step 4.

### A6: The visitor opens a Search Results Page by its address

**Trigger:** The visitor opens a bookmarked or shared address of a Search Results Page instead of entering a Search Term (step 1)
**Flow:**

1. The system takes the Search Term from the address.
2. Use case continues at step 2.

### A7: The chosen offer is no longer available

**Trigger:** The offer the visitor picked was deleted, or its Short Name changed, after the Search Results Page was presented (step 5)
**Flow:**

1. The system informs the visitor that the requested page does not exist and offers the way to the Offer List, as UC-003 A5 does.
2. Use case ends.

### A8: The chosen event is no longer available

**Trigger:** The event the visitor picked was deleted, or its Short Name changed, after the Search Results Page was presented (step 5)
**Flow:**

1. The system informs the visitor that the requested page does not exist and offers the way back to the Event List, as UC-001 A2 does.
2. Use case ends.

## Postconditions

### Success Postconditions

- The visitor has reached the Event Page or the Offer Page of a match for their Search Term.
- No data is stored or changed; the Search Term is not recorded.

### Failure Postconditions

_None — the use case changes no data._

## Business Rules

### BR-001: The search covers exactly what the public lists show

The search finds Upcoming Events and Voluntary Service Offers, and nothing else. An Event whose Display Date has passed is not found, just as it is hidden from the Event List (UC-001 BR-001); every Voluntary Service Offer can be found, just as every offer is on the Offer List (UC-003 BR-001). An Administrator Session changes nothing: a signed-in Administrator finds the same entries as any visitor. Information Pages, Contact Messages, User Accounts and Exchange Offers are never searched.

### BR-002: Every word a visitor reads is searchable

For an Event the search covers its title, Date Text, location and Description; for a Voluntary Service Offer its Organisation, location and Description. A Description is searched as the text a visitor reads on the page, not as its stored markup: formatting, link addresses and picture data are not matched, the visible text of a link or a table is.

### BR-003: All words must match, each from its beginning

A Search Term of several words finds an entry only when every word occurs in it, in any field and in any order. A word matches a word of the entry that begins with it, so `pole` finds `Polen` and `freiwillig` finds `Freiwilligeneinsatz`. Every character that is neither a letter nor a digit separates two words, in the Search Term and in the entry alike: `YMCA/YWCA` and `Cevi-Lager` are two words each, so `ywca` finds `YMCA/YWCA` and `lager` finds `Cevi-Lager`. Upper and lower case are not distinguished, and a letter with an accent, umlaut or other mark matches the same letter without it (`Poděbrady` is found by `podebrady`, `Zürich` by `zurich`); `ł`, `ø` and `đ` match `l`, `o` and `d`. `ß` is not replaced: it matches only `ß`.

### BR-004: Better matches first

Within each group the entries are ordered by how well they match. An entry ranks by the best field in which any word of the Search Term occurs: title or Organisation first, then location or Date Text, then Description. Entries of the same rank follow a fixed order: events by Display Date, nearest first, as on the Event List (UC-001 BR-002); offers by Organisation in alphabetical order. Entries that are still equal follow the order in which they were recorded, the earlier first. The same Search Term over the same stored entries therefore always yields the same order.

### BR-005: A Search Results Page has its own address

The Search Results Page carries the Search Term in its address, so that opening the same address again shows the results for the same Search Term at that moment. Searching changes no data and records nothing: the platform stores neither the Search Term nor who searched, in its database or in its application log. The application log names the address of a search without its Search Term, as it does for every request. The rule covers the platform itself; what a reverse proxy in front of it logs is a matter of its operation, not of this use case.

### BR-006: The search works without script

Entering a Search Term, starting the search, reading the Search Results Page and following a match work in a browser that runs no script. Live Results (A1) only add to that; a visitor without them loses nothing but the convenience (C-021).

### BR-007: The search field is reachable from every public page

Every public page offers the search field in the same place, so that a visitor can start a search wherever they are without first navigating to a search page.

### BR-008: The excerpt shows where the Search Term occurs

The excerpt of a match on the Search Results Page is at most 160 characters of the Description's visible text, as long as the excerpt on the lists (FR-035). It begins shortly before the earliest place in the Description at which any word of the Search Term occurs, and an ellipsis marks each end at which text was cut. When no word of the Search Term occurs in the Description, the excerpt is the beginning of the Description, as on the lists. The matching words are not highlighted.

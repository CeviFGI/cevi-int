# Use Case: Browse Voluntary Service Offers

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Browse Voluntary Service Offers
**Primary Actor:** Besucher
**Goal:** A visitor gets an overview of the voluntary service opportunities the working group recommends, so they can decide which organisation to approach.
**Status:** Implemented

## Preconditions

- The visitor has opened the public website.

## Main Success Scenario

1. The visitor opens the voluntary service section of the site.
2. The system presents all recorded offers with the offering organisation, the location, a description and a link to the organisation.
3. The visitor follows the link of an offer that interests them to the website of the organisation.

## Alternative Flows

### A1: No offers recorded

**Trigger:** No voluntary service offer exists (step 2)
**Flow:**

1. The system presents an empty list.
2. Use case ends.

## Postconditions

### Success Postconditions

- The visitor has seen all published voluntary service offers.
- No data is changed.

### Failure Postconditions

- No data is changed.

## Business Rules

### BR-012: All offers are shown

Voluntary service offers are not filtered by date or any other criterion — every recorded offer is visible to every visitor.

### BR-013: Every offer points to its organisation

Each offer names the organisation that runs it and links to that organisation's own website, because the application only advertises the offer and does not administer applications.

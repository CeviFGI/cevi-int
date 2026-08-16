# Use Case: Sign In as Administrator

## Overview

**Use Case ID:** UC-006
**Use Case Name:** Sign In as Administrator
**Primary Actor:** Administrator
**Goal:** An administrator identifies themselves so that the maintenance functions for events and voluntary service offers become available, and gives up that access again when finished.
**Status:** Implemented

## Preconditions

- A user account with the administrator role exists.

## Main Success Scenario

1. The administrator opens the sign-in page.
2. The system presents a form asking for user name and password.
3. The administrator enters their user name and password and submits the form.
4. The system verifies the credentials against the stored user accounts.
5. The system establishes the session and sends the administrator to the start page.
6. The maintenance links for events and voluntary service offers are now visible to the administrator.

## Alternative Flows

### A1: Credentials not recognised

**Trigger:** The user name is unknown or the password does not match (step 4)
**Flow:**

1. The system presents an error page explaining that the sign-in failed.
2. Use case continues at step 1.

### A2: Already signed in

**Trigger:** The administrator opens the sign-in page while a session is already established (step 1)
**Flow:**

1. The system sends the administrator to the start page without asking for credentials.
2. Use case ends.

### A3: Sign out

**Trigger:** The administrator chooses to sign out (step 6)
**Flow:**

1. The system ends the session and confirms that the administrator is signed out.
2. Use case ends.

### A4: Sign out without an active session

**Trigger:** The sign-out link is opened while no session is established (A3 step 1)
**Flow:**

1. The system sends the visitor to the start page.
2. Use case ends.

### A5: Maintenance function opened without a session

**Trigger:** Anyone opens a maintenance function while not signed in
**Flow:**

1. The system sends them to the sign-in page.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The administrator has an established session and the maintenance functions are accessible.
- After signing out, the session is ended and the maintenance functions are no longer accessible.

### Failure Postconditions

- No session is established and the maintenance functions stay inaccessible.

## Business Rules

### BR-021: Only accounts with the administrator role may maintain content

Access to the maintenance functions depends on the administrator role held by the account, not merely on being signed in.

### BR-022: Passwords are never stored readably

Passwords are stored only in an irreversible form, so a copy of the stored accounts does not reveal them.

### BR-023: Maintenance links are shown only to signed-in users

Edit and delete links on the public pages appear only for a visitor with an established session.

### BR-024: An established session survives page changes

Once signed in, the administrator stays signed in across pages until they sign out or the session expires.

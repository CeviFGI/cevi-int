# Use Case: Contact the International Working Group

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Contact the International Working Group
**Primary Actor:** Besucher
**Goal:** A visitor asks the international working group for advice or support and reaches a real person without needing an account.
**Status:** Implemented

## Preconditions

- The visitor has opened the public website.

## Main Success Scenario

1. The visitor opens the contact page.
2. The system presents the kinds of support the working group offers, a free-text field for the message and a simple spam protection question.
3. The visitor writes the message, including a way to be reached if a reply is wanted, and answers the spam protection question.
4. The visitor submits the form.
5. The system checks the answer to the spam protection question.
6. The system records the message.
7. The system notifies the working group of the new message by e-mail.
8. The system confirms to the visitor that the message has been sent.

## Alternative Flows

### A1: Spam protection answered incorrectly

**Trigger:** The spam protection field is empty or does not contain the expected answer (step 5)
**Flow:**

1. The system presents the form again with the written message preserved and explains which answer is expected.
2. Use case continues at step 3.

### A2: Message cannot be recorded

**Trigger:** The message cannot be stored (step 6)
**Flow:**

1. The system discards the incomplete recording.
2. Use case continues at step 7, so the message still reaches the working group.

### A3: Notification cannot be delivered

**Trigger:** The e-mail notification cannot be sent (step 7)
**Flow:**

1. The system notes the failure for the operators.
2. Use case continues at step 8 — the visitor is not confronted with the delivery problem.

## Postconditions

### Success Postconditions

- The message is recorded and the working group has been notified by e-mail.
- The visitor sees a confirmation.

### Failure Postconditions

- The message is not recorded and no notification is sent; the visitor sees the form again with the message preserved.

## Business Rules

### BR-017: Spam protection question must be answered

A message is only accepted when the spam protection field contains the expected number. This keeps automated submissions out without requiring the visitor to register.

### BR-018: Contact messages are recorded and forwarded

Every accepted message is both recorded for later reference and forwarded by e-mail, so a message is not lost if one of the two paths fails.

### BR-019: Messages go to one fixed recipient

Notifications are always sent to the single address configured for the contact form; the visitor cannot choose the recipient.

### BR-020: No sender data is requested

The form asks only for the message text. If the visitor wants a reply, they include their own contact details in the message.

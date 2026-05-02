# Bill Management Row Layout Design

## Goal

Refine the client-side bill management view so records are easier to scan in one pass. Replace the current expandable card list with a denser, table-like single-row layout that still feels modern and interactive.

## Current Problem

The existing bill list uses stacked cards with inline expansion. This makes each record visually tall, causes the page rhythm to break when one row expands, and reduces the number of visible records per screen. For bill management, the user’s primary task is rapid comparison across many rows, not reading one card at a time.

## Proposed Interaction Model

### List View

- Keep the current bill management section, filters, batch tools, and pagination in place.
- Replace the current `bill-row` card presentation with a single-row table-like layout.
- Each row shows:
  - selection checkbox
  - bill number
  - vessel/voyage
  - goods name
  - quantity
  - status
- Each row stays at a stable height and should not expand inline.
- Rows remain fully clickable except for the checkbox.

### Hover Behavior

- On mouse hover, apply a subtle visual lift:
  - a very small scale-up
  - a slightly stronger shadow
  - a cleaner surface highlight
- The effect should be restrained so the interface feels responsive without becoming noisy or jumpy.

### Detail View

- Clicking a row opens a centered modal dialog instead of expanding the row.
- The dialog contains the full bill detail fields currently shown in the inline expanded panel.
- The dialog header should emphasize:
  - bill number
  - current status
- The dialog body should organize fields into a clean grid for fast reading.
- The dialog footer or action area should include:
  - edit
  - delete
  - close

## Visual Direction

- Make the list feel closer to a professional data table than a stack of cards.
- Preserve the existing light, calm visual system introduced in the recent refresh.
- Use row separators, column alignment, and tighter spacing to improve information density.
- Keep the modal consistent with the project’s current rounded, soft-surface design language.

## State and Behavior Requirements

- Existing selection behavior must continue to work.
- Existing edit and delete actions must continue to work.
- Existing batch delete flow must continue to work.
- Existing empty state and pagination must remain intact.
- Only the presentation and detail interaction model should change; no data logic or API behavior should change.

## Scope

- Modify `frontend/client/src/App.vue`
- Modify `frontend/client/src/styles.css`

## Non-Goals

- No change to admin screens.
- No backend or API changes.
- No change to filter semantics or pagination behavior.
- No redesign of template management, extraction, or export views.

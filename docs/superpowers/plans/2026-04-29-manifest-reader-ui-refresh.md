# Manifest Reader UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refresh the client and admin UIs so they feel cleaner, more consistent, and easier to operate without changing business behavior.

**Architecture:** Keep the implementation inside the existing Vue app shells. Update template-level affordances in `App.vue` and centralize the visual overhaul in each app's `styles.css`.

**Tech Stack:** Vue 3, Vite, plain CSS

---

### Task 1: Update client workspace structure

**Files:**
- Modify: `frontend/client/src/App.vue`

- [ ] Replace symbolic navigation glyphs with inline SVG icon mappings.
- [ ] Add a compact summary strip to the client workspace header.
- [ ] Keep existing view logic unchanged while improving orientation affordances.

### Task 2: Refresh client visual system

**Files:**
- Modify: `frontend/client/src/styles.css`

- [ ] Update color tokens, shadows, and surface treatments for a calmer light theme.
- [ ] Restyle navigation, buttons, inputs, header surfaces, and list rows.
- [ ] Preserve responsive behavior while improving spacing and hierarchy.

### Task 3: Update admin workspace structure

**Files:**
- Modify: `frontend/admin/src/App.vue`

- [ ] Replace symbolic navigation glyphs with inline SVG icon mappings.
- [ ] Add a compact summary strip to the admin workspace header.
- [ ] Keep existing admin flows intact while improving scanability.

### Task 4: Refresh admin visual system

**Files:**
- Modify: `frontend/admin/src/styles.css`

- [ ] Update tokens and surfaces to match the shared client direction.
- [ ] Restyle sidebar, topbar, buttons, forms, charts, summary cards, and tables.
- [ ] Preserve role-based behavior and modal usability.

### Task 5: Verification

**Files:**
- Verify: `frontend/package.json`

- [ ] Run `npm run build` from `frontend/`.
- [ ] Fix any compile or style regression reported by Vite.
- [ ] Summarize the final UI changes and any residual limitations.

# Manifest Reader UI Refresh Design

## Goal

Refresh both `client` and `admin` surfaces with a calmer, more premium UI language that balances Apple-like restraint with Google-like clarity and operational efficiency.

## Visual Direction

- Use a light neutral canvas with a cool blue accent and restrained shadows.
- Replace symbolic text icons with a unified outline SVG set.
- Shift from heavy dark chrome to lighter structural grouping, clearer spacing, and softer cards.

## Layout Decisions

- Keep the existing information architecture and page logic intact.
- Add a summary strip to workspace headers so users can orient quickly.
- Use a clearer separation between navigation, hero header, work panels, forms, and data lists.

## Component Decisions

- Primary actions use a darker, more confident filled treatment.
- Secondary and ghost actions use lighter surfaces with subtle borders.
- Inputs, filter bars, editors, and list rows adopt larger radii and softer borders for a more modern operational feel.
- Tables and list rows should feel less boxed and more grid-aligned.

## Scope

- `frontend/client/src/App.vue`
- `frontend/client/src/styles.css`
- `frontend/admin/src/App.vue`
- `frontend/admin/src/styles.css`

## Non-Goals

- No backend/API changes.
- No route or data-flow refactor.
- No new UI library dependency.

# Bill Management Row Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the client-side bill management list into a compact single-row table-like layout with subtle hover lift and a modal dialog for full bill details.

**Architecture:** Keep all bill-management behavior inside the existing client `App.vue` and `styles.css` pair. Replace the inline expanded row presentation with a modal-backed detail flow while preserving existing selection, edit, delete, filtering, and pagination logic.

**Tech Stack:** Vue 3, Vite, plain CSS

---

### Task 1: Reshape the bill list template

**Files:**
- Modify: `frontend/client/src/App.vue`

- [ ] **Step 1: Replace the inline expandable row body with a single summary row**

Update the bill list markup so each `article.bill-row` contains only one summary row and no inline detail panel. Keep the checkbox isolated from the row click target. Use a new button class for the whole row and move status into a dedicated table-like cell.

```vue
<div class="bill-list" role="list">
  <article
    v-for="bill in savedBills"
    :key="bill.id"
    class="bill-row"
    :class="{ checked: selectedBillIds.includes(bill.id) }"
    role="listitem"
  >
    <div class="bill-row-main">
      <label class="check-control row-check" @click.stop>
        <input type="checkbox" :checked="selectedBillIds.includes(bill.id)" @change="toggleBillSelection(bill.id)" />
        <span class="sr-only">选择 {{ bill.blNo }}</span>
      </label>
      <button class="bill-grid-row" type="button" @click="openBillDialog(bill)">
        <span class="bill-cell bill-cell-primary">
          <small>提单号</small>
          <strong>{{ bill.blNo }}</strong>
        </span>
        <span class="bill-cell">
          <small>船名航次</small>
          <strong>{{ bill.vessel }}</strong>
        </span>
        <span class="bill-cell">
          <small>货运商品名称</small>
          <strong>{{ bill.goodsName }}</strong>
        </span>
        <span class="bill-cell bill-cell-compact">
          <small>数量</small>
          <strong>{{ bill.quantity }}</strong>
        </span>
        <span class="bill-cell bill-cell-status">
          <small>状态</small>
          <span class="pill">{{ bill.status }}</span>
        </span>
      </button>
    </div>
  </article>
</div>
```

- [ ] **Step 2: Add modal dialog markup after the bill list section**

Insert a detail dialog that renders the currently selected bill and reuses the existing field list for the detail grid. Keep edit and delete actions inside the modal.

```vue
<section v-if="billDetailDialog.open && activeBillDetail" class="bill-detail-dialog-backdrop" @click.self="closeBillDialog">
  <article class="bill-detail-dialog">
    <header class="bill-detail-dialog-head">
      <div>
        <p class="eyebrow">Bill Detail</p>
        <h2>{{ activeBillDetail.blNo }}</h2>
        <span class="pill">{{ activeBillDetail.status }}</span>
      </div>
      <button class="ghost-button" type="button" @click="closeBillDialog">关闭</button>
    </header>

    <div class="detail-grid bill-detail-dialog-grid">
      <div v-for="field in activeBillDetail.detailFields" :key="field.label" class="detail-field">
        <span>{{ field.label }}</span>
        <strong>{{ field.value }}</strong>
      </div>
    </div>

    <footer class="bill-detail-dialog-actions">
      <button class="secondary-button" type="button" @click="startEditBill(activeBillDetail)">编辑</button>
      <button class="danger-button" type="button" @click="removeBill(activeBillDetail)">删除</button>
      <button class="ghost-button" type="button" @click="closeBillDialog">关闭</button>
    </footer>
  </article>
</section>
```

- [ ] **Step 3: Add dialog state and handlers in script setup**

Introduce a small dialog state object and two helpers so the list can open and close the modal without changing existing CRUD logic.

```js
const billDetailDialog = reactive({
  open: false,
  billId: "",
});

const activeBillDetail = computed(() => savedBills.value.find((bill) => bill.id === billDetailDialog.billId));

function openBillDialog(bill) {
  billDetailDialog.billId = bill.id;
  billDetailDialog.open = true;
}

function closeBillDialog() {
  billDetailDialog.open = false;
  billDetailDialog.billId = "";
}
```

- [ ] **Step 4: Close the detail dialog when edit mode begins**

Update the edit entrypoint so opening the editor dismisses the dialog first, preventing overlapping surfaces.

```js
function startEditBill(bill) {
  closeBillDialog();
  billEditor.open = true;
  billEditor.mode = "edit";
  billEditor.editingId = bill.id;
  billEditor.error = "";
  Object.assign(billForm, mapBillToForm(bill));
}
```

### Task 2: Restyle the bill list into a compact row layout

**Files:**
- Modify: `frontend/client/src/styles.css`

- [ ] **Step 1: Replace the old card-row layout with a table-like row grid**

Remove the old `.bill-summary` and inline expansion styling, and add compact row styles with aligned cells.

```css
.bill-list {
  display: grid;
  gap: 10px;
}

.bill-row {
  border: 1px solid rgba(142, 156, 179, 0.12);
  border-radius: 16px;
  background: rgba(248, 251, 255, 0.9);
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.bill-row:hover {
  transform: scale(1.008);
  background: #ffffff;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.bill-grid-row {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(130px, 1fr) minmax(180px, 1.25fr) minmax(200px, 1.4fr) minmax(90px, 0.65fr) minmax(100px, 0.7fr);
  gap: 14px;
  align-items: center;
  border: 0;
  padding: 16px 18px;
  background: transparent;
  color: var(--ink);
  text-align: left;
}

.bill-cell {
  min-width: 0;
}

.bill-cell strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
```

- [ ] **Step 2: Add checked-state and dense row helpers**

Preserve the selection highlight and tighten the row shell so the list remains easy to scan when many rows are visible.

```css
.bill-row.checked {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.14), 0 16px 30px rgba(15, 23, 42, 0.06);
}

.bill-row-main {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  padding-left: 14px;
}

.bill-cell-compact,
.bill-cell-status {
  justify-self: start;
}
```

- [ ] **Step 3: Add modal dialog styles**

Create a centered overlay and modal surface that match the current design system.

```css
.bill-detail-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(10px);
}

.bill-detail-dialog {
  width: min(880px, 100%);
  max-height: min(82vh, 760px);
  overflow: auto;
  border: 1px solid rgba(142, 156, 179, 0.16);
  border-radius: 24px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 28px 60px rgba(15, 23, 42, 0.18);
}

.bill-detail-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
```

- [ ] **Step 4: Update responsive behavior for the new row grid**

Adjust the existing mobile breakpoints so the list wraps cleanly when space is limited.

```css
@media (max-width: 960px) {
  .bill-grid-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bill-cell-status {
    grid-column: 1 / -1;
  }

  .bill-detail-dialog {
    padding: 18px;
  }
}
```

### Task 3: Verification

**Files:**
- Verify: `frontend/package.json`

- [ ] **Step 1: Run the production build**

Run:

```bash
npm run build
```

Expected: both `admin` and `client` Vite builds succeed with exit code `0`.

- [ ] **Step 2: Sanity-check the targeted UI behavior**

Verify manually in the client bill management screen:

```text
1. Each bill appears as a single compact row.
2. Hovering a row gives a subtle lift effect.
3. Clicking a row opens a centered detail dialog.
4. Checkbox selection still works without opening the dialog.
5. Edit/Delete from the dialog still work as before.
```

- [ ] **Step 3: Summarize residual risks**

Record whether the current implementation still needs:

```text
- column width tuning for long vessel names
- mobile breakpoint tightening
- keyboard escape support for dialog close
```

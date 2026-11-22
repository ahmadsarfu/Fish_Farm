# Medicine inventory

Implemented on `feature/medicine-inventory` for release to master. This module is independent of fish types, ponds, customers, batch costing and fish expenses. It remains farm-scoped to protect each customer's data.

## Workflow

Create a product for each sealed pack size (BOTTLE or PACKET). Units are whole numbers only. Product name/brand/pack/unit become immutable after the first receipt, preventing historical relabeling; minimum stock, suggested sale price and active status can change with version checks.

Record purchases/opening stock as receipts with quantity, purchase cost, lot number and expiry. Each receipt is a separate cost lot, even if the supplier repeats its batch number. For opening stock, put `Opening stock` in notes. Sale and own use are separate stock-out entries. Sales profit is actual selling price less that lot's purchase cost; own use creates a medicine-only cost, with no fish/batch expense. Final unit sale price can include an agreed discount. Customer dues, tax invoices, GST and payment settlement are outside this version.

Transactions use one lot at a time. UI lists lots by earliest expiry; split multi-lot sales into separate entries. Expiry is checked against the transaction date; expired stock remains visible, and damage/expired write-off or supplier return can remove it. Movements must be entered chronologically within a lot; enter old movements before newer ones. Stock is current, while financial totals use the selected date range. The expiry alert window is 30 days.

Sales returns must reference an unreversed original sale from the same lot, use its selling price, and cannot exceed its unreturned quantity. Supplier returns use purchase cost. OWNER can append a reversal with reason; MANAGER can enter transactions and view history; LABOUR cannot access the module. Purchase reversal is restricted once its lot has later entries; use a supplier return for remaining packs. Original entries are retained. Negative stock is rejected atomically under a lot lock. UUID keys deduplicate retried receipts/movements/reversals and reject changed payloads; writes are not automatically replayed by the UI.

## Database and rollout

Run `docs/sql/medicine-inventory-upgrade.sql` after selecting the existing application database. It creates exactly three additive tables and can be repeated. It does not alter fish tables or change existing rows. `schema-full.sql` is for fresh, empty databases only. No live database changes have been made from this workspace.

The 10 MB quota is shared with existing farm data. Measure actual provider usage after creating the tables. Stock and summaries are aggregate queries over the ledger; there are no daily snapshots or stored exports/files. Names are kept in the product/lot records, not copied into every movement. The movement itself stores actor/time/reason; duplicate farm-audit rows are not written. History pages are limited to 100 rows, default 25. Exports are generated on demand, limited to 10,000 rows per date range. CSV includes raw movement directions/references, including original and reversing rows, and must not be interpreted as a list of active sales only.

A database quota increase will eventually be required as retained history grows. No automatic record deletion is implemented. Never remove indexes simply to free space; tenant, locking and idempotency constraints protect correctness.

Deploy migration and backend before the matching frontend. There is no medicine data migration or copy from fish tables.

## Validation

MySQL integration tests exercise 100 received / 30 sold / 10 own use = 60 remaining, financial separation, returns/reversals, unchanged historical prices, idempotency, whole-unit validation, expired stock, concurrent overselling, CSRF, farm isolation and role restrictions. Browser tests use fixtures for purchase/sale/own-use, mobile navigation, owner-only reversal and CSV download. Production availability is not verified by these tests.

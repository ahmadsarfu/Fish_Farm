# Feeding record review

Feature branch: `feature/feeding-record-integrity` in both Fish_Farm and Fish_Farm_UI.

Daily checklist completion is permanent through the checklist. The history page is read-only; a farm OWNER can open Review to correct quantity, cancel, or restore a cancelled entry with a 5–500 character reason. MANAGER can view the revision history; LABOUR only sees the assigned checklist. Missed entries require an OWNER and a reason. Feed master rate changes only affect new entries.

Every review retains original quantity, saved rate and original expense, plus an append-only revision containing reviewer, time, reason, before/after values and expense difference. Cancelled rows remain visible with zero net quantity and expense. Restore reuses the original row. Summary/checklist totals use the effective value once; revision adjustments are audit history and must not be added again. Existing records receive a snapshot of their values at migration time; earlier unlogged edits/deletions cannot be reconstructed.

Review locks the pond and record, checks the expected version, and updates feed, batch allocation and revision atomically. Stale concurrent reviews return 409. Costs already carried into transfers or harvests remain protected; rejected reviews roll back completely. Legacy direct update/delete endpoints are denied. The system keeps correction history in the application database; privileged database operators can still modify data directly.

## Deployment order

These steps are for a later approved release; this branch does not change production.

1. Back up the existing database, including daily_feeds and batch cost tables. Confirm the existing farm/platform schema migrations are present.
2. Enter maintenance and pause all feeding writes, including old application instances. Do not run this migration while the master backend accepts writes: its inserts do not supply the new required original-value columns.
3. Run `docs/sql/feeding-record-integrity-upgrade.sql` against the selected application database (phpMyAdmin SQL/import supports its delimiters). It adds columns, backfills original values and creates feed_revisions. It does not delete feeding rows. MySQL DDL commits independently: retain the backup and inspect any reported error before proceeding.
4. Deploy the backend feature commit, then the matching frontend commit. Keep maintenance enabled until both are ready.
5. Verify owner correction/cancel/restore, manager read-only access, labour locked checklist, and net expense/batch totals on controlled test data before reopening writes.

The migration can be rerun without replacing populated original snapshots or revisions. `schema-full.sql` includes this feature for fresh installs only; never run that script against existing data. Rollback requires coordinated code/database restoration or a forward fix; simply returning to the old backend will leave feeding inserts incompatible with the required columns.

## Verification

The MySQL integration suite covers original rate retention, owner/farm boundaries, locked deletion/unticking, cancellation and restore totals, concurrent reviews, and transaction rollback after a batch transfer. Browser tests use mocked API responses to verify owner/manager/labour controls, mandatory reasons, review failures and mobile layout; they do not assert production availability. The migration was also applied twice to a disposable database created from the master schema with an existing feeding row, confirming the original snapshot survived a changed current value.

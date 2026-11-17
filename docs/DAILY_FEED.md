# Daily Feed Management

Apply `docs/sql/daily-feed-management.sql` manually to the application's existing
MySQL 8 database before starting the updated API. Select the correct database and
back it up first. There is no automatic migration and `ddl-auto=none` remains unchanged.
The script adds feed master, feeding ledger and pond setup tables and may be rerun; `IF NOT EXISTS` does not repair an
incompatible pre-existing table. No production credentials or rates are included.

## One-tap daily checklist

Open **Daily Feed > Pond Feed Setup** once and configure an active feed plus AM and
PM quantities for each active pond. Use zero to disable one slot; at least one
quantity must be positive. Setup persists in the existing `pond_feed_configs` table.
Existing configurations are loaded without re-entry. Actual ponds come from the
pond database. The same configured feed is used for both times, with independent
quantities.

The default **Daily Checklist** shows a date, a row per pond, large AM/PM ticks,
completed/planned slots, a progress bar, and actual saved cost. Labour selects the
date and ticks only after feeding. The server saves the configured quantity and
current rate atomically. A repeated or concurrent tick is idempotent: it does not
create another record or charge. Completed slots show saved quantities and cost;
subsequent setup or master-price changes do not reprice or resize them. A zero-rate
or inactive feed cannot be completed. Future feeding cannot be marked complete.
Unconfigured ponds are shown with a setup prompt and excluded from planned totals.

Unticking requires confirmation and removes the saved entries for that pond/date/
time, reversing their feed cost. Existing manually recorded entries already count
as a completed slot; the tick does not add another charge. If a slot contains several
manual feed entries, undo removes all of them, as stated in the confirmation.
Existing feed records remain accessible under **Feed Master & Records**.

The checklist uses `daily_feeds` as its ledger. Existing legacy `feeding_records`
and its `expenses` links are preserved, without copying or double-counting them.
Legacy ledger entries are not automatically migrated into this checklist. Existing
`pond_feed_configs` tables with the documented columns can be used directly; the
checklist requires no new columns on those tables. `feed_types` must expose `size_mm`
and `name`, and `daily_feeds` must already exist. Fresh installs use
`docs/sql/daily-feed-management.sql`.

The dashboard follows Controller → Service interface → ServiceImpl → Repository →
Entity. Pond row locks serialize setup and checkbox writes; manual mutations also
lock the selected pond.

### Checklist API

- `GET /api/feeding-dashboard?date=2026-11-17`
- `PUT /api/pond-feed-configs/{pondId}?date=2026-11-17` with
  `{"feedTypeId":1,"morningQtyKg":2,"eveningQtyKg":2}`
- `PUT /api/feeding-dashboard/{pondId}/slots/MORNING` (or `EVENING`) with
  `{"date":"2026-11-17","completed":true}`; send false to undo.

Mutation responses return the dashboard for the selected date. No role or login
system is added; deployment access follows existing application policy.

## Detailed records and feed master

In the React app, open **Daily Feed > Feed Master & Records**. In **Feed Master**, create one record for each
size (1, 2, 3, 4 mm), with a name and actual INR/kg rate. Masters may be edited or
made inactive; their size cannot change. Then record a pond, date, morning/evening,
feed and quantity in kg. Dates cannot be in the future. Quantities allow three
decimal places and prices two. Different sizes can be recorded for the same session.
A second record for the same pond/date/time/feed is rejected: edit its quantity instead.

The server calculates `expense = ROUND(quantityKg * savedRatePerKg, 2)` using decimal
HALF_UP rounding. The rate is captured when the entry is created, so master price
changes never reprice old entries. Editing quantity retains its rate; selecting a
different feed captures that feed's current rate. Existing entries using inactive
feeds/ponds remain editable; new selections must be active. Deleting an entry removes
its feed expense after confirmation. The feed ledger is the source of feed expenses;
it does not write duplicate rows into the existing, unused generic `expenses` table.

History and totals support inclusive from/to dates and an optional pond filter.
Frontend totals use the server summary. Existing farm pages remain available.
The app retains the existing API default of `http://localhost:8080`; set
`VITE_API_BASE_URL` for a different backend before starting/building Vite.

## API

- `GET /api/feed-types`
- `POST /api/feed-types`, `PUT /api/feed-types/{id}`:
  `{"sizeMm":1,"name":"Starter","ratePerKg":50.25,"active":true}`
- `GET /api/daily-feeds?from=2026-11-01&to=2026-11-30&pondId=1`
- `GET /api/daily-feeds/summary` with the same required date filters and optional pond.
- `POST /api/daily-feeds`, `PUT /api/daily-feeds/{id}`:
  `{"pondId":1,"feedTypeId":1,"feedingDate":"2026-11-17","feedingTime":"MORNING","quantityKg":2.500,"notes":"Optional"}`
- `DELETE /api/daily-feeds/{id}`

Missing records return 404, duplicates 409, invalid inputs 400. The database unique
constraint also prevents concurrent duplicate creation. No new authentication policy
is introduced; deployment access follows the existing application's policy.

## Validation

Run the existing Maven tests with a disposable MySQL database configured through
Spring datasource overrides. `DailyFeedIntegrationTests` and most `FeedingDashboardIntegrationTests` use rollback transactions
and expect the feed tables to exist. Run `npm ci`, `npm run build` and `npm run lint`
in Fish_Farm_UI. The UI manifest now declares its existing invoice import `jspdf`.

The checklist concurrent-tap test commits isolated fixtures to exercise separate MySQL connections, then removes its own fixtures. The suite expects an empty disposable feed database.

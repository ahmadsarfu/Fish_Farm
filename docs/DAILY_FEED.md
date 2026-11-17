# Daily Feed Management

Apply `docs/sql/daily-feed-management.sql` manually to the application's existing
MySQL 8 database before starting the updated API. Select the correct database and
back it up first. There is no automatic migration and `ddl-auto=none` remains unchanged.
The script adds two tables and may be rerun; `IF NOT EXISTS` does not repair an
incompatible pre-existing table. No production credentials or rates are included.

In the React app, open **Daily Feed**. In **Feed Master**, create one record for each
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
Spring datasource overrides. `DailyFeedIntegrationTests` uses rollback transactions
and expects the two new tables to exist. Run `npm ci`, `npm run build` and `npm run lint`
in Fish_Farm_UI. The UI manifest now declares its existing invoice import `jspdf`.

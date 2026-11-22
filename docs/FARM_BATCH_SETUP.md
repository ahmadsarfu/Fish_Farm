# Login, farms and two-stage batch costing

This release requires a database upgrade before starting the new backend. Existing
rows are retained in farm 1. Legacy `users` passwords are not imported into the new
login system. There are no default login credentials.

## Upgrade the existing local installation

Stop the old backend and frontend preview first. Keep any local Git changes; pull
only after checking `git status` and resolving or backing them up.

From a normal terminal in the backend checkout:

```bash
git pull --ff-only origin master
mysqldump -u root -p --single-transaction --routines fish_farm_db > ~/fishfarm-before-login.sql
mysql -u root -p fish_farm_db < docs/sql/farm-batch-upgrade.sql
bash scripts/start-local.sh
```

Enter your own database password at the MySQL prompts. The upgrade adapts legacy
`pellet_size_mm`/missing-name feed tables, creates missing feeding/setup tables,
adds farm ownership and audit metadata, changes globally unique names/numbers to
farm-local keys, and creates the new auth/batch tables. It does not drop business
tables or copy/duplicate expenses. It is repeatable, but MySQL DDL commits
independently: stop and investigate any error before starting the app. Inspect the
backup before applying the upgrade. Existing feed sizes outside 1–4 mm cause an
explicit migration error rather than silent conversion.

The local start helper creates a random, owner-readable setup code in the ignored
`.local/farm-setup-code` file. Read it in a separate local terminal:

```bash
cat .local/farm-setup-code
```

Open the frontend, choose **Create a farm account**, select **Use my existing farm
data**, and enter the code plus your own owner username/password. This claims farm
1 once. Existing ponds, customers, feed settings and history will be visible.
Keep the code out of chat, Git and screenshots. When using IntelliJ instead of the
helper, configure `FARM_SETUP_TOKEN` in that run configuration with a strong random
value. A shell `export` does not change an already-running IDE's environment.
The code cannot claim a farm again once an owner exists.

In the frontend checkout:

```bash
git pull --ff-only origin master
npm ci
npm run build
npm run preview
```

Local defaults allow `http://localhost:5173` and `http://localhost:4174`. Use the same
hostname for UI and API (localhost on both, rather than mixing localhost/127.0.0.1).
Other origins must be explicitly listed in the comma-separated
`APP_ALLOWED_ORIGINS` backend variable. `VITE_API_BASE_URL` chooses the API base at
build time; an empty value uses same-origin `/api` requests. Refresh the browser
once after upgrading. The new service worker never caches API responses and removes
the old `api-cache` runtime cache.

## Fresh installation

For an empty database, run `docs/sql/schema-full.sql` instead of the old schema or
upgrade file, then start the API and create a new farm owner through registration.
The fresh installer refuses to modify a database that already contains tables. It
contains structure only, with no accounts, passwords or seed business data.

## Roles and farms

- **Owner:** farm operations, batch costs, team creation, roles and assignments,
  activity history, and additional farms.
- **Manager:** existing farm operations and batch costing. Cannot manage users.
- **Labour:** today's feeding for assigned ponds only. Cannot undo ticks, change
  setup, view batch/customer/stock APIs, or edit another date. Ask a manager for
  corrections. All users can change their own password under **My Account**.

A new registration without the existing-data option creates an empty, separate
farm. Farm selection is checked against active memberships on the server. Every
mapped domain entity uses Hibernate discriminator tenancy, including child rows,
ledger records and old modules. No farm ID is accepted from request bodies as an
authority. Disabling a member takes effect on their next request. Password changes
invalidate every existing session for that account. Team members have globally
unique usernames; creating a member cannot reset an existing person's password.

Authentication uses BCrypt hashes, HttpOnly session cookies, CSRF protection,
explicit credentialed CORS origins, session-ID rotation, no-store API responses,
and login/registration throttling. Business dates and validation use
`Asia/Kolkata` (IST), also supplied to the UI. This
avoids UTC/local-midnight disagreements for labour ticks. New audit timestamps are
stored in UTC and displayed in that business timezone. Existing DATE values are
not shifted. Business dates and UI timestamps use IST in this deployment. MySQL connections and Hibernate timestamps use UTC regardless of host region. Do not expose an older unauthenticated backend
alongside the new server.

## Batch workflow

1. **Batches & Costing → Start Batch:** select fish and an empty nursery pond,
   count, date and seed cost. A new purchase also creates a Fish Stock record once.
   Alternatively, link an existing stock entry; its purchase amount/count/date are
   reused and it cannot be linked twice. Historical pre-tracking growth cost can
   be entered separately, without repeating seed cost. Existing past Daily Feed
   records are not retrospectively assigned by guesswork.
2. Configure feed under **Daily Feed → Pond Feed Setup**. Each completed tick
   snapshots its actual quantity/rate and adds the cost once to the pond's current
   batch stage. Feeding on a pond without a batch continues to work, but is not
   assigned to a batch. Existing pre-batch feeds remain unallocated.
3. Record mortality and medicine/labour/other expenses against the stage. Mortality
   reduces the count, not the accumulated cost. Dates must not precede the stage's
   latest activity. A zero remaining count closes a stage.
4. Transfer all or part of the survivors to an empty, active pond. The source and
   destination are locked in ID order. The destination opens a grow-out stage with
   proportionate carried cost. Transfer charges are an additional destination
   expense. Full transfers carry the exact remaining balance; partial transfers
   round HALF_UP to two decimals. The same batch can split into several ponds,
   with a visible parent-stage lineage. There is one active batch per pond. Count-based
   allocation assumes similar average fish size; biomass/weight-based transfer
   allocation is not included in this release.
5. Continue ticking feed in the grow-out pond. Set its appropriate feed/quantities;
   transfer does not guess a feed size or change the pond's setup automatically.
6. Use **Harvest / settle** from the batch to post count and weight. It creates an
   existing Harvest record and adds weight to Inventory once. Direct old-module
   harvests on an active batch pond are blocked so they cannot bypass batch counts.
   The original linked Fish Stock entry and batch harvest cannot be deleted from
   the old pages. Their purchase/count/cost history remains intact. New stocking
   into an occupied batch pond and deactivating that pond are blocked until its
   fish have been transferred or harvested.
7. Record settlement revenue at harvest, or use **Sale settlement** later for
   additional payments. These are batch accounting entries, not automatic customer
   invoices or dispatch receipts. Do not enter the same revenue twice. Existing
   Dispatch/Sales History continues independently; automatic invoice-to-batch
   allocation is not part of this release. The UI calls the result recorded revenue
   minus cost, since later settlements can change it.

The phase summary shows nursery additions including seed, transfer charges, and
new grow-out costs separately. Their sum is total batch expense. Carried-in cost
is shown on stage cards, but is never counted as a second batch expense. Reports
also show remaining fish, mortality, cost per surviving fish, harvested weight and
cost per kg when the batch has closed. A stage with no survivors may retain an
unrecovered loss balance; it remains part of total batch expense.

Transfers, mortality, stage expenses, harvests and settlements require a UUID
request key. Exact retries return the saved result; reusing a key with different
input returns 409. Mutation transactions use READ_COMMITTED with explicit pond and
stage locks, so a concurrent retry can see the just-committed movement and its child
stage. API clients must retain a request key when retrying an unchanged operation.
The UI does this until the form changes or succeeds.

Once feeding cost has been carried into a transfer or harvest, the feeding cannot
be silently edited/undone. Earlier immutable movements also cannot be deleted.
Corrections requiring historical reallocation need an explicit accounting adjustment
workflow; never edit the database ledger to force a different balance. New ticks,
undo actions, batch movements and access changes record the authenticated actor.

Example: seed ₹1,000 + nursery feed ₹100 + other nursery cost ₹4,900 = ₹6,000.
100 fish minus 40 deaths leaves 60, carrying ₹100/fish. Transfer cost ₹500 plus
new grow-out costs ₹3,500 makes total ₹10,000. If 50 survive at harvest, the average
cost is ₹200/fish; if their harvested weight is 25 kg, total batch cost is ₹400/kg.

## Validation and deployment

Run Maven `verify` against an empty disposable test database using the documented
cloud local overrides. Security tests exercise real filters and CSRF; legacy domain
regressions use an explicit test farm with filters disabled. Tests cover tenant
reads/writes/foreign IDs, farm switching, role/assignment boundaries, revocation,
password hashes, throttling, feed idempotency, stage snapshots, partial/full transfer,
concurrent retries, mortality, harvest/inventory and settlement reconciliation.
The regression concurrency fixture cleans up its own audit rows. Never run these
tests against production data.

Frontend: `npm ci`, `npm run build`. New auth, account, team and batch components
pass targeted ESLint; the existing App/invoice/entrypoint lint issues are separately
reported and are not disabled.

For an HTTPS deployment, use the `production` Spring profile and set the required
`SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD` and `APP_ALLOWED_ORIGINS` through
secure deployment configuration. Use a database URL with verified TLS. Serve UI and
API on the same site, preferably through one HTTPS origin with `/api` reverse-proxied
to Spring Boot. Set `VITE_API_BASE_URL=` before the production frontend build. The
production profile forces secure cookies and keeps `ddl-auto=none`; migrations
remain a manual deployment step. Supply `FARM_SETUP_TOKEN` securely only if an
unclaimed legacy farm needs its first owner.

This is the initial self-use/single-server foundation. Sessions and throttling are
in-memory. Multiple API replicas need shared sessions/throttling. Subscription billing,
email verification/password recovery, offline authenticated operations, mixed-batch
pond allocations, and automatic invoice reconciliation are future work.

For customer invitations, plans and the separate Super Admin panel, continue with
[Platform Setup](PLATFORM_SETUP.md) after the farm/batch migration.

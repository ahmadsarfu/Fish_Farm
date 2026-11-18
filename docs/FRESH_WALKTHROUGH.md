# Fresh database and your first complete customer flow

This resets only your local `fish_farm_db`. It removes **all old accounts and farm
records** and rebuilds the matching schema. It first saves a complete SQL backup
under `.local/backups/`. Other databases and your authenticator configuration are
not removed. Keep the backup private; it is ignored by Git. The cloud assistant
cannot run this against a MySQL server that is accessible only on your laptop.

## 1. Reset locally

Stop the Spring Boot backend (IntelliJ Stop or Ctrl+C). From your backend folder:

```bash
git pull --ff-only origin master
python3 scripts/reset-local-database.py
```

Enter your local MySQL password when prompted. The script needs `mysql` and
`mysqldump`. It aborts before clearing if connection/backup fails. It does not put
the password in shell history or command arguments. It checks the default API
port 8080 is stopped; if you run another API instance elsewhere, stop it too.
The script can use `--port` and `--api-port` for non-default local ports.

Wait for **Fresh schema verified**. It does not create default credentials or fake
customer accounts. Do not start the API after a failed reset; use the printed
backup path to restore or investigate the error first. Re-running the reset
creates a separate backup each time; it does not overwrite the previous one.

## 2. Create your platform owner

```bash
bash scripts/setup-platform-admin.sh
```

Choose your Super Admin username and a password of at least 14 characters. Add
the displayed time-based key to an authenticator app. If `.local/platform.env`
already exists, the existing authenticator key is reused. Old DB usernames and
passwords were removed, so this creates a new platform account.

In another terminal update/start the UI:

```bash
cd ~/fishfarm-ui
git pull --ff-only origin master
npm ci
npm run build
npm run preview
```

Open `http://localhost:4174/admin/login` and log in using your chosen admin details
and the authenticator's current six-digit code.

## 3. Add one customer

Click **Add customer**. Use the following example, substituting a real mobile
number you control for the contact field:

| Field | Example |
|---|---|
| Owner name | Sarfaraz |
| Farm name | Demo Fish Farm |
| Mobile | Your own mobile number |
| Email | Your own email, or leave empty |
| Plan | Starter; keep the 30-day trial defaults |

Click **Create activation link** and copy the link. For this walkthrough, open it
in an incognito window or a separate browser profile so the Super Admin session
stays separate. Set the customer's password, then click **Activate my farm**.
This opens an empty dashboard with clear setup links.

## 4. Make the first operational entries

1. **Ponds**: create Pond A (`P-001`) and Pond B (`P-002`).
2. **Fish Types**: add Rohu.
3. **Daily Feed → Feed Master & Records → Feed Master**: add 1 mm Starter feed
   at ₹60/kg and 2 mm Grower feed at ₹55/kg. These are walkthrough prices; use
   your actual rates when entering real records.
4. **Daily Feed → Pond Feed Setup**: set Pond A to 1 mm feed, AM 2 kg and PM 2 kg.
5. **Batches & Costing → Start Batch**: choose Rohu, Pond A, 100 fish and
   ₹1,000 purchase cost. Start this batch before ticking feed so nursery feeding
   is attributed to it. A new seed purchase also adds a Fish Stock entry.
6. **Team & Settings**: add a Labour user and assign Pond A. In a separate browser
   session log in as Labour. After feeding, tick AM. Completed count becomes 1;
   expense for that slot is ₹120. Tick PM after feeding: total becomes ₹240.
   Refresh: saved ticks remain. Labour cannot edit feed rates or plans.

## 5. Follow a nursery batch through transfer

Continue the batch created above after its two nursery feed ticks:

1. Owner: **Batches & Costing**, open the batch and record 40 deaths in the nursery
   stage. Remaining fish: 60. Nursery cost is ₹1,240 (₹1,000 seed + ₹240 feed).
2. Transfer all 60 to Pond B with ₹100 transfer expense. The ₹1,240 nursery
   balance is carried forward with the survivors; it is not counted twice.
3. Configure Pond B feeding at 2 mm, AM 2 kg and PM 2 kg. Each completed tick costs
   ₹110. Two ticks add ₹220 in the grow-out stage. Assign Pond B to Labour first
   if that user will record the ticks.
4. With no other costs, total batch expense is **₹1,560**:
   ₹1,240 nursery + ₹100 transfer + ₹220 grow-out. Cost per surviving fish is ₹26.
   Completed ticks keep their saved rates even if feed prices later change.

## 6. Try the sales/support controls

Back in Super Admin:

- **Manage plan → Active / paid**: set a future expiry date after collecting a
  payment separately. The app records access; it does not charge a card.
- **Pause**: refresh the customer dashboard; it shows renewal guidance. Data stays.
- Reactivate: refresh again; operations return.
- **Reset password**: privately share the one-hour link after checking identity.
  A completed reset logs out old sessions and preserves farm data and plan dates.

A managed owner may request another farm from Team & Settings. It stays paused
until the provider activates its plan. The same account/mobile then switches
between farms instead of creating another account with a duplicate contact.

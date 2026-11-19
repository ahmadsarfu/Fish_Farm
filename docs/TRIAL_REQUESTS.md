# Trial requests

New visitors use **New farm? Request a free trial** on `/login`, or open
`/request-trial`. Public owner registration can remain disabled.

The form collects an owner name, farm name, mobile and optional email. Contacts
are normalized; repeated requests for the same mobile receive the same generic
acknowledgement and never overwrite the original request. Requests do not create
accounts, reserve account contacts, reveal account existence or start trials.
The endpoint requires CSRF and allows up to ten submissions per remote address
per 15 minutes per running API instance. This in-memory limiter resets on restart;
use a shared limiter before scaling to multiple API instances.

A Super Admin opens `/admin/trial-requests`, reviews the contact details and
chooses trial days, plan name, pond and user limits. Verify the requester controls
the contact before approving; this workflow does not perform OTP verification.
Approval atomically creates the existing inactive owner/farm invitation and marks
the request approved. Concurrent/repeated review cannot create a second farm.
Existing account contacts cause approval to fail without changing the request.
Use the existing account onboarding/support process instead, or reject the request.

Copy the private activation link and share it manually (for example WhatsApp).
No SMS or email is sent automatically. The customer chooses a password and the
trial starts on activation, using the existing invitation expiry and token rules.
If the link is closed/lost, use **Customers & plans → New activation link**.
Rejection records the reviewing admin and time without creating a farm. Requests
are retained in the admin history; the same mobile cannot submit another request.

## Existing database deployment

Before deploying the new API/frontend, run `docs/sql/trial-requests-upgrade.sql`
on the target database. It only creates `trial_requests` and is safe to rerun;
it does not delete or rewrite existing records. No new environment variables are
needed; keep `PUBLIC_REGISTRATION_ENABLED=false` for this approval-based flow.

Use the hosted database credentials and prompt for the password:

```bash
mysql -h bz5p61o7qfsords853ws-mysql.services.clever-cloud.com \
  -P 3306 -u uu12exegz8t4z7zv -p --ssl-mode=REQUIRED \
  bz5p61o7qfsords853ws < docs/sql/trial-requests-upgrade.sql
```

`REQUIRED` matches the current testing deployment: traffic is encrypted but server
identity is not verified. Use the provider's trusted CA with verified TLS when
available. Do not put database credentials or TOTP secrets in the frontend.

Fresh installations run `schema-full.sql`, `platform-onboarding-upgrade.sql` and
any other documented module migrations; `schema-full.sql` includes the new table.

After deployment, test `/request-trial`, review/approve as Super Admin, copy the
activation link and activate in a separate browser session. Manager, labour and
owner accounts cannot access platform trial requests.

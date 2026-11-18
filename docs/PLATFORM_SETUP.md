# Selling Fish Farm: setup and day-to-day use

One Super Admin runs the business. Each customer has their own farm Owner account.
The existing Owner, Manager and Labour permissions and farm isolation remain in place.

## Existing installation: one-time database upgrade

Stop the API and back up your database before upgrading. Keep the backup outside
the repository. From the backend project directory, run:

```bash
mysqldump --single-transaction --no-tablespaces -u root -p fish_farm_db > ~/Downloads/fish_farm_before_platform.sql
mysql -u root -p fish_farm_db < docs/sql/platform-onboarding-upgrade.sql
```

Run this after the previous farm/batch migration. For a fresh installation, run
`docs/sql/schema-full.sql` against an empty database, then run the platform upgrade.
The platform upgrade is repeatable. Existing farms retain active Legacy access
without limits or expiry; existing usernames and passwords keep working. No real
customer database is migrated by the development/test commands in this repository.

## Create your Super Admin locally

Use Java 17+, Python 3, OpenSSL and your existing database configuration. The setup
script creates the first Super Admin only; it never changes an existing account's
password or promotes an existing ordinary user.

```bash
bash scripts/setup-platform-admin.sh
```

The script asks you to choose a username and password (14 characters minimum).
It displays a private authenticator key: add it manually in Google Authenticator,
Microsoft Authenticator or another TOTP app, choosing time-based / 6 digits.
Store a secure recovery copy of this key. The generated `.local/platform.env` is
ignored by Git and contains the key and the setting that disables open signup.
The password is not saved in that file. Never share this file with customers.

When the API is ready, open `http://localhost:4174/admin/login`. Enter your chosen
username, password and current six-digit authenticator code. There are no default
admin credentials. The server clock must be accurate for authenticator codes.

If your original farm still has no Owner account, open `/setup-existing`, create
its ordinary Owner account and enter the code from `.local/farm-setup-code`.
This one-time data-claim screen is separate from the customer login. Super Admin
and farm Owner accounts are intentionally separate.

For subsequent local starts, use:

```bash
bash scripts/start-local.sh
```

This loads `.local/platform.env` automatically. Starting from IntelliJ instead
requires the same environment values in its run configuration. Do not leave
`PLATFORM_BOOTSTRAP_ENABLED=true` or the bootstrap password set after setup.

## A practical sale

1. In **Customers & plans**, click **Add customer**.
2. Enter owner name, farm name and mobile. Email is optional. The default plan is
   Starter with a 30-day trial, 10 ponds and 5 active users including the owner.
   Open Plan settings to change those defaults.
3. Click **Create activation link**, then copy and privately share the link with
   the customer. The app does not send email, SMS or WhatsApp messages itself.
4. The customer opens the link, chooses a password and enters their own dashboard.
   Trial access starts at activation. Clear setup links lead to ponds, fish types,
   feeding setup and adding team members.
5. The customer logs in at `/login` using mobile or email. Existing accounts and
   Labour accounts can continue using usernames.
6. After collecting payment, open **Manage plan**, choose **Active / paid**, set
   expiry/limits and save. This records access manually; it does not charge a card,
   generate a payment receipt or verify that money has been received.

One subscription controls one farm. One account may access multiple farms. A
managed customer can request another farm in Team & Settings. It stays paused
until you configure and activate its plan in the admin panel. Creating another
farm never grants extra free operational access to a managed customer.

## Support without changing customer data

- An unused activation link expires after seven days. **New activation link**
  replaces it; the old link stops working.
- For a customer who has activated, **Reset password** creates a one-hour reset
  link. Verify their identity before sharing it. A completed reset revokes all
  existing sessions and does not restart or extend the subscription.
- Pausing/expiry preserves farm data. Customers can still log in, change their
  password, log out or switch to another farm. Business APIs enforce paused and
  expired plans on existing sessions too. Access is valid through the expiry date
  in the configured farm business timezone.
- Pond limits apply to non-deleted ponds. User limits count active memberships,
  including the owner. Re-enabling a member also checks capacity. Lowering a pond
  limit does not delete ponds: it blocks further additions until below the limit.
- Platform activity records invites, activation, reset and plan changes. The
  Super Admin role cannot read or modify customers' harvest, sales or feeding APIs.
  There is no silent impersonation feature.
- Unactivated accounts reserve their mobile/email. Reissue their invitation rather
  than creating a duplicate. Editing identity, moving owners and deleting customers
  are deliberately separate future workflows.

Mobile numbers are unique across application accounts. Indian ten-digit numbers
normalize to `+91...`; other countries require the country code. Emails normalize
to lowercase and are unique when present. These checks also have database unique
indexes. Manual link delivery does **not** prove ownership of an email or phone;
this release does not label either contact as OTP-verified.

## Production configuration

Use HTTPS, the production Spring profile, explicit allowed UI origins and the
existing database/secrets configuration. Set these through the server's secret
manager or private environment configuration:

- `PLATFORM_ADMIN_TOTP_SECRET`: the same private Base32 secret configured in your
  authenticator; at least 32 characters. Keep it available on every API instance.
- `PUBLIC_REGISTRATION_ENABLED=false`: normal customers use invitations. The
  existing-farm claim is still protected by the separate legacy setup code.
- For first admin creation only: `PLATFORM_BOOTSTRAP_ENABLED=true`,
  `PLATFORM_ADMIN_USERNAME`, `PLATFORM_ADMIN_PASSWORD`. Remove the password and
  bootstrap flag after successful setup. The production profile defaults open
  registration to disabled.

An activation link uses the current UI origin. For a sale, open the admin panel on
its public HTTPS address before copying the link; localhost is only for testing.
Keep normal page-to-index.html hosting fallback and `/api` proxy configuration.

This is a modular monolith: controllers delegate to service interfaces and
implementations, repositories own persistence, and the frontend separates page,
form, layout and API modules. Customer listing is paginated. Payment-provider and
SMS/email integrations can be added behind services without changing the worker
feeding workflow. Before running multiple API instances, use shared session
storage (such as Spring Session + Redis), a shared login rate limiter and the same
MFA configuration; the current session store and login throttle are per instance.

## Verification

The platform integration tests use a dedicated MySQL database. Run the existing
backend test suite with that test datasource; never point test commands at the
real customer database. Frontend `npm run test:e2e` uses API fixtures; the manual
browser smoke can additionally be run against a disposable API/database.

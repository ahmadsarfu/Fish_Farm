# Trial deployment: frontend + backend on Render

The local MySQL database is not reachable as localhost from Render. Deploy in
this order: **hosted MySQL → import current data → backend → frontend → login**.
No database reset is part of this deployment.

The intended trial layout is two Render Web Services using the dashboard's Free
instance option, plus a separately hosted MySQL 8 database. Render's current plan
availability/limits and the database provider's free/trial offer must be checked
in their dashboards before creating resources. Do not select a paid database
plan if your budget is zero. A promotional credit trial is not the same as an
ongoing free plan. The documentation websites were not reachable from this cloud
workspace, so current provider prices/quotas were not independently verified.

## 1. Get hosted MySQL 8

One option to check is Aiven: create a MySQL service and select its Free plan if
currently offered to your account/region. If only paid plans or expiring credits
are shown, decide whether that offer suits the trial before proceeding. Render's
free application instance is not a persistent MySQL host; do not put your only
MySQL data in its container filesystem.

Record these from the database dashboard:

- Host, port, username and password.
- Database name (often `defaultdb`; using that provided database is fine).
- Download the provider's CA certificate, for example `ca.pem`.

Keep credentials and the CA file locally; do not commit database passwords to Git
or share them in chat. The CA certificate itself is public, but it must be the
one supplied by your database provider.

## 2. Copy the laptop database without losing accounts

Stop the local API briefly while exporting. Run in the laptop terminal:

```bash
umask 077
mysqldump -u root -p --single-transaction --no-tablespaces --set-gtid-purged=OFF fish_farm_db > ~/Downloads/fishfarm-render-export.sql
```

Make sure the dump succeeded and the file is non-empty. This export intentionally
does not use `--databases`/`--add-drop-database`, so it can be imported into the
provider's chosen database name. Keep a separate copy of this backup.

For a NEW, EMPTY hosted database, replace HOST, PORT, USER and CLOUD_DATABASE below
with the provider's values, and point --ssl-ca at your downloaded certificate:

```bash
mysql --host=HOST --port=PORT --user=USER -p --ssl-mode=VERIFY_IDENTITY --ssl-ca="$HOME/Downloads/ca.pem" CLOUD_DATABASE < ~/Downloads/fishfarm-render-export.sql
```

Enter the hosted database password, not the laptop's root password. Do not import
this dump over an existing customer database: it contains table definitions and
can replace tables. Use the empty database created for this trial.

Verify against the hosted database:

```bash
mysql --host=HOST --port=PORT --user=USER -p --ssl-mode=VERIFY_IDENTITY --ssl-ca="$HOME/Downloads/ca.pem" CLOUD_DATABASE -e "SELECT id,username,active,platform_admin FROM app_accounts; SELECT id,name FROM farms;"
```

Your existing admin should be listed. The current schema should already include
platform-onboarding-upgrade.sql from local setup. If an old backup lacks those
columns, apply that migration to the hosted database before deploying the API.
Do not run reset-local-database.py or create another default admin for a migrated
installation.

## 3. Backend: Fish_Farm Web Service

In Render choose **New → Web Service**, connect `ahmadsarfu/Fish_Farm` and set:

| Field | Value |
|---|---|
| Name | Your available backend service name, for example `fishfarm-api` |
| Language | Docker (change the screenshot's Node selection) |
| Branch | master |
| Root Directory | Blank; remove `src/` |
| Dockerfile Path | ./Dockerfile |
| Docker Build Context | . |
| Docker Command | Blank |
| Instance type | Free, if offered |
| Health Check Path | /api/auth/setup-status |

There is no separate Build/Start Command for this Docker service. Under Secret
Files, add a file named `ca.pem` containing the database provider's public PEM CA
certificate. It will be available as `/etc/secrets/ca.pem`.

Add private Environment values:

```text
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:PORT/CLOUD_DATABASE?sslMode=VERIFY_IDENTITY
DB_USERNAME=HOSTED_DATABASE_USERNAME
DB_PASSWORD=HOSTED_DATABASE_PASSWORD
MYSQL_CA_CERT_PATH=/etc/secrets/ca.pem
PUBLIC_REGISTRATION_ENABLED=false
APP_ALLOWED_ORIGINS=http://localhost:4174
PLATFORM_ADMIN_TOTP_SECRET=YOUR_EXISTING_PRIVATE_BASE32_KEY
```

Replace every uppercase placeholder. APP_ALLOWED_ORIGINS will be updated to the
actual frontend URL after its deployment. PLATFORM_ADMIN_TOTP_SECRET must be the
same key already enrolled in Sarfraz's authenticator (from ignored
.local/platform.env on the laptop), not the rotating six-digit code. Enter only
the value, without the shell's `export KEY=` prefix. Keep this key private.

Do not set the bootstrap username/password for an imported database that already
contains the admin. Click Deploy. Render supplies PORT automatically. When Live,
copy the actual backend URL and open:

```text
https://YOUR_BACKEND.onrender.com/api/auth/setup-status
```

Expect JSON and HTTP 200. If it fails, inspect backend logs for database host,
port, TLS certificate or missing-schema errors. Do not continue to login while
this check is failing.

## 4. Frontend: Fish_Farm_UI Node Web Service

Choose **New → Web Service**, connect `ahmadsarfu/Fish_Farm_UI` and set:

| Field | Value |
|---|---|
| Name | An available frontend service name, for example `fishfarm-ui` |
| Language | Node |
| Branch | master |
| Root Directory | Blank |
| Build Command | npm ci --include=dev && VITE_API_BASE_URL= npm run build |
| Start Command | npm start |
| Instance type | Free, if offered |
| Health Check Path | /healthz |

Set one environment value:

```text
API_PROXY_TARGET=https://YOUR_ACTUAL_BACKEND.onrender.com
```

Use only the backend origin: no /api suffix, database password or token. The
build command intentionally sets VITE_API_BASE_URL to an empty value so browser
requests stay on `/api` at the frontend origin. This is a Node Web Service, not a
Static Site: its small server serves the build and proxies API calls. It avoids
cross-site session-cookie problems between Render's default service domains.

Never put DB_PASSWORD or PLATFORM_ADMIN_TOTP_SECRET in the frontend service.
Choose Node 22.12+ or 24 if a version setting is needed; package.json specifies
compatible versions.

## 5. Finish the allowed-origin setting

Copy the actual public frontend URL, for example the URL Render displays for
fishfarm-ui. In the BACKEND Environment settings replace APP_ALLOWED_ORIGINS with
that exact HTTPS origin (no trailing slash), save, and let it redeploy.

Open these addresses after both services are Live:

```text
https://YOUR_FRONTEND.onrender.com/healthz
https://YOUR_FRONTEND.onrender.com/api/auth/setup-status
https://YOUR_FRONTEND.onrender.com/admin/login
```

Both JSON checks should succeed. Sarfraz can log in with the existing password
and current authenticator code. Then test owner/manager/labour login, a page
refresh and one feeding tick. Activation links now use the public frontend
address, so a customer can open them on a phone without using your laptop.

## Trial expectations

Free compute may sleep after inactivity. The first request can take a while
because both UI and API may need to wake up. Check the workspace's shared free
instance-hour allowance for two services; do not assume two always-on services
fit within an account's quota. Restart/deploy can log users out because sessions
currently live in API memory. Database records remain in the hosted database.

Use the provider's backups/export options and retain local SQL backups. Check
whether a trial expires or a free database can be suspended/deleted for inactivity.
This setup is for evaluation; review uptime, backups and shared session storage
before relying on it for daily farm operations at scale.

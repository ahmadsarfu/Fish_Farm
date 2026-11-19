# Spring Boot backend on Render

Create a **Web Service** from `ahmadsarfu/Fish_Farm`, branch `master`:

| Setting | Value |
|---|---|
| Language / runtime | Docker |
| Root Directory | Leave blank (the directory containing pom.xml and Dockerfile) |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Docker Command | Leave blank; the Dockerfile supplies it |
| Health Check Path | `/api/auth/setup-status` |

Docker services do not need a separate Build Command or Start Command. The image
build runs `mvn -B -DskipTests clean package` with Java 17, then starts the packaged
Spring Boot jar as a non-root user. The app binds to Render's `PORT` variable and
falls back to 8080 locally. Render supplies PORT; do not hardcode it in settings.
Tests run against disposable databases during development, not at deploy time.
Private `.local` files, backups, environment files and local build output are
excluded from the image build context.

## Required environment variables

Set these in Render's Environment settings, not in Git or the Dockerfile:

```text
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:mysql://YOUR_MYSQL_HOST:3306/fish_farm_db?sslMode=VERIFY_IDENTITY
DB_USERNAME=YOUR_DATABASE_USER
DB_PASSWORD=YOUR_DATABASE_PASSWORD
APP_ALLOWED_ORIGINS=https://YOUR_FRONTEND_HOST
PUBLIC_REGISTRATION_ENABLED=false
PLATFORM_ADMIN_TOTP_SECRET=YOUR_EXISTING_PRIVATE_AUTHENTICATOR_SECRET
```

Use the JDBC connection string, TLS settings/certificates and port required by
your MySQL provider. The example assumes a public CA trusted by Java. Use a
provider account limited to this application's database; the laptop's root/root
credentials and localhost URL do not point to a hosted database. Do not place
MySQL data inside the backend container filesystem.

Apply the existing schema/migrations to the hosted database before starting the
service. To preserve your current accounts and farm data, migrate a protected
backup of your existing database rather than rebuilding/resetting it. The
production profile uses `ddl-auto=none` and does not create tables at startup.

If the hosted database already contains your Super Admin, keep the existing
PLATFORM_ADMIN_TOTP_SECRET. Do not create another admin or rotate the key just to
deploy. For a genuinely empty hosted installation, follow PLATFORM_SETUP.md and
set these additional private environment values for the first successful start:

```text
PLATFORM_BOOTSTRAP_ENABLED=true
PLATFORM_ADMIN_USERNAME=YOUR_CHOSEN_ADMIN_USERNAME
PLATFORM_ADMIN_PASSWORD=YOUR_CHOSEN_STRONG_PASSWORD
```

After the first admin has been created, remove the bootstrap flag and password
from Render and redeploy. Keep the authenticator secret configured. Never share
passwords, JDBC credentials or the authenticator secret in deployment logs/chat.

## Frontend and login

Build the frontend with `VITE_API_BASE_URL` set to the backend's public HTTPS URL
(no trailing `/api`). APP_ALLOWED_ORIGINS must exactly match the browser UI origin,
including scheme; multiple allowed origins can be comma-separated.

Cookie sessions and CSRF cookies are configured SameSite=Lax. For dependable
browser login, serve the UI and API on the same site, for example
`https://farm.example.com` and `https://api.example.com`, or use a same-origin
reverse proxy. Distinct default `*.onrender.com` services can be cross-site;
CORS alone does not make Lax cookies work in that topology. Coordinate frontend
hosting before relying on production login. Do not remove CSRF protection to
work around a cookie deployment issue.

For daily farm use, choose a service/database arrangement with suitable uptime,
automatic backups and tested restores. Check your plan's sleep, storage and
retention behavior. The current application stores sessions and login throttling
in one API instance; use shared session/rate-limit storage before adding replicas.

After deployment, verify `/api/auth/setup-status`, the UI login, MFA, farm switching
and one feeding tick through the actual public HTTPS addresses.

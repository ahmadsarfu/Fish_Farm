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

For a trial using Render's default domains, deploy Fish_Farm_UI as a **Node Web
Service** with its production API proxy (see RENDER_FREE_TRIAL.md):

- Build: `npm ci --include=dev && VITE_API_BASE_URL= npm run build`
- Start: `npm start`
- Environment: `API_PROXY_TARGET=https://YOUR_BACKEND.onrender.com`
- Set backend APP_ALLOWED_ORIGINS to the actual public frontend HTTPS origin.

The browser calls the frontend's own `/api` path. The Node server forwards it to
Spring Boot, including cookies and CSRF headers. This keeps browser authentication
same-origin without changing SameSite=Lax or disabling CSRF. The proxy keeps API
responses uncached and forwards separate Set-Cookie headers.

A Static Site can instead be used when you have a same-origin API reverse proxy
or frontend/API custom subdomains on the same primary site. Distinct default
`*.onrender.com` services can be cross-site; CORS alone does not make Lax cookies
work in that topology.

For a managed MySQL provider with a private CA, add its public CA certificate as a
Render Secret File named `ca.pem`, set `MYSQL_CA_CERT_PATH=/etc/secrets/ca.pem`, and
keep `sslMode=VERIFY_IDENTITY` in the JDBC URL. The startup script adds that CA to
a temporary copy of the JVM trust store while preserving standard public CAs.
No database password or authenticator secret is baked into the Docker image.

For daily farm use, choose a service/database arrangement with suitable uptime,
automatic backups and tested restores. Check your plan's sleep, storage and
retention behavior. The current application stores sessions and login throttling
in one API instance; use shared session/rate-limit storage before adding replicas.

After deployment, verify `/api/auth/setup-status`, the UI login, MFA, farm switching
and one feeding tick through the actual public HTTPS addresses.

For the complete backend + frontend trial walkthrough, see [Render Free Trial](RENDER_FREE_TRIAL.md).

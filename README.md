# Fish Farm Management API

Spring Boot / Java 17 / MySQL 8 backend for ponds, stocking, feeding, customers,
harvests, dispatches, sales history and batch costing.

**Upgrading an existing installation:** follow [Farm and Batch Setup](docs/FARM_BATCH_SETUP.md)
before starting this authenticated release. Existing data stays in legacy farm 1;
claiming it requires a one-time setup code. There are no default application users.

- Existing database: `docs/sql/farm-batch-upgrade.sql`.
- Empty database: `docs/sql/schema-full.sql` (refuses a nonempty database).
- Local API start: `bash scripts/start-local.sh` after migration.
- Test: `bash mvnw verify` against a disposable configured test database.
- Frontend repository: [Fish_Farm_UI](https://github.com/ahmadsarfu/Fish_Farm_UI).

Owner, Manager and assigned Labour access is enforced on the server. All mapped
business data is farm-scoped. Sessions use HttpOnly cookies and CSRF protection;
production HTTPS configuration is described in the setup guide.

Batch costing tracks nursery/grow-out, mortality, partial transfers, feed snapshots,
other costs, harvest/inventory and manually recorded sale settlements. It does not
double-count carried cost. Initial scope is self-use on a single server; billing,
password-recovery email and automatic invoice allocation are not included.

Commercial onboarding and Super Admin setup: [Platform Setup](docs/PLATFORM_SETUP.md).

Backend deployment on Render (Java 17 Docker service and hosted MySQL):
[Render Deployment](docs/RENDER_DEPLOYMENT.md).

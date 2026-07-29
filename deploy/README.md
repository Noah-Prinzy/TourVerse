# TourVerse production deployment

This directory provides a provider-neutral Docker Compose deployment for a
single Linux host. It runs PostgreSQL, the Ktor API, the built React
application, and Caddy as the HTTPS reverse proxy.

Use this configuration for staging before creating a separate production
environment. Do not reuse staging databases, JWT secrets, passwords, domains,
or Maps keys in production.

## Prerequisites

- A Linux server with Docker Engine and Docker Compose.
- A domain whose `A`/`AAAA` records point to the server.
- Inbound TCP ports 80 and 443 and UDP port 443 allowed by the firewall.
- A separate off-server location for encrypted database backups.

## Configure the environment

Copy `production.env.example` to `.env` inside this directory. The `.env` file
is ignored by Git. Replace every placeholder with a private value.

Generate secrets locally rather than using memorable passwords:

```powershell
$bytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Restrict browser Maps keys to the exact HTTPS domain. Restrict Android Maps
keys to the application ID and release signing certificate. Keep backend
provider keys server-side.

## Start staging

From this directory on the server:

```bash
docker compose --env-file .env -f docker-compose.production.yml config
docker compose --env-file .env -f docker-compose.production.yml build
docker compose --env-file .env -f docker-compose.production.yml up -d
docker compose --env-file .env -f docker-compose.production.yml ps
```

Caddy obtains and renews the TLS certificate after DNS and firewall routing
are correct. Flyway applies pending migrations during API startup.

Verify:

```bash
curl --fail https://YOUR_DOMAIN/api/health
curl --fail https://YOUR_DOMAIN/api/docs
```

## Database backups

Create an encrypted off-server backup before every release and at least daily:

```bash
docker compose --env-file .env -f docker-compose.production.yml \
  exec -T postgres sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > tourverse-$(date +%F-%H%M).dump
```

Periodically restore a backup into a separate temporary PostgreSQL instance.
A backup that has never been restored is not yet a verified recovery plan.

## Updating

```bash
git pull --ff-only
docker compose --env-file .env -f docker-compose.production.yml build
docker compose --env-file .env -f docker-compose.production.yml up -d
docker compose --env-file .env -f docker-compose.production.yml ps
```

Review new Flyway migrations and take a database backup before updating.
Never modify a migration already applied to staging or production.

## Production checklist

- CI is green for the exact revision being deployed.
- The working tree is clean and the revision is tagged.
- Production has its own domain, database, volume, secrets, and provider keys.
- Development seed destinations remain disabled.
- Real destinations have been curated and approved.
- Administrator passwords are unique and stored in a password manager.
- Health checks, resource usage, logs, disk space, TLS expiry, and backup jobs
  have alerts.
- Registration, login, refresh, logout, profile deletion, catalogue browsing,
  trips, bookings, notifications, and role boundaries pass staging tests.
- A privacy policy, terms, support address, and data-retention policy are
  published before accepting public registrations.

## Rollback

Application containers can be rolled back by checking out the previous tagged
revision and rebuilding. Database rollback is not automatic: prefer additive,
backward-compatible migrations. Restore the verified pre-release backup only
after assessing data created since that backup.

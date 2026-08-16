# Deployment

Everything an operator needs to run this application in production. The application ships secure
defaults where it can; the points below are the ones that cannot be decided inside the image.

**Production stack:** `docker-swarm/stacks/international/docker-compose.yml` in the
`cevi-tools-infrastructure` repository. `compose.yml` in this repository mirrors it as a reference
and for local runs.

---

## 1. Secrets

Three secrets must exist. `src/main/docker/mount_secrets.sh` reads them at container start and
**aborts the start if any is missing** — a silent fallback is what makes a misconfigured deployment
look healthy.

| Docker secret                     | Environment variable                        | Purpose |
|-----------------------------------|---------------------------------------------|---------|
| `international_http_session_key`  | `QUARKUS_HTTP_AUTH_SESSION_ENCRYPTION_KEY`  | Encrypts the session cookie. |
| `international_admin_password`    | `APPLICATION_ADMIN_PASSWORD`                | Password of the first administrator account. |
| `international_mailer_password`   | `QUARKUS_MAILER_PASSWORD`                   | Mail relay account for contact form notifications. |

### Creating them

```shell
# On a swarm manager
openssl rand -base64 32 | docker secret create international_http_session_key -
openssl rand -base64 24 | docker secret create international_admin_password -
printf '%s' '<mail relay password>' | docker secret create international_mailer_password -
```

### About the session key

The cookie is stateless: it is an encrypted `expiry:username` pair and nothing else is kept on the
server. Anyone holding the key can mint a valid administrator cookie for any existing user name
**without knowing the password**. Treat the key like a master password.

- It must be at least 16 characters. `openssl rand -base64 32` is the right size.
- It is **not** in the repository. `application.properties` carries a throw-away key for the `dev`
  and `test` profiles only, and `ProductionSecurityCheck` refuses to start the `prod` profile if the
  key is missing or still holds the development placeholder.
- Rotating it invalidates all sessions — everyone signs in again. That is the only consequence, so
  rotate whenever there is doubt.

> **One-time action:** the key that used to sit in `application.properties` is in the git history and
> the repository is public. If it was ever used in production, rotate the secret **and** change the
> administrator password.

### About the initial administrator password

Used **only** when the user table is empty — the first start, or a start against a restored empty
database. Once an account exists the secret is ignored, so it can stay in place across deployments.
The account name defaults to `admin` and can be changed with `APPLICATION_ADMIN_USERNAME`.

The application has no page for changing a password yet, so a rotation means editing the `users`
table directly (or emptying it and restarting with a new secret). Sign in once after the first
deployment to confirm the account works.

Demo content (`DemoData`) is excluded from the `prod` profile: its trigger is an empty database,
which is exactly the state of a fresh production system.

---

## 2. TLS and the reverse proxy

TLS is terminated by Traefik; the container speaks plain HTTP on the internal network only.

`application-prod.properties` sets `quarkus.http.proxy.proxy-address-forwarding=true`. This is what
makes the application see the original `https` scheme, which in turn is what makes it mark the
session and CSRF cookies `Secure` and keep redirects on `https`.

> **The port must never be published directly while this is enabled.** With forwarding on, the
> application believes the `X-Forwarded-*` headers of whoever connects. On the swarm the service is
> attached to `traefik-net-1400` and publishes no port — keep it that way.

The application sends `Strict-Transport-Security` with `max-age=31536000; includeSubDomains`, so a
browser that has seen the site once will not make a plain-HTTP request to it again. Keep the
`redirect-to-https@file` middleware on the `web` entrypoint for first-time visitors.

---

## 3. Rate limiting

The application deliberately has no rate limiting of its own — it belongs in the proxy, in front of
the process. Three Traefik middlewares are configured in the stack:

| Route                                  | Limit                    | Why |
|----------------------------------------|--------------------------|-----|
| everything                             | 20 req/s, burst 40       | Ordinary browsing, generous. |
| `/auth/j_security_check`               | 5 req/10 s, burst 5      | Sign-in has no lockout; this is what bounds guessing. |
| `/kontakt`                             | 1 req/10 s, burst 3      | Anonymous, writes to the database and sends a mail per submission. |

The tighter limits need their own routers with `priority=100`, otherwise the catch-all router of the
host rule wins. Traefik counts per source address and answers excess requests with HTTP 429.

Sign-in attempts are logged by the application (`AuthenticationEventLogger`): failures at `WARN`
with user name and source address, successes at `INFO`. A run of `WARN` lines from one address is
the signal worth alerting on.

---

## 4. Request limits

| Setting                                    | Value  | Why |
|--------------------------------------------|--------|-----|
| `quarkus.http.limits.max-body-size`        | 1 MB   | Backstop for the anonymous contact form. |
| `quarkus.http.limits.max-form-attribute-size` | 256 KB | A description may hold 65 535 characters and the editor embeds pasted pictures inline. |

Do not lower `max-form-attribute-size` below ~128 KB: Quarkus REST defaults to 2 KB, which silently
turns every long event description into an HTTP 413.

Contact messages are additionally limited to 5 000 characters, validated before storage.

---

## 5. Container hardening

The stack sets `cap_drop: [ALL]`, `security_opt: [no-new-privileges:true]` and CPU/memory limits.
The image already runs as the unprivileged user `185`.

Two further points:

- **Mount only the database directory.** The volume is `/cluster/swarm/data/international:/data`.
  Never mount a directory that holds anything besides the database.
- **A read-only root filesystem** (`read_only: true` plus `tmpfs: [/tmp]`) is set in the reference
  `compose.yml` but **not** in the swarm stack — it has not been verified against a running
  deployment. Enable it there once a deployment with it has been confirmed to start.

---

## 6. Logging and data protection

- Application logs run at `INFO`. Client IP addresses are logged at `DEBUG` only, so a production
  system records no visitor movement profile.
- Contact message contents are **not** logged; an entry is referenced by its database id.
- Raising the level to `DEBUG` starts recording IP addresses. Do that for a specific investigation
  and put it back afterwards.

---

## 7. Backup

The whole state is one file: `/cluster/swarm/data/international/international.db` (plus the WAL
sidecar files). There is no documented backup today — this is the largest open operational risk.

```shell
# Consistent copy while the application runs
sqlite3 /cluster/swarm/data/international/international.db ".backup '/backup/international-$(date +%F).db'"
```

A restore is a file copy plus a restart. Note that a restore to an **empty** database re-triggers the
initial administrator bootstrap.

---

## 8. Deployment checklist

1. All three secrets exist on the swarm (`docker secret ls`).
2. The service publishes no port and is attached to `traefik-net-1400` only.
3. Container started — a missing secret aborts the start, so a running container means the secrets
   arrived.
4. `https://international.cevi.tools/` reachable, plain HTTP redirects to it.
5. Sign in once and open `/version` (it requires an administrator session) to confirm that the
   application version and the schema version match the release.
6. Response headers carry `Content-Security-Policy`, `Strict-Transport-Security`,
   `X-Frame-Options`, `X-Content-Type-Options` and `Referrer-Policy`.
7. The session cookie `quarkus-credential` carries `Secure`, `HttpOnly` and `SameSite=Strict`.

```shell
curl -sI https://international.cevi.tools/anlaesse | grep -iE 'content-security|strict-transport|x-frame|x-content|referrer'
```

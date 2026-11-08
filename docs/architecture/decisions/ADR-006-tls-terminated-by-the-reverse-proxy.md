# ADR-006: Terminate TLS at the surrounding Traefik reverse proxy

**Status:** Accepted
**Date:** 2026-11-08
**Requirements:** NFR-008, NFR-007, NFR-023, NFR-019, C-015

## Context

All production traffic must be served over HTTPS with TLS 1.2 or higher (NFR-008). The application
deploys as a single image (NFR-019) into an infrastructure that already runs Traefik in front of its
services.

## Decision

TLS is terminated by the surrounding Traefik infrastructure. The application contributes the
`Strict-Transport-Security` header (NFR-023) and the `Secure` flag of the session cookie (NFR-007).
The production settings are documented in `docs/deployment.md` (C-015).

## Consequences

- The application handles no certificates.
- Transport security depends on the proxy configuration, which lives outside this repository.
